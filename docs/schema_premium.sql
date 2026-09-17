-- ============================================================================
-- BeamBot - premium: Stream instantly / Full-speed downloads / Zero ads
-- Additive. Run once in the Turso console. Nothing is enforced until you flip
-- feature_flags.stream_gate_enabled to 1, so for now the app stays free.
-- ============================================================================

CREATE TABLE IF NOT EXISTS plans (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    code           TEXT NOT NULL UNIQUE,          -- 'free' | 'plus' | 'pro'
    name           TEXT NOT NULL,
    tagline        TEXT,
    price_cents    INTEGER NOT NULL DEFAULT 0,
    currency       TEXT NOT NULL DEFAULT 'INR',
    billing_period TEXT NOT NULL DEFAULT 'month',
    is_featured    INTEGER NOT NULL DEFAULT 0,
    max_devices    INTEGER NOT NULL DEFAULT 3,
    max_streams    INTEGER NOT NULL DEFAULT 1,
    is_active      INTEGER NOT NULL DEFAULT 1,
    sort_order     INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS features (
    key         TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT,
    icon        TEXT,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS plan_features (
    plan_id     INTEGER NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    feature_key TEXT NOT NULL REFERENCES features(key) ON DELETE CASCADE,
    enabled     INTEGER NOT NULL DEFAULT 0,
    value_int   INTEGER,                          -- quota when relevant, -1 unlimited
    PRIMARY KEY (plan_id, feature_key)
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id            INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_id            INTEGER NOT NULL REFERENCES plans(id),
    status             TEXT NOT NULL,             -- active | grace | expired | canceled | refunded
    provider           TEXT NOT NULL,             -- kofi | bmc | patreon | upi | manual
    provider_ref       TEXT,
    license_key        TEXT,
    current_period_end INTEGER,
    started_at         INTEGER NOT NULL DEFAULT (unixepoch()),
    issued_at          INTEGER,
    canceled_at        INTEGER,
    created_at         INTEGER NOT NULL DEFAULT (unixepoch()),
    updated_at         INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (provider, provider_ref)
);
CREATE INDEX IF NOT EXISTS idx_subs_user ON subscriptions (user_id, status);

CREATE TABLE IF NOT EXISTS user_entitlements (
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature_key TEXT NOT NULL,
    enabled     INTEGER NOT NULL DEFAULT 0,
    value_int   INTEGER,
    source      TEXT,                             -- plan | override | gift | trial
    expires_at  INTEGER,
    updated_at  INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (user_id, feature_key)
);

CREATE TABLE IF NOT EXISTS payment_events (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    provider    TEXT NOT NULL,
    event_id    TEXT NOT NULL,
    user_id     INTEGER,
    kind        TEXT,
    amount_cents INTEGER,
    currency    TEXT,
    payload     TEXT NOT NULL,
    received_at INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (provider, event_id)
);

CREATE TABLE IF NOT EXISTS activation_claims (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER REFERENCES users(id) ON DELETE CASCADE,
    platform     TEXT NOT NULL,
    reference    TEXT NOT NULL,
    amount_cents INTEGER,
    currency     TEXT,
    days_granted INTEGER NOT NULL DEFAULT 30,
    status       TEXT NOT NULL DEFAULT 'pending', -- pending | approved | rejected
    auto_matched INTEGER NOT NULL DEFAULT 0,
    reviewed_by  INTEGER,
    reviewed_at  INTEGER,
    note         TEXT,
    created_at   INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (platform, reference)
);
CREATE INDEX IF NOT EXISTS idx_claims_status ON activation_claims (status, created_at DESC);

CREATE TABLE IF NOT EXISTS device_sessions (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id    TEXT NOT NULL,
    platform     TEXT,
    name         TEXT,
    app_version  TEXT,
    last_seen_at INTEGER NOT NULL DEFAULT (unixepoch()),
    revoked_at   INTEGER,
    created_at   INTEGER NOT NULL DEFAULT (unixepoch()),
    UNIQUE (user_id, device_id)
);
CREATE INDEX IF NOT EXISTS idx_device_active ON device_sessions (user_id, revoked_at, last_seen_at);

CREATE TABLE IF NOT EXISTS usage_counters (
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature_key TEXT NOT NULL,
    period_key  TEXT NOT NULL,                    -- '2026-W38' | '2026-09-17' | 'lifetime'
    used        INTEGER NOT NULL DEFAULT 0,
    updated_at  INTEGER NOT NULL DEFAULT (unixepoch()),
    PRIMARY KEY (user_id, feature_key, period_key)
);

CREATE TABLE IF NOT EXISTS feature_flags (
    key        TEXT PRIMARY KEY,
    enabled    INTEGER NOT NULL DEFAULT 0,
    rollout_pc INTEGER NOT NULL DEFAULT 0,
    note       TEXT,
    updated_at INTEGER NOT NULL DEFAULT (unixepoch())
);

ALTER TABLE users ADD COLUMN is_owner INTEGER NOT NULL DEFAULT 0;

-- ── the three pillars ─────────────────────────────────────────────────────
INSERT OR IGNORE INTO features (key, name, description, icon, sort_order) VALUES
 ('stream_instant', 'Stream instantly',
  'Every movie and series, in your browser. Subtitles, audio choices, no waiting.', 'play', 1),
 ('download_full_speed', 'Full-speed downloads',
  'Direct downloads at the fastest speed your connection can take.', 'download', 2),
 ('no_ads', 'Zero ads',
  'Nothing between you and the film. Anywhere, on any device.', 'shield', 3);

INSERT OR IGNORE INTO plans (code, name, tagline, price_cents, currency, billing_period, is_featured, max_devices, max_streams, sort_order)
VALUES ('free', 'Free',  'Browse everything, download via Telegram', 0,     'INR', 'month', 0, 3, 0, 0),
       ('plus', 'Plus',  'Stream everything, ad-free',               14900, 'INR', 'month', 1, 4, 1, 1),
       ('pro',  'Pro',   'Stream everything, fastest downloads',     29900, 'INR', 'month', 0, 6, 2, 2);

-- free: Telegram downloads only, no streaming, ads on
INSERT OR IGNORE INTO plan_features (plan_id, feature_key, enabled, value_int)
SELECT p.id, f.key,
       CASE f.key WHEN 'stream_instant' THEN 0 WHEN 'download_full_speed' THEN 0 WHEN 'no_ads' THEN 0 ELSE 1 END,
       -1
FROM plans p, features f WHERE p.code = 'free';

-- paid: all three on
INSERT OR IGNORE INTO plan_features (plan_id, feature_key, enabled, value_int)
SELECT p.id, f.key, 1, -1 FROM plans p, features f WHERE p.code IN ('plus','pro');

-- ── rollout switches: everything stays free until you turn the gate on ────
INSERT OR IGNORE INTO feature_flags (key, enabled, rollout_pc, note) VALUES
 ('stream_gate_enabled', 0, 0, 'OFF = everyone streams free. ON = free plan loses streaming.'),
 ('ads_enabled',         0, 0, 'OFF = nobody sees ads. ON = free plan sees ads.'),
 ('pricing_screen',      1, 100, 'Show the plan chooser in the app'),
 ('enforce_device_limit',0, 0, 'OFF = ignore max_devices');

-- ============================================================================
-- Verify:  SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';
--   -> 35 + 11 = 46
--   SELECT code, name, price_cents, is_featured FROM plans;   -> 3 rows
--   SELECT key, enabled FROM feature_flags;                   -> all gates OFF except pricing_screen
--   SELECT COUNT(*) FROM plan_features;                       -> 9
-- ============================================================================