# Browse filter patch — two small worker additions

The new filter sheet does two things your worker can't serve yet: a **year range**
(the slider picks 1988–2026, not one year) and a **live result count** ("Show 327
Results"). Both need a few lines. Until you deploy this, the sheet still works —
the range simply behaves as "no year filter" and the button reads "Show Results"
without a number.

Line numbers are from the worker around lines 383–508; they have not moved since
the enrich work, which was all added later in the file.

---

## 1. `buildWhereClause` — accept a year range (line 383)

Replace the function signature and the `year` block:

```js
function buildWhereClause(q, genre, year, language) {
```

becomes

```js
function buildWhereClause(q, genre, year, language, yearFrom, yearTo) {
```

and after the existing `if (year) { ... }` line, add:

```js
    if (yearFrom) { conds.push('release_year >= ?'); args.push(parseInt(yearFrom, 10)); }
    if (yearTo) { conds.push('release_year <= ?'); args.push(parseInt(yearTo, 10)); }
```

## 2. `buildSeriesWhereClause` — the same for series (line 393)

```js
function buildSeriesWhereClause(q, genre, year, language) {
```

becomes

```js
function buildSeriesWhereClause(q, genre, year, language, yearFrom, yearTo) {
```

and after its `if (year) { ... }` line:

```js
    if (yearFrom) { conds.push('first_release_year >= ?'); args.push(parseInt(yearFrom, 10)); }
    if (yearTo) { conds.push('first_release_year <= ?'); args.push(parseInt(yearTo, 10)); }
```

---

## 3. `handleListMovies` — pass the range through and return a total (line 417)

Replace the last three lines of the function:

```js
    const { where, args } = buildWhereClause(query.q, query.genre, query.year, query.language);
    const rows = await tursoQueryAll(`SELECT ${MOVIE_COLS} FROM movies ${where} ORDER BY ${orderBy} LIMIT ? OFFSET ?`, [...args, limit, offset], env);
    return json({ page, limit, items: rows }, 200, env);
```

with:

```js
    const { where, args } = buildWhereClause(query.q, query.genre, query.year, query.language, query.year_from, query.year_to);
    const rows = await tursoQueryAll(`SELECT ${MOVIE_COLS} FROM movies ${where} ORDER BY ${orderBy} LIMIT ? OFFSET ?`, [...args, limit, offset], env);
    // One extra COUNT so the app can label the apply button with a real number.
    const counted = await tursoQueryOne(`SELECT COUNT(*) AS total FROM movies ${where}`, args, env);
    return json({ page, limit, total: counted ? counted.total : rows.length, items: rows }, 200, env);
```

## 4. `handleListSeries` — identical treatment (line 496)

```js
    const { where, args } = buildSeriesWhereClause(query.q, query.genre, query.year, query.language);
    const rows = await tursoQueryAll(`SELECT ${SERIES_COLS} FROM series ${where} ORDER BY ${orderBy} LIMIT ? OFFSET ?`, [...args, limit, offset], env);
    return json({ page, limit, items: rows }, 200, env);
```

becomes

```js
    const { where, args } = buildSeriesWhereClause(query.q, query.genre, query.year, query.language, query.year_from, query.year_to);
    const rows = await tursoQueryAll(`SELECT ${SERIES_COLS} FROM series ${where} ORDER BY ${orderBy} LIMIT ? OFFSET ?`, [...args, limit, offset], env);
    const counted = await tursoQueryOne(`SELECT COUNT(*) AS total FROM series ${where}`, args, env);
    return json({ page, limit, total: counted ? counted.total : rows.length, items: rows }, 200, env);
```

---

## Deploy, then check

```bash
# range: 2010..2019 -> should return only those years, with a total
curl "https://beamplay.beam-api.workers.dev/movies?page=1&limit=3&year_from=2010&year_to=2019"
```

Look for `"total": <number>` in the response. No schema change, no data touched —
this is read-only query work.

## App side (already shipped)

`Api.movies(...)` / `Api.seriesList(...)` now send `year_from` / `year_to`, and
`MediaPage` reads `total`. So the moment this patch is deployed, the slider's
range starts filtering and the button shows "Show N Results" — no app rebuild.
