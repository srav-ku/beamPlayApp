# BeamStream - schema redesign plan (cinephile layer)

Reviewed 2026-09-17. Inputs: BeamStream live schema + CineCollection (cinephile) schema.
Rule for everything below: **additive first, never destructive, nothing renamed.**

## 1. What the two schemas are

| | BeamStream (live) | CineCollection (cinephile) |
|---|---|---|
| Identity | `users` INTEGER autoincrement, email, role, password_hash, trakt_access_token | `users` TEXT cuid, email, name, avatar_url |
| Catalog | `movies` + `series` + `seasons` + `episodes` + `movie_links` + `episode_links` | one `movies` table carrying movies *and* tv (`media_type`, `source`) |
| Metadata breadth | title, year, runtime, tmdb/imdb rating, cast, genres | + original_title, director, writer, rotten_tomatoes, metacritic, tvmaze_id, jikan_id, external_ids, cache stamps |
| Personal | `user_library` (list_type), `user_playback`, `user_settings` | `collections` + `collection_items`, `user_movie_data` (watched, rating, notes, favourite, tags) |
| Ops | `download_files`, `bot_sessions`, `rate_limits`, `reports`, `requests` | `search_cache`, `api_rate_limits` |
| Social | - | follow / followers / feed (UI exists; no table in the dump) |

## 2. Collisions, and how we resolve them

**`users` - ID type mismatch (the one real decision).** BeamStream is INTEGER, cinephile is TEXT cuid.
Resolution: BeamStream `users` stays the **single identity table** (Google + guest login already
work on it and we are not touching auth). Add `name`, `avatar_url`, `is_guest`. Every cinephile
table that referenced a TEXT user id is re-keyed to INTEGER with a foreign key to `users(id)`.
Cinephile has no real users (a demo row only), so this costs nothing now and would be expensive later.

**`movies` - do NOT merge into one table.** Cinephile's single `movies` table is convenient for a
collection app, but `seasons`, `episodes`, `episode_links`, `download_files` and both bots all
depend on BeamStream's split. Instead **widen** BeamStream's `movies`/`series` with the metadata
columns the cinephile side carries. The catalog stays canonical; the extra fields are additive.

## 3. Phase A - widen the catalog (safe, additive)

```sql
ALTER TABLE users ADD COLUMN name TEXT;
ALTER TABLE users ADD COLUMN avatar_url TEXT;
ALTER TABLE users ADD COLUMN is_guest INTEGER NOT NULL DEFAULT 0;

ALTER TABLE movies ADD COLUMN original_title TEXT;
ALTER TABLE movies ADD COLUMN director TEXT;
ALTER TABLE movies ADD COLUMN writer TEXT;
ALTER TABLE movies ADD COLUMN rotten_tomatoes TEXT;
ALTER TABLE movies ADD COLUMN metacritic INTEGER;
ALTER TABLE movies ADD COLUMN tvmaze_id INTEGER;
ALTER TABLE movies ADD COLUMN jikan_id INTEGER;
ALTER TABLE movies ADD COLUMN external_ids TEXT;
ALTER TABLE movies ADD COLUMN source TEXT;
ALTER TABLE movies ADD COLUMN last_refreshed_at INTEGER;
ALTER TABLE movies ADD COLUMN refresh_count INTEGER NOT NULL DEFAULT 0;

-- same set on series, minus runtime/rating differences
ALTER TABLE series ADD COLUMN director TEXT;
ALTER TABLE series ADD COLUMN writer TEXT;
ALTER TABLE series ADD COLUMN rotten_tomatoes TEXT;
ALTER TABLE series ADD COLUMN metacritic INTEGER;
ALTER TABLE series ADD COLUMN tvmaze_id INTEGER;
ALTER TABLE series ADD COLUMN jikan_id INTEGER;
ALTER TABLE series ADD COLUMN external_ids TEXT;
ALTER TABLE series ADD COLUMN source TEXT;
ALTER TABLE series ADD COLUMN last_refreshed_at INTEGER;
ALTER TABLE series ADD COLUMN refresh_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE user_settings ADD COLUMN default_audio_language TEXT;
ALTER TABLE user_settings ADD COLUMN boost_speed REAL NOT NULL DEFAULT 2.0;
ALTER TABLE user_settings ADD COLUMN auto_play_next INTEGER NOT NULL DEFAULT 1;

ALTER TABLE user_playback ADD COLUMN device_id TEXT;
ALTER TABLE user_playback ADD COLUMN completed_at INTEGER;
```

Every column is nullable or defaulted, so the admin panel, the worker and both bots keep working
without a single code change, and no existing row is rewritten.

