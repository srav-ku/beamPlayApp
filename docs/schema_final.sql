-- ============================================================================
-- BeamBot - FINAL schema (supersedes docs/schema_v2.sql)
-- Additive only: nothing dropped, renamed or rewritten. Run top to bottom once,
-- on the Turso BRANCH first if you have a branch token, then on main.
--
-- libSQL has no "ADD COLUMN IF NOT EXISTS": check first, e.g.
--   SELECT name FROM pragma_table_info('users') WHERE name='name';
-- ============================================================================

-- ── 1. IDENTITY ─────────────────────────────────────────────────────────────
ALTER TABLE users ADD COLUMN name                      TEXT;
ALTER TABLE users ADD COLUMN avatar_url                TEXT;
ALTER TABLE users ADD COLUMN is_guest                  INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_seen_at              INTEGER;
ALTER TABLE users ADD COLUMN updated_at                INTEGER;
ALTER TABLE users ADD COLUMN theme                     TEXT;     -- 'dark' | 'light'
ALTER TABLE users ADD COLUMN is_history_public         INTEGER NOT NULL DEFAULT 1;
ALTER TABLE users ADD COLUMN recovery_passphrase_hash  TEXT;     -- only if email+password login is ever added
ALTER TABLE users ADD COLUMN trakt_refresh_token       TEXT;
ALTER TABLE users ADD COLUMN trakt_username            TEXT;
ALTER TABLE users ADD COLUMN trakt_last_sync_at        INTEGER;

-- ── 2. SETTINGS (global per user, syncs across devices) ─────────────────────
ALTER TABLE user_settings ADD COLUMN default_audio_language TEXT;
ALTER TABLE user_settings ADD COLUMN boost_speed            REAL NOT NULL DEFAULT 2.0;
ALTER TABLE user_settings ADD COLUMN auto_play_next         INTEGER NOT NULL DEFAULT 1;
ALTER TABLE user_settings ADD COLUMN preferred_quality      TEXT;
ALTER TABLE user_settings ADD COLUMN subtitle_enabled       INTEGER NOT NULL DEFAULT 1;

-- ── 3. CATALOG ──────────────────────────────────────────────────────────────
ALTER TABLE movies ADD COLUMN original_title       TEXT;
ALTER TABLE movies ADD COLUMN release_date         TEXT;
ALTER TABLE movies ADD COLUMN director             TEXT;
ALTER TABLE movies ADD COLUMN writer               TEXT;
ALTER TABLE movies ADD COLUMN country              TEXT;
ALTER TABLE movies ADD COLUMN language             TEXT;
ALTER TABLE movies ADD COLUMN studio               TEXT;
ALTER TABLE movies ADD COLUMN franchise_id         INTEGER;
ALTER TABLE movies ADD COLUMN tagline              TEXT;
ALTER TABLE movies ADD COLUMN budget               INTEGER;
ALTER TABLE movies ADD COLUMN revenue              INTEGER;
ALTER TABLE movies ADD COLUMN homepage             TEXT;
ALTER TABLE movies ADD COLUMN vote_count           INTEGER;
ALTER TABLE movies ADD COLUMN popularity           REAL;
ALTER TABLE movies ADD COLUMN rotten_tomatoes      TEXT;
ALTER TABLE movies ADD COLUMN metacritic           INTEGER;
ALTER TABLE movies ADD COLUMN keywords             TEXT;
ALTER TABLE movies ADD COLUMN trailer_key          TEXT;
ALTER TABLE movies ADD COLUMN is_anime             INTEGER NOT NULL DEFAULT 0;
ALTER TABLE movies ADD COLUMN status               TEXT;
ALTER TABLE movies ADD COLUMN source               TEXT;
ALTER TABLE movies ADD COLUMN external_ids         TEXT;
ALTER TABLE movies ADD COLUMN recommendations_json TEXT;
ALTER TABLE movies ADD COLUMN last_refreshed_at    INTEGER;
ALTER TABLE movies ADD COLUMN refresh_count        INTEGER NOT NULL DEFAULT 0;

