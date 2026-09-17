-- ============================================================================
-- BeamBot - premium / plans / device limits (additive, run once)
-- Nothing here is enforced until the app implements it. The tables exist so the
-- decision is data, not code, when you turn it on.
-- ============================================================================

-- ── plan catalog ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS plans (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    code           TEXT NOT NULL UNIQUE,      -- 'free' | 'plus' | 'pro'
    name           TEXT NOT NULL,
    price_cents    INTEGER NOT NULL DEFAULT 0,
    currency       TEXT NOT NULL DEFAULT 'INR',
    billing_period TEXT NOT NULL DEFAULT 'month',  -- month | year | lifetime
    max_devices    INTEGER NOT NULL DEFAULT 2,     -- simultaneous signed-in devices
    max_streams    INTEGER NOT NULL DEFAULT 1,     -- concurrent playback
    is_active      INTEGER NOT NULL DEFAULT 1,
    sort_order     INTEGER NOT NULL DEFAULT 0
);

-- what each plan unlocks: one row per feature key per plan
CREATE TABLE IF NOT EXISTS plan_features (
    plan_id     INTEGER NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    feature_key TEXT NOT NULL,          -- 'offline_download' | 'ai_recommendations' | ...
    value_int   INTEGER,                -- quota, -1 = unlimited
    value_bool  INTEGER,                -- on/off
    PRIMARY KEY (plan_id, feature_key)
);

-- ── the feature dictionary (so the UI can render "what you get") ──────────
CREATE TABLE IF NOT EXISTS features (
    key         TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT,
    kind        TEXT NOT NULL,          -- 'bool' | 'quota'
    sort_order  INTEGER NOT NULL DEFAULT 0
);

-- ── subscriptions ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS subscriptions (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id            INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_id            INTEGER NOT NULL REFERENCES plans(id),
    status             TEXT NOT NULL,   -- active | grace | expired | canceled | refunded
    provider           TEXT NOT NULL,   -- google_play | stripe | razorpay | manual
    provider_ref       TEXT,            -- purchase token / subscription id
    current_period_end INTEGER,
    started_at         INTEGER NOT NULL DEFAULT (unixepoch()),
    canceled_at        INTEGER,
    created_at         INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at         INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (provider, provider_ref)
);
CREATE INDEX IF NOT EXISTS idx_subs_user ON subscriptions (user_id, status);

-- every webhook/receipt we processed, for idempotency and audit
CREATE TABLE IF NOT EXISTS payment_events (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    provider    TEXT NOT NULL,
    event_id    TEXT NOT NULL,
    user_id     INTEGER,
    kind        TEXT,
    payload     TEXT NOT NULL,
    received_at INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (provider, event_id)
);

-- per-user grants: comps, gifts, limited-time unlocks, and overrides
CREATE TABLE IF NOT EXISTS user_entitlements (
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature_key TEXT NOT NULL,
    value_int   INTEGER,
    value_bool  INTEGER,
    source      TEXT,                  -- plan | override | trial | gift
    expires_at  INTEGER,
    updated_at  INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (user_id, feature_key)
);

-- ── device limits and concurrent streams ──────────────────────────────────
CREATE TABLE IF NOT EXISTS device_sessions (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id    TEXT NOT NULL,
    platform     TEXT,                 -- android | web | tv
    name         TEXT,
    app_version  TEXT,
    last_seen_at INTEGER NOT NULL DEFAULT (unixepoch()),
    revoked_at   INTEGER,
    created_at   INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (user_id, device_id)
);
CREATE INDEX IF NOT EXISTS idx_device_active ON device_sessions (user_id, revoked_at, last_seen_at);

-- ── quotas (AI recommendations, downloads per month, searches, ...) ───────
CREATE TABLE IF NOT EXISTS usage_counters (
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature_key TEXT NOT NULL,
    period_key  TEXT NOT NULL,          -- '2026-09' | '2026-09-17' | 'lifetime'
    used        INTEGER NOT NULL DEFAULT 0,
    updated_at  INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (user_id, feature_key, period_key)
);

-- ── rollout switches ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS feature_flags (
    key        TEXT PRIMARY KEY,
    enabled    INTEGER NOT NULL DEFAULT 0,
    rollout_pc INTEGER NOT NULL DEFAULT 0,   -- % of users
    note       TEXT,
    updated_at INTEGER NOT NULL DEFAULT (unixepoch())
);

-- ── seed: the three plans, free usable forever ────────────────────────────
INSERT OR IGNORE INTO plans (code, name, price_cents, currency, billing_period, max_devices, max_streams, sort_order)
VALUES ('free', 'Free',  0,     'INR', 'month', 2, 1, 0),
       ('plus', 'Plus',  14900, 'INR', 'month', 4, 2, 1),
       ('pro',  'Pro',   29900, 'INR', 'month', 6, 3, 2);

INSERT OR IGNORE INTO features (key, name, description, kind, sort_order) VALUES
 ('offline_download',    'Offline downloads',   'Keep titles on the device for no-signal viewing', 'quota', 1),
 ('ai_recommendations',  'AI recommendations',  'Natural-language suggestions, per day',           'quota', 2),
 ('pinned_collections',  'Pinned collections',  'Pin lists to the top of Home',                    'quota', 3),
 ('advanced_filters',    'Advanced discovery',  'Studio, franchise, decade and country filters',   'bool',  4),
 ('year_in_review',      'Year in Review',      'Annual stats and rating breakdown',               'bool',  5),
 ('trakt_sync',          'Trakt sync',          'Import and export your history',                  'bool',  6),
 ('priority_requests',   'Priority requests',   'Your title requests jump the queue',              'bool',  7),
 ('no_ads',              'No ads',              'Ad-free playback',                                'bool',  8);

INSERT OR IGNORE INTO plan_features (plan_id, feature_key, value_int, value_bool)
SELECT p.id, f.key,
       CASE f.key WHEN 'ai_recommendations' THEN 3 WHEN 'pinned_collections' THEN 1 WHEN 'offline_download' THEN 0 ELSE 0 END,
       CASE f.key WHEN 'advanced_filters' THEN 0 WHEN 'year_in_review' THEN 1 WHEN 'no_ads' THEN 1 ELSE 0 END
FROM plans p, features f WHERE p.code = 'free';

INSERT OR IGNORE INTO plan_features (plan_id, feature_key, value_int, value_bool)
SELECT p.id, f.key,
       CASE f.key WHEN 'ai_recommendations' THEN 30 WHEN 'pinned_collections' THEN 3 WHEN 'offline_download' THEN 20 ELSE -1 END,
       1
FROM plans p, features f WHERE p.code IN ('plus','pro');

-- ============================================================================
-- Verify:  SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';
--   -> 35 + 8 = 43
--   SELECT code, max_devices, max_streams FROM plans;
--   SELECT COUNT(*) FROM plan_features;   -> 24
-- ============================================================================