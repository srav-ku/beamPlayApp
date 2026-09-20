// ============================================================================
// ADMIN - ENRICH  (v3 - self-updating: admin endpoint + daily cron)
//
// Two things to install:
//   A) Replace handleAdminEnrich (the whole function) with everything below,
//      EXCEPT keep the last block separate - see step B.
//   B) Add one line just before `export default beamWorker;` (line 2719):
//
//         beamWorker.scheduled = async (event, env, ctx) => {
//             ctx.waitUntil(runStaleEnrichment(env, 25));
//         };
//
//   C) SQL once:  ALTER TABLE movies ADD COLUMN enriched_at INTEGER;
//   D) Add a Cron Trigger: daily at 03:00 UTC  ->  cron expression `0 3 * * *`
//      (Dashboard: Workers & Pages -> your worker -> Settings -> Triggers ->
//       Cron Triggers -> Add.  Or wrangler.toml: [triggers] crons = ["0 3 * * *"])
//
// After that you never run anything by hand again.
//
// STALENESS POLICY (why this does not waste calls)
//   1. never enriched            -> enrich now          (backfill)
//   2. enriched but no RT yet    -> retry after 7 days  (ratings land post-release)
//   3. fully enriched            -> refresh after 30 days (keeps numbers current)
//   25 rows per run x daily cron => the catalogue self-heals in days, then stays
//   fresh at roughly 11 TMDB/OMDb calls a day for 328 titles.
// ============================================================================

// --- shared: enrich one movie row -------------------------------------------
async function enrichOne(movie, env, errors) {
    let t = {};
    try {
        t = await tmdbGet(
            `/movie/${movie.tmdb_id}?append_to_response=videos,credits,external_ids`,
            env
        ) || {};
    } catch (e) {
        if (errors) errors.push({ id: movie.id, step: 'tmdb', message: String((e && e.message) || e).slice(0, 200) });
    }

    let rt = null;
    let metacritic = null;
    const imdbId = movie.imdb_id || t.imdb_id || (t.external_ids && t.external_ids.imdb_id);
    if (imdbId) {
        try {
            const omdb = await omdbGet(imdbId, env);
            if (omdb && omdb.Response !== 'False') {
                const rtEntry = (omdb.Ratings || []).find(r => r.Source === 'Rotten Tomatoes');
                rt = rtEntry ? rtEntry.Value : null;
                const mc = parseInt(omdb.Metascore, 10);
                metacritic = isNaN(mc) ? null : mc;
            }
        } catch (e) {
            if (errors) errors.push({ id: movie.id, step: 'omdb', message: String((e && e.message) || e).slice(0, 200) });
        }
    }

    const videos = (t.videos && t.videos.results) || [];
    const trailer =
        videos.find(v => v.site === 'YouTube' && v.type === 'Trailer') ||
        videos.find(v => v.site === 'YouTube' && v.type === 'Teaser') ||
        videos.find(v => v.site === 'YouTube');

    const crew = (t.credits && t.credits.crew) || [];
    const director = crew.find(c => c.job === 'Director');

    await tursoRun(
        `UPDATE movies SET
            rt_rating = COALESCE(?, rt_rating),
            metacritic = COALESCE(?, metacritic),
            budget = COALESCE(?, budget),
            revenue = COALESCE(?, revenue),
            trailer_key = COALESCE(?, trailer_key),
            director = COALESCE(?, director),
            director_tmdb_id = COALESCE(?, director_tmdb_id),
            enriched_at = ?
         WHERE id = ?`,
        [
            rt,
            metacritic,
            t.budget ? t.budget : null,
            t.revenue ? t.revenue : null,
            trailer ? trailer.key : null,
            director ? director.name : null,
            director ? director.id : null,
            nowEpoch(),
            movie.id,
        ],
        env
    );

    return { id: movie.id, rt, metacritic, trailer: trailer ? trailer.key : null };
}

// --- which rows are worth spending a call on? -------------------------------
async function pickStaleIds(env, limit) {
    const now = nowEpoch();
    const week = 7 * 24 * 3600;
    const month = 30 * 24 * 3600;
    const rows = await tursoQueryAll(
        `SELECT id, tmdb_id, imdb_id FROM movies
          WHERE tmdb_id IS NOT NULL
            AND (
                 enriched_at IS NULL
                 OR (rt_rating IS NULL AND enriched_at < ?)
                 OR enriched_at < ?
            )
          ORDER BY COALESCE(enriched_at, 0) ASC
          LIMIT ?`,
        [now - week, now - month, limit],
        env
    );
    return rows || [];
}

// --- the cron body ----------------------------------------------------------
async function runStaleEnrichment(env, limit) {
    const rows = await pickStaleIds(env, limit || 25);
    let updated = 0;
    for (const movie of rows) {
        try {
            await enrichOne(movie, env, null);
            updated++;
        } catch (e) {
            console.log('enrich failed for id', movie.id, e && e.message);
        }
    }
    console.log(`enrich cron: refreshed ${updated} of ${rows.length} candidate rows`);
    return updated;
}

// --- admin endpoint: { ids: [..] }  or  { "mode": "stale", "limit": 25 } -----
async function handleAdminEnrich(request, env, params) {
    const body = await readBody(request);
    const skipped = [];
    const errors = [];
    let movies = [];
    let mode = 'ids';

    if (body.mode === 'stale') {
        mode = 'stale';
        movies = await pickStaleIds(env, Math.min(50, body.limit || 25));
    } else {
        const ids = Array.isArray(body.ids) ? body.ids : [];
        if (ids.length === 0) {
            return errJson('send { ids: [1,2] } or { mode: "stale" }', 400, env);
        }
        if (ids.length > 50) return errJson('max 50 ids per call', 400, env);
        for (const id of ids) {
            const movie = await tursoQueryOne(
                'SELECT id, tmdb_id, imdb_id FROM movies WHERE id = ?', [id], env
            );
            if (!movie || !movie.tmdb_id) { skipped.push(id); continue; }
            movies.push(movie);
        }
    }

    let updated = 0;
    for (const movie of movies) {
        await enrichOne(movie, env, errors);
        updated++;
    }

    return json({ ok: true, mode, updated, skipped, remainingCandidates: null, errors }, 200, env);
}