ALTER TABLE series ADD COLUMN original_title       TEXT;
ALTER TABLE series ADD COLUMN first_air_date       TEXT;
ALTER TABLE series ADD COLUMN last_air_date        TEXT;
ALTER TABLE series ADD COLUMN director             TEXT;
ALTER TABLE series ADD COLUMN writer               TEXT;
ALTER TABLE series ADD COLUMN country              TEXT;
ALTER TABLE series ADD COLUMN language             TEXT;
ALTER TABLE series ADD COLUMN studio               TEXT;
ALTER TABLE series ADD COLUMN franchise_id         INTEGER;
ALTER TABLE series ADD COLUMN tagline              TEXT;
ALTER TABLE series ADD COLUMN homepage             TEXT;
ALTER TABLE series ADD COLUMN vote_count           INTEGER;
ALTER TABLE series ADD COLUMN popularity           REAL;
ALTER TABLE series ADD COLUMN rotten_tomatoes      TEXT;
ALTER TABLE series ADD COLUMN metacritic           INTEGER;
ALTER TABLE series ADD COLUMN keywords             TEXT;
ALTER TABLE series ADD COLUMN trailer_key          TEXT;
ALTER TABLE series ADD COLUMN is_anime             INTEGER NOT NULL DEFAULT 0;
ALTER TABLE series ADD COLUMN status               TEXT;
ALTER TABLE series ADD COLUMN source               TEXT;
ALTER TABLE series ADD COLUMN external_ids         TEXT;
ALTER TABLE series ADD COLUMN recommendations_json TEXT;
ALTER TABLE series ADD COLUMN last_refreshed_at    INTEGER;
ALTER TABLE series ADD COLUMN refresh_count        INTEGER NOT NULL DEFAULT 0;

ALTER TABLE episodes ADD COLUMN air_date    TEXT;
ALTER TABLE episodes ADD COLUMN rating      REAL;
ALTER TABLE episodes ADD COLUMN vote_count  INTEGER;

CREATE INDEX IF NOT EXISTS idx_movies_release_year ON movies (release_year);
CREATE INDEX IF NOT EXISTS idx_movies_popularity   ON movies (popularity DESC);
CREATE INDEX IF NOT EXISTS idx_movies_status       ON movies (status);
CREATE INDEX IF NOT EXISTS idx_movies_anime        ON movies (is_anime);
CREATE INDEX IF NOT EXISTS idx_series_year         ON series (first_release_year);
CREATE INDEX IF NOT EXISTS idx_series_popularity   ON series (popularity DESC);
CREATE INDEX IF NOT EXISTS idx_series_anime        ON series (is_anime);

-- ── 4. PEOPLE + CREDITS (Person detail page, "Top actors/directors") ───────
CREATE TABLE IF NOT EXISTS people (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    tmdb_id         INTEGER UNIQUE,
    name            TEXT NOT NULL,
    name_lower      TEXT,
    profile_path    TEXT,
    department      TEXT,          -- Acting | Directing | Writing | ...
    birthday        TEXT,
    place_of_birth  TEXT,
    biography       TEXT,
    popularity      REAL,
    known_for_json  TEXT,
    created_at      INTEGER NOT NULL DEFAULT (unixepoch())
);
CREATE INDEX IF NOT EXISTS idx_people_name ON people (name_lower);

CREATE TABLE IF NOT EXISTS credits (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    person_id    INTEGER NOT NULL REFERENCES people(id) ON DELETE CASCADE,
    media_type   TEXT NOT NULL,     -- 'movie' | 'series' | 'episode'
    media_id     INTEGER NOT NULL,
    character    TEXT,              -- for actors
    job          TEXT,              -- for crew (Director, Writer, ...)
    department   TEXT,
    credit_order INTEGER NOT NULL DEFAULT 0,
    UNIQUE (person_id, media_type, media_id, job, character)
);
CREATE INDEX IF NOT EXISTS idx_credits_media  ON credits (media_type, media_id, credit_order);
CREATE INDEX IF NOT EXISTS idx_credits_person ON credits (person_id);

