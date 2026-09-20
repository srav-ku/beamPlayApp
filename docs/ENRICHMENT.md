# Enrichment job — filling the remaining Detail cards

The Detail screen is built to render four rating cards (TMDB, IMDb, Rotten
Tomatoes, Metacritic) and a Box Office row (Budget, Revenue, Profit). TMDB and
IMDb come from data we already have. The other three need enrichment:

- **Rotten Tomatoes** and **Metacritic** are *not* in the TMDB API. OMDb returns
  both (`Ratings[]` contains Rotten Tomatoes; `Metascore` is Metacritic).
- **Budget / Revenue** are in TMDB's `movie/{id}` response (`budget`, `revenue`).

## 1. Schema (additive — no existing data is touched)

```sql
-- Safe to re-run: each statement is guarded by the column check in the runner.
ALTER TABLE movies ADD COLUMN rt_rating    TEXT;     -- "93%"  (Rotten Tomatoes)
ALTER TABLE movies ADD COLUMN metacritic   INTEGER;  -- 77     (Metacritic)
ALTER TABLE movies ADD COLUMN budget       INTEGER;  -- USD
ALTER TABLE movies ADD COLUMN revenue      INTEGER;  -- USD
ALTER TABLE movies ADD COLUMN trailer_key  TEXT;     -- YouTube key
ALTER TABLE movies ADD COLUMN director      TEXT;    -- "Christopher Nolan"
ALTER TABLE movies ADD COLUMN director_tmdb_id INTEGER;
```

These are `ADD COLUMN` only: existing rows keep their values, and every new
column is nullable, so nothing that reads `movies` today changes behaviour.
The rollback path is the existing `beambot-backup-2026-09-17` branch.

## 2. Worker endpoint (paste into the worker, then deploy)

```js
// POST /admin/enrich  { ids: [1,2,3] }  - header x-admin-key must equal ADMIN_KEY
async function handleAdminEnrich(request, env) {
    const key = request.headers.get('x-admin-key');
    if (!key || key !== cfg(env, 'ADMIN_KEY')) return errJson('forbidden', 403, env);

    const body = await readBody(request);
    const ids = Array.isArray(body.ids) ? body.ids : [];
    const omdbKey = cfg(env, 'OMDB_API_KEY');
    let updated = 0;

    for (const id of ids) {
        const movie = await tursoQueryOne(
            'SELECT id, tmdb_id, imdb_id FROM movies WHERE id = ?', [id], env
        );
        if (!movie || !movie.tmdb_id) continue;

        // Budget / revenue come from TMDB itself.
        const tmdb = await (await fetch(
            `https://api.themoviedb.org/3/movie/${movie.tmdb_id}?api_key=${cfg(env, 'TMDB_API_KEY')}`
        )).json();

        // RT + Metacritic come from OMDb, keyed by IMDb id.
        let rt = null, metacritic = null;
        if (omdbKey && movie.imdb_id) {
            const omdb = await (await fetch(
                `https://www.omdbapi.com/?apikey=${omdbKey}&i=${movie.imdb_id}`
            )).json();
            if (omdb && omdb.Response !== 'False') {
                metacritic = parseInt(omdb.Metascore, 10);
                if (isNaN(metacritic)) metacritic = null;
                const rtEntry = (omdb.Ratings || []).find(r => r.Source === 'Rotten Tomatoes');
                rt = rtEntry ? rtEntry.Value : null;
            }
        }

        const trailer = (tmdb.videos?.results || [])
            .find(v => v.site === 'YouTube' && v.type === 'Trailer');

        await tursoRun(
            `UPDATE movies SET
                rt_rating = COALESCE(?, rt_rating),
                metacritic = COALESCE(?, metacritic),
                budget = COALESCE(?, budget),
                revenue = COALESCE(?, revenue),
                trailer_key = COALESCE(?, trailer_key)
             WHERE id = ?`,
            [rt, metacritic, tmdb.budget || null, tmdb.revenue || null,
             trailer ? trailer.key : null, id],
            env
        );
        updated++;
    }
    return json({ ok: true, updated }, 200, env);
}
```

Route it next to the other admin route:

```js
if (path === '/admin/enrich' && method === 'POST') return handleAdminEnrich(request, env);
```

Environment variables to add (as *variables*, like `FIREBASE_PROJECT_ID`):
`OMDB_API_KEY`, `TMDB_API_KEY`, `ADMIN_KEY`.

## 3. Include the new columns in the responses the app reads

`SELECT *` on `movies` already returns them. If the movie list/detail handlers
enumerate columns explicitly, add `rt_rating, metacritic, budget, revenue,
trailer_key, director, director_tmdb_id`.

## 4. What changes in the app

Nothing further: `MediaItem` already declares `rt_rating`, `metacritic`,
`budget` and `revenue`. The Detail screen renders those cards **only when the
value is non-null**, so the moment the worker starts returning them they appear.
The Trailer button behaves the same way — it exists on screen only when TMDB
returns a YouTube trailer, which the app already fetches live.

## 5. Running it

```bash
# enrich one batch
curl -X POST https://beamplay.beam-api.workers.dev/admin/enrich \
  -H "content-type: application/json" \
  -H "x-admin-key: $ADMIN_KEY" \
  -d '{"ids":[1,2,3,4,5]}'
```

Batches keep each request inside the worker CPU limit; a few hundred ids is a
reasonable schedule (cron every few minutes) until the catalog is covered.

## Note on what is *not* here

Cast photos, director id and trailer keys come from TMDB and already work in the
app. Only RT, Metacritic and box office need this job — and RT/Metacritic
specifically need an OMDb key, which is why this is the one piece that needs a
credential from you.
