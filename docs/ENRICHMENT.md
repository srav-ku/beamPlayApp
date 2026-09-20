# Enrichment — exact placement (worker lines from the 2615-line worker)

The worker **already has** `TMDB_API_KEY`, `OMDB_API_KEY` (lines 32-33) and an
`omdbGet(imdbId, env)` helper (line 228). **No new credentials are needed.**

Four edits, then the SQL, then one call.

---

## Edit 1 — line 407: make the API return the new fields

The app can only see what `MOVIE_COLS` selects. Replace lines **407-408**:

```js
const MOVIE_COLS = `id, tmdb_id, imdb_id, title, overview, poster_path, backdrop_path,
                    release_year, runtime, tmdb_rating, imdb_rating, "cast", genres, created_at`;
```

with:

```js
const MOVIE_COLS = `id, tmdb_id, imdb_id, title, overview, poster_path, backdrop_path,
                    release_year, runtime, tmdb_rating, imdb_rating, "cast", genres, created_at,
                    rt_rating, metacritic, budget, revenue, trailer_key, director, director_tmdb_id`;
```

## Edit 2 — line 1064: let the fields through the existing update endpoint

Inside `handleAdminUpdateMovie`, replace the whitelist line:

```js
    for (const k of ['title', 'overview', 'poster_path', 'backdrop_path', 'release_year', 'runtime', 'tmdb_rating', 'imdb_rating', 'cast', 'genres']) {
```

with:

```js
    for (const k of ['title', 'overview', 'poster_path', 'backdrop_path', 'release_year', 'runtime', 'tmdb_rating', 'imdb_rating', 'cast', 'genres', 'rt_rating', 'metacritic', 'budget', 'revenue', 'trailer_key', 'director', 'director_tmdb_id']) {
```

## Edit 3 — after line 1073: paste the batch handler

`handleAdminUpdateMovie` ends at line 1073. Paste this **immediately after that
closing brace**, before the `// SYNC SERIES SEASONS` comment on line 1075:

```js
// ============================================================================
// ADMIN - ENRICH (Rotten Tomatoes + Metacritic + box office + trailer)
// POST /admin/enrich   { ids: [1,2,3] }
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

        // Budget / revenue / trailer / director come from TMDB.
        const tmdbRes = await fetch(
            `https://api.themoviedb.org/3/movie/${movie.tmdb_id}?api_key=${cfg(env, 'TMDB_API_KEY')}&append_to_response=videos,credits`,
            { headers: { Accept: 'application/json' } }
        );
        const tmdb = tmdbRes.ok ? await tmdbRes.json() : {};
        const trailer = (tmdb.videos?.results || [])
            .find(v => v.site === 'YouTube' && v.type === 'Trailer');
        const director = (tmdb.credits?.crew || []).find(c => c.job === 'Director');

        // Rotten Tomatoes + Metacritic come from OMDb.
        let rt = null;
        let metacritic = null;
        const imdbId = movie.imdb_id || tmdb.imdb_id;
        if (imdbId) {
            const omdb = await omdbGet(imdbId, env);
            if (omdb && omdb.Response !== 'False') {
                const rtEntry = (omdb.Ratings || []).find(r => r.Source === 'Rotten Tomatoes');
                rt = rtEntry ? rtEntry.Value : null;
                const mc = parseInt(omdb.Metascore, 10);
                metacritic = Number.isNaN(mc) ? null : mc;
            }
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
```

## Edit 4 — line 2533: register the route

Inside the `ROUTES` array (starts at line 2484). Add one line right after:

```js
    ['PATCH', '/admin/movies/:id', handleAdminUpdateMovie, 'admin'],   // line 2533
```

so it reads:

```js
    ['PATCH', '/admin/movies/:id', handleAdminUpdateMovie, 'admin'],
    ['POST', '/admin/enrich', handleAdminEnrich, 'admin'],
```

Then **deploy** the worker.

---

## SQL — run in Turso

```sql
ALTER TABLE movies ADD COLUMN rt_rating    TEXT;
ALTER TABLE movies ADD COLUMN metacritic   INTEGER;
ALTER TABLE movies ADD COLUMN budget       INTEGER;
ALTER TABLE movies ADD COLUMN revenue      INTEGER;
ALTER TABLE movies ADD COLUMN trailer_key  TEXT;
ALTER TABLE movies ADD COLUMN director      TEXT;
ALTER TABLE movies ADD COLUMN director_tmdb_id INTEGER;
```

**`SQLite error: duplicate column name: director` is not a problem.** It means
that column already exists — skip it and run the rest. `ALTER TABLE ... ADD
COLUMN` is purely additive: it never rewrites, moves or drops existing rows, so
existing data cannot be affected. Run one statement at a time and ignore only
the "duplicate column" errors; any other error is real.

---

## Run the enrichment

```bash
curl -X POST https://beamplay.beam-api.workers.dev/admin/enrich \
  -H "content-type: application/json" \
  -H "authorization: Bearer <admin user's JWT>" \
  -d '{"ids":[1,2,3,4,5]}'
```

The route is `authMode: 'admin'`, so it uses the worker's existing admin check —
the same token your admin screens already use. Batch 50 ids per call.

---

## Verify

```bash
curl "https://beamplay.beam-api.workers.dev/movies/1" | grep -o 'rt_rating[^,]*'
```

`"rt_rating":"93%"` means the app will show the Rotten Tomatoes and Metacritic
cards the next time that title is opened — no app rebuild needed, because
`MediaItem` already declares those fields and the cards render only when the
value is non-null. The Trailer button is independent of all this: it appears
whenever TMDB returns a YouTube trailer, which the app already fetches live.
