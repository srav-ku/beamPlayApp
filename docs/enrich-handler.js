// ============================================================================
// ADMIN - ENRICH  (v2 - uses the worker's own tmdbGet helper, reports errors)
// POST /admin/enrich   { ids: [214] }
//
// PASTE THIS OVER the existing handleAdminEnrich function (the one starting
// near line 1097 and ending with "return json({ ok: true, updated }, 200, env);").
// Nothing else in the worker changes. No new environment variables.
//
// Why v2:
//   v1 called TMDB with a hand-written fetch and swallowed any failure, so
//   budget/revenue/trailer/director came back NULL with no explanation.
//   v2 uses the worker's own tmdbGet() - the same helper the import flow
//   already uses successfully - and returns an `errors` array so a failure
//   names itself instead of going quiet.
//
// The response now looks like:
//   { ok: true, updated: 3, skipped: [], errors: [] }
//   { ok: true, updated: 0, skipped: [1], errors: [{ id: 214, step: 'tmdb', message: 'TMDB 401: ...' }] }
// ============================================================================
async function handleAdminEnrich(request, env, params) {
    const body = await readBody(request);
    const ids = Array.isArray(body.ids) ? body.ids : [];
    if (ids.length === 0) return errJson('ids array is required', 400, env);
    if (ids.length > 50) return errJson('max 50 ids per call', 400, env);

    let updated = 0;
    const skipped = [];
    const errors = [];

    for (const id of ids) {
        const movie = await tursoQueryOne(
            'SELECT id, tmdb_id, imdb_id FROM movies WHERE id = ?', [id], env
        );
        if (!movie || !movie.tmdb_id) { skipped.push(id); continue; }

        // TMDB: budget, revenue, trailer key, director, imdb id.
        let t = {};
        try {
            t = await tmdbGet(
                `/movie/${movie.tmdb_id}?append_to_response=videos,credits,external_ids`,
                env
            ) || {};
        } catch (e) {
            errors.push({ id, step: 'tmdb', message: String((e && e.message) || e).slice(0, 200) });
        }

        // OMDb: Rotten Tomatoes + Metacritic.
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
                errors.push({ id, step: 'omdb', message: String((e && e.message) || e).slice(0, 200) });
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
                director_tmdb_id = COALESCE(?, director_tmdb_id)
             WHERE id = ?`,
            [
                rt,
                metacritic,
                t.budget ? t.budget : null,
                t.revenue ? t.revenue : null,
                trailer ? trailer.key : null,
                director ? director.name : null,
                director ? director.id : null,
                id,
            ],
            env
        );
        updated++;
    }

    return json({ ok: true, updated, skipped, errors }, 200, env);
}
