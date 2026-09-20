// ============================================================================
// ADMIN - ENRICH  (corrected)
// POST /admin/enrich   { ids: [1,2,3] }
//
// Paste this OVER the handleAdminEnrich function in the worker.
// Requires NO new environment variables:
//   - auth comes from the ROUTES entry ('admin'), so the router already checked
//     the admin JWT before this runs. The old x-admin-key check is removed -
//     it depended on an ADMIN_KEY variable that does not exist, which made the
//     endpoint answer 403 every single time.
//   - TMDB_API_KEY and OMDB_API_KEY already exist in the worker (lines 32-33).
//
// Fixes over the first version:
//   1. append_to_response=videos,credits  -> tmdb.videos was always undefined,
//      so trailer_key could never be written. Now it is.
//   2. imdb id falls back to tmdb.imdb_id -> many rows have a NULL imdb_id, and
//      those were silently skipped for RT + Metacritic.
//   3. director + director_tmdb_id are filled from TMDB crew.
// ============================================================================
async function handleAdminEnrich(request, env, params) {
    const body = await readBody(request);
    const ids = Array.isArray(body.ids) ? body.ids : [];
    if (ids.length === 0) return errJson('ids array is required', 400, env);
    if (ids.length > 50) return errJson('max 50 ids per call', 400, env);

    let updated = 0;
    const skipped = [];

    for (const id of ids) {
        const movie = await tursoQueryOne(
            'SELECT id, tmdb_id, imdb_id FROM movies WHERE id = ?', [id], env
        );
        if (!movie || !movie.tmdb_id) { skipped.push(id); continue; }

        // Budget / revenue / trailer key / director all come from TMDB.
        let tmdb = {};
        try {
            const tmdbRes = await fetch(
                `https://api.themoviedb.org/3/movie/${movie.tmdb_id}?api_key=***, 'TMDB_API_KEY')}&append_to_response=videos,credits`,
                { headers: { Accept: 'application/json' } }
            );
            if (tmdbRes.ok) tmdb = await tmdbRes.json();
        } catch (_) { /* leave tmdb empty; COALESCE keeps old values */ }

        const trailer = (tmdb.videos?.results || [])
            .find(v => v.site === 'YouTube' && v.type === 'Trailer');
        const director = (tmdb.credits?.crew || []).find(c => c.job === 'Director');

        // Rotten Tomatoes + Metacritic come from OMDb.
        let rt = null;
        let metacritic = null;
        const imdbId = movie.imdb_id || tmdb.imdb_id;
        if (imdbId) {
            try {
                const omdb = await omdbGet(imdbId, env);
                if (omdb && omdb.Response !== 'False') {
                    const rtEntry = (omdb.Ratings || []).find(r => r.Source === 'Rotten Tomatoes');
                    rt = rtEntry ? rtEntry.Value : null;
                    const mc = parseInt(omdb.Metascore, 10);
                    metacritic = isNaN(mc) ? null : mc;
                }
            } catch (_) { /* leave RT/metacritic null */ }
        }

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
                tmdb.budget ? tmdb.budget : null,
                tmdb.revenue ? tmdb.revenue : null,
                trailer ? trailer.key : null,
                director ? director.name : null,
                director ? director.id : null,
                id,
            ],
            env
        );
        updated++;
    }

    return json({ ok: true, updated, skipped }, 200, env);
}
