-- ============================================================================
-- BeamBot schema v2 - additive only. Nothing dropped, nothing renamed.
-- Run on the Turso BRANCH first, verify, then on main.
--
-- Note: libSQL has no "ADD COLUMN IF NOT EXISTS". Check first, e.g.
--   SELECT name FROM pragma_table_info('users') WHERE name='name';
-- then run the matching ALTER. Every column is nullable or defaulted, so the
-- admin panel, the admin/user bots and the worker keep working untouched.
-- ============================================================================

-- ── 1. identity ─────────────────────────────────────────────────────────────
ALTER TABLE users ADD COLUMN name           TEXT;
ALTER TABLE users ADD COLUMN avatar_url     TEXT;
ALTER TABLE users ADD COLUMN is_guest       INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_seen_at   INTEGER;
ALTER TABLE users ADD COLUMN updated_at     INTEGER;

-- ── 2. settings (global per user, syncs across devices) ─────────────────────
ALTER TABLE user_settings ADD COLUMN default_audio_language TEXT;
ALTER TABLE user_settings ADD COLUMN boost_speed            REAL NOT NULL DEFAULT 2.0;
ALTER TABLE user_settings ADD COLUMN auto_play_next         INTEGER NOT NULL DEFAULT 1;
ALTER TABLE user_settings ADD COLUMN preferred_quality      TEXT;
ALTER TABLE user_settings ADD COLUMN subtitle_enabled       INTEGER NOT NULL DEFAULT 1;

-- ── 3. catalog: what a cinephile app needs on top of title/year/rating ─────
ALTER TABLE movies ADD COLUMN original_title     TEXT;
ALTER TABLE movies ADD COLUMN director           TEXT;
ALTER TABLE movies ADD COLUMN writer             TEXT;
ALTER TABLE movies ADD COLUMN country            TEXT;
ALTER TABLE movies ADD COLUMN language           TEXT;
ALTER TABLE movies ADD COLUMN studio             TEXT;
ALTER TABLE movies ADD COLUMN franchise_id       INTEGER;
ALTER TABLE movies ADD COLUMN rotten_tomatoes    TEXT;
ALTER TABLE movies ADD COLUMN metacritic         INTEGER;
ALTER TABLE movies ADD COLUMN keywords           TEXT;   -- JSON array
ALTER TABLE movies ADD COLUMN trailer_url        TEXT;
ALTER TABLE movies ADD COLUMN status             TEXT;   -- released | upcoming
ALTER TABLE movies ADD COLUMN source             TEXT;
ALTER TABLE movies ADD COLUMN external_ids       TEXT;   -- JSON
ALTER TABLE movies ADD COLUMN last_refreshed_at  INTEGER;
ALTER TABLE movies ADD COLUMN refresh_count      INTEGER NOT NULL DEFAULT 0;

ALTER TABLE series ADD COLUMN original_title     TEXT;
ALTER TABLE series ADD COLUMN director           TEXT;
ALTER TABLE series ADD COLUMN writer             TEXT;
ALTER TABLE series ADD COLUMN country            TEXT;
ALTER TABLE series ADD COLUMN language           TEXT;
ALTER TABLE series ADD COLUMN studio             TEXT;
ALTER TABLE series ADD COLUMN franchise_id       INTEGER;
ALTER TABLE series ADD COLUMN rotten_tomatoes    TEXT;
ALTER TABLE series ADD COLUMN metacritic         INTEGER;
ALTER TABLE series ADD COLUMN keywords           TEXT;
ALTER TABLE series ADD COLUMN trailer_url        TEXT;
ALTER TABLE series ADD COLUMN status             TEXT;
ALTER TABLE series ADD COLUMN source             TEXT;
ALTER TABLE series ADD COLUMN external_ids       TEXT;
ALTER TABLE series ADD COLUMN last_refreshed_at  INTEGER;
ALTER TABLE series ADD COLUMN refresh_count      INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_movies_release_year ON movies (release_year);
CREATE INDEX IF NOT EXISTS idx_movies_status       ON movies (status);
CREATE INDEX IF NOT EXISTS idx_series_year         ON series (first_release_year);

-- ── 4. playback: device awareness + clean completion ───────────────────────
ALTER TABLE user_playback ADD COLUMN device_id    TEXT;
ALTER TABLE user_playback ADD COLUMN completed_at INTEGER;
ALTER TABLE user_playback ADD COLUMN source_url   TEXT;   -- stable link that was played