-- ── 5. FRANCHISES ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS franchises (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT NOT NULL UNIQUE,
    tmdb_id       INTEGER,
    description   TEXT,
    backdrop_path TEXT
);

-- ── 6. LIBRARY: collections ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS collections (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id          INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name             TEXT NOT NULL,
    description      TEXT,
    is_public        INTEGER NOT NULL DEFAULT 0,
    is_default       INTEGER NOT NULL DEFAULT 0,   -- Watch Later / Favorites
    is_pinned        INTEGER NOT NULL DEFAULT 0,   -- max 3
    is_smart         INTEGER NOT NULL DEFAULT 0,   -- rule-driven, auto-updating
    smart_query_json TEXT,
    share_slug       TEXT UNIQUE,
    sort_order       INTEGER NOT NULL DEFAULT 0,
    cover_media_type TEXT,
    cover_media_id   INTEGER,
    created_at       INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at       INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (user_id, name)
);
CREATE INDEX IF NOT EXISTS idx_collections_user   ON collections (user_id);
CREATE INDEX IF NOT EXISTS idx_collections_public ON collections (is_public);

CREATE TABLE IF NOT EXISTS collection_items (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    collection_id  INTEGER NOT NULL REFERENCES collections(id) ON DELETE CASCADE,
    media_type     TEXT NOT NULL,          -- 'movie' | 'series' | 'episode'
    media_id       INTEGER NOT NULL,
    season_number  INTEGER,
    episode_number INTEGER,
    position       INTEGER NOT NULL DEFAULT 0,
    note           TEXT,
    added_by_id    INTEGER REFERENCES users(id) ON DELETE SET NULL,
    added_at       INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (collection_id, media_type, media_id)
);
CREATE INDEX IF NOT EXISTS idx_ci_collection ON collection_items (collection_id, position);