## 4. Phase B - the cinephile layer (new tables only)

```sql
CREATE TABLE IF NOT EXISTS collections (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id        INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name           TEXT NOT NULL,
    description    TEXT,
    is_public      INTEGER NOT NULL DEFAULT 0,
    is_default     INTEGER NOT NULL DEFAULT 0,
    sort_order     INTEGER NOT NULL DEFAULT 0,
    cover_media_type TEXT,
    cover_media_id INTEGER,
    created_at     INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at     INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (user_id, name)
);
CREATE INDEX IF NOT EXISTS idx_collections_user ON collections(user_id);

-- Polymorphic on purpose: a list must be able to hold a movie, a series OR a single episode.
CREATE TABLE IF NOT EXISTS collection_items (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    collection_id INTEGER NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    media_type    TEXT NOT NULL,            -- 'movie' | 'series' | 'episode'
    media_id      INTEGER NOT NULL,         -- movies.id / series.id / episodes.id
    season_number INTEGER,
    episode_number INTEGER,
    position      INTEGER NOT NULL DEFAULT 0,
    note          TEXT,
    added_at      INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (collection_id, media_type, media_id)
);
CREATE INDEX IF NOT EXISTS idx_ci_collection ON collection_items(collection_id, position);

CREATE TABLE IF NOT EXISTS user_title_data (
    user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_type      TEXT NOT NULL,
    media_id        INTEGER NOT NULL,
    watched         INTEGER NOT NULL DEFAULT 0,
    watched_date    INTEGER,
    watch_count     INTEGER NOT NULL DEFAULT 0,
    personal_rating INTEGER,                -- 1-10
    notes           TEXT,
    is_favorite     INTEGER NOT NULL DEFAULT 0,
    custom_tags     TEXT,                   -- JSON array
    updated_at      INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (user_id, media_type, media_id)
);
CREATE INDEX IF NOT EXISTS idx_utd_watched ON user_title_data(user_id, watched);

CREATE TABLE IF NOT EXISTS follows (
    follower_id  INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (follower_id, following_id)
);

-- The ~20 curated presets from the cinephile site ("Oscar Winners", "Hidden Gems", ...)
CREATE TABLE IF NOT EXISTS collection_templates (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL UNIQUE,
    description TEXT,
    emoji       TEXT,
    query_json  TEXT NOT NULL,   -- filter spec: genres / decade / country / studio / franchise
    sort_order  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS search_cache (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    query_hash  TEXT NOT NULL UNIQUE,
    query       TEXT NOT NULL,
    search_type TEXT NOT NULL,
    results     TEXT NOT NULL,
    source      TEXT,
    cached_at   INTEGER NOT NULL DEFAULT (unixepoch()),
    expires_at  INTEGER
);

CREATE TABLE IF NOT EXISTS api_rate_limits (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    api_name      TEXT NOT NULL,
    date          TEXT NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0,
    UNIQUE (api_name, date)
);
```

## 5. Phase C - fold in what exists

`user_library` (user_id, media_id, list_type) already holds watchlist/favourite-ish rows. Do not
drop it. Migrate: one `collections` row per (user, list_type) -> `collection_items` referencing
`user_title_data`/`collections`. Keep `user_library` readable until the clients move over, then
retire it in a later phase. Same for `user_playback`: it already has progress, episode ids and
`is_finished`, so cross-device resume needs only the two new columns from Phase A.

## 6. Risk list - what must not break

| Consumer | Depends on | Verdict |
|---|---|---|
| Admin panel | `movies`, `series`, `seasons`, `episodes`, `movie_links`, `episode_links`, `users.role` | Safe: additive only |
| Admin bot | `download_files` (unique `file_unique_id`, `content_type` + `content_id`), `bot_sessions` | Untouched |
| User bot | `rate_limits`, `download_files.channel_msg_id` | Untouched |
| Web + app | `user_library`, `user_playback`, `user_settings` | Additive; new columns defaulted |
| Worker | `movies.title_lower`, `series.title_lower` indexes | Untouched |

Known blemish in the current dump: `user_library` is defined **twice** in the schema file. Harmless
at runtime, but re-running the file on a fresh DB will error. Worth cleaning the file, not the DB.

## 7. Backup before touching anything

```bash
# local backup of the whole database (run this FIRST, keep the file)
turso db shell <db-name> ".dump" > beamstream-backup-$(date +%F).sql

# a read-only token is enough for inspection/backup
turso db tokens create <db-name> --read-only
```

Order of operations: dump -> verify the dump restores into a scratch DB -> apply Phase A ->
apply Phase B -> only then migrate Phase C data.