-- ── 5. library: collections ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS collections (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id          INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name             TEXT NOT NULL,
    description      TEXT,
    is_public        INTEGER NOT NULL DEFAULT 0,
    is_default       INTEGER NOT NULL DEFAULT 0,
    sort_order       INTEGER NOT NULL DEFAULT 0,
    cover_media_type TEXT,
    cover_media_id   INTEGER,
    created_at       INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at       INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (user_id, name)
);
CREATE INDEX IF NOT EXISTS idx_collections_user ON collections (user_id);

-- Polymorphic: a list must hold a movie, a series OR a single episode.
CREATE TABLE IF NOT EXISTS collection_items (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    collection_id INTEGER NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    media_type    TEXT NOT NULL,          -- 'movie' | 'series' | 'episode'
    media_id      INTEGER NOT NULL,
    season_number  INTEGER,
    episode_number INTEGER,
    position      INTEGER NOT NULL DEFAULT 0,
    note          TEXT,
    added_at      INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (collection_id, media_type, media_id)
);
CREATE INDEX IF NOT EXISTS idx_ci_collection ON collection_items (collection_id, position);

-- Curated presets ("Oscar Winners", "Hidden Gems", ...) as data, not code.
CREATE TABLE IF NOT EXISTS collection_templates (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL UNIQUE,
    description TEXT,
    emoji       TEXT,
    query_json  TEXT NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

-- ── 6. tracking: ratings, notes, favourites, per-episode watched ───────────
CREATE TABLE IF NOT EXISTS user_title_data (
    user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_type      TEXT NOT NULL,
    media_id        INTEGER NOT NULL,
    watched         INTEGER NOT NULL DEFAULT 0,
    watched_date    INTEGER,
    watch_count     INTEGER NOT NULL DEFAULT 0,
    personal_rating INTEGER,               -- 1-10
    notes           TEXT,
    is_favorite     INTEGER NOT NULL DEFAULT 0,
    custom_tags     TEXT,                  -- JSON array
    updated_at      INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (user_id, media_type, media_id)
);
CREATE INDEX IF NOT EXISTS idx_utd_watched ON user_title_data (user_id, watched);

CREATE TABLE IF NOT EXISTS user_episode_data (
    user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    episode_id    INTEGER NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    watched       INTEGER NOT NULL DEFAULT 0,
    watched_date  INTEGER,
    updated_at    INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (user_id, episode_id)
);

-- ── 7. franchises (for "Star Wars / Marvel / DC" style shelves) ────────────
CREATE TABLE IF NOT EXISTS franchises (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL UNIQUE,
    tmdb_id     INTEGER,
    description TEXT,
    backdrop_path TEXT
);

-- ── 8. social (data now, UI later) ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS follows (
    follower_id  INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (follower_id, following_id)
);
CREATE INDEX IF NOT EXISTS idx_follows_following ON follows (following_id);

-- ── 9. devices (sync + "continue on your other device") ───────────────────
CREATE TABLE IF NOT EXISTS devices (
    id           TEXT PRIMARY KEY,
    user_id      INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform     TEXT,                     -- android | web | tv
    name         TEXT,
    last_seen_at INTEGER,
    created_at   INTEGER NOT NULL DEFAULT (unixepoch())
);
CREATE INDEX IF NOT EXISTS idx_devices_user ON devices (user_id);

-- ── 10. ops: caching, quotas, notifications ──────────────────────────────
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
CREATE INDEX IF NOT EXISTS idx_search_cache_expires ON search_cache (expires_at);

CREATE TABLE IF NOT EXISTS api_rate_limits (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    api_name      TEXT NOT NULL,
    date          TEXT NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0,
    UNIQUE (api_name, date)
);

CREATE TABLE IF NOT EXISTS notifications (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind       TEXT NOT NULL,              -- new_episode | request_status | ...
    title      TEXT,
    body       TEXT,
    payload    TEXT,                       -- JSON
    read_at    INTEGER,
    created_at INTEGER NOT NULL DEFAULT (unixepoch())
);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications (user_id, read_at);

-- ── 11. tighten existing queues (additive, no data loss) ──────────────────
ALTER TABLE requests ADD COLUMN user_id    INTEGER;
ALTER TABLE requests ADD COLUMN admin_note TEXT;
ALTER TABLE reports  ADD COLUMN user_id    INTEGER;
ALTER TABLE reports  ADD COLUMN admin_note TEXT;

-- ============================================================================
-- Order of operations
--   1. confirm the Turso branch exists and is current (a branch is a
--      copy-on-write clone, so it is a valid rollback point)
--   2. run this file against the BRANCH
--   3. verify: SELECT name FROM pragma_table_info('users');  -- expect new cols
--   4. then run it against main
-- Nothing here deletes or rewrites a single existing row.
-- ============================================================================