CREATE TABLE IF NOT EXISTS collection_templates (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL UNIQUE,
    description TEXT,
    emoji       TEXT,
    kind        TEXT NOT NULL DEFAULT 'list',   -- 'list' (fixed titles) | 'rule' (query)
    query_json  TEXT,                           -- used when kind='rule'
    sort_order  INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS collection_template_items (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    template_id INTEGER NOT NULL REFERENCES collection_templates(id) ON DELETE CASCADE,
    media_type  TEXT NOT NULL,
    media_id    INTEGER NOT NULL,
    position    INTEGER NOT NULL DEFAULT 0
);

-- ── 7. TRACKING (ratings are 1-5 to match the 5-star UI) ──────────────────
CREATE TABLE IF NOT EXISTS user_title_data (
    user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_type      TEXT NOT NULL,
    media_id        INTEGER NOT NULL,
    watched         INTEGER NOT NULL DEFAULT 0,
    watched_date    INTEGER,
    watch_count     INTEGER NOT NULL DEFAULT 0,
    personal_rating INTEGER,               -- 1-5
    notes           TEXT,
    is_favorite     INTEGER NOT NULL DEFAULT 0,
    custom_tags     TEXT,                  -- JSON array
    updated_at      INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (user_id, media_type, media_id)
);
CREATE INDEX IF NOT EXISTS idx_utd_watched ON user_title_data (user_id, watched);

CREATE TABLE IF NOT EXISTS user_episode_data (
    user_id      INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    episode_id   INTEGER NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    watched      INTEGER NOT NULL DEFAULT 0,
    watched_date INTEGER,
    updated_at   INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (user_id, episode_id)
);

-- ── 8. PLAYBACK (cross-device resume) ──────────────────────────────────────
ALTER TABLE user_playback ADD COLUMN device_id    TEXT;
ALTER TABLE user_playback ADD COLUMN completed_at INTEGER;
ALTER TABLE user_playback ADD COLUMN source_url   TEXT;

CREATE TABLE IF NOT EXISTS devices (
    id           TEXT PRIMARY KEY,
    user_id      INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform     TEXT,
    name         TEXT,
    last_seen_at INTEGER,
    created_at   INTEGER NOT NULL DEFAULT (unixepoch())
);
CREATE INDEX IF NOT EXISTS idx_devices_user ON devices (user_id);

-- ── 9. SOCIAL ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS follows (
    follower_id  INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (follower_id, following_id)
);
CREATE INDEX IF NOT EXISTS idx_follows_following ON follows (following_id);

CREATE TABLE IF NOT EXISTS activities (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind       TEXT NOT NULL,   -- watched | rated | favourited | added_to_list | created_list | started_following
    media_type TEXT,
    media_id   INTEGER,
    list_id    INTEGER,
    payload    TEXT,
    is_public  INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL DEFAULT (unixepoch())
);
CREATE INDEX IF NOT EXISTS idx_activities_user ON activities (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_activities_feed ON activities (created_at DESC, is_public);

CREATE TABLE IF NOT EXISTS user_series_follow (
    user_id           INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    series_id         INTEGER NOT NULL REFERENCES series(id) ON DELETE CASCADE,
    notify            INTEGER NOT NULL DEFAULT 1,
    last_seen_season  INTEGER,
    last_seen_episode INTEGER,
    created_at        INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (user_id, series_id)
);

-- ── 10. OPS: caches, quotas, notifications ─────────────────────────────────
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

-- generic provider cache: TMDB 1h, OMDb 24h, so quotas are never the bottleneck
CREATE TABLE IF NOT EXISTS provider_cache (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    provider   TEXT NOT NULL,           -- 'tmdb' | 'omdb' | 'trakt' | 'ai'
    cache_key  TEXT NOT NULL,
    payload    TEXT NOT NULL,
    cached_at  INTEGER NOT NULL DEFAULT (unixepoch()),
    expires_at INTEGER,
    UNIQUE (provider, cache_key)
);
CREATE INDEX IF NOT EXISTS idx_provider_cache_expires ON provider_cache (expires_at);

CREATE TABLE IF NOT EXISTS api_rate_limits (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    api_name      TEXT NOT NULL,
    date          TEXT NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0,
    UNIQUE (api_name, date)
);

CREATE TABLE IF NOT EXISTS ai_recommendations (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    query_hash TEXT NOT NULL UNIQUE,
    prompt     TEXT NOT NULL,
    payload    TEXT NOT NULL,           -- JSON: recommended titles
    cached_at  INTEGER NOT NULL DEFAULT (unixepoch()),
    expires_at INTEGER
);

CREATE TABLE IF NOT EXISTS notifications (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind       TEXT NOT NULL,
    title      TEXT,
    body       TEXT,
    payload    TEXT,
    read_at    INTEGER,
    created_at INTEGER NOT NULL DEFAULT (unixepoch())
);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications (user_id, read_at);

CREATE TABLE IF NOT EXISTS user_recent_searches (
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    query       TEXT NOT NULL,
    searched_at INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (user_id, query)
);

-- ── 11. existing queues get a user link ────────────────────────────────────
ALTER TABLE requests ADD COLUMN user_id    INTEGER;
ALTER TABLE requests ADD COLUMN admin_note TEXT;
ALTER TABLE reports  ADD COLUMN user_id    INTEGER;
ALTER TABLE reports  ADD COLUMN admin_note TEXT;

-- ============================================================================
-- VERIFY AFTER RUNNING
--   SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name;
--     -> 16 existing + 15 new = 31 tables
--   Row counts MUST be unchanged:
--     movies 328, series 108, seasons 242, episodes 2732,
--     movie_links 1688, episode_links 608, download_files 1705, users 4
--   Untouched and critical: download_files (file_unique_id unique), bot_sessions,
--     pending_saves, rate_limits, movies.title_lower / series.title_lower indexes
-- ============================================================================