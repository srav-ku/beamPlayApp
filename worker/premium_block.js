// ============================================================================
// BeamBot worker - PREMIUM BLOCK (drop-in)
//
// Paste this into your worker file ABOVE the "// ROUTER" section. It only calls
// helpers you already have: json(), errJson(), tursoQueryAll(), tursoQueryOne(),
// getRequestUser(), requireAdmin(). Nothing existing is modified by the paste.
//
// Then make exactly three edits (see the bottom of this file):
//   1. add two entries to ROUTES
//   2. replace the body of handleGetMovieLinks with the gated version
//   3. replace the body of handleGetEpisodeLinks with the gated version
//
// Until you flip the flags in the database, behaviour is identical to today.
// ============================================================================

const PREMIUM_FEATURE_KEYS = ['stream_instant', 'download_full_speed', 'no_ads'];

// ── switches, cached 60s per isolate so this is not a DB read per request ──
let __flagCache = { at: 0, flags: null };

async function getFlags(env) {
    const now = Date.now();
    if (__flagCache.flags && now - __flagCache.at < 60_000) return __flagCache.flags;
    let flags = {};
    try {
        const rows = await tursoQueryAll('SELECT key, enabled FROM feature_flags', [], env);
        for (const r of rows) flags[r.key] = Number(r.enabled) === 1;
    } catch (_) {
        // if the table is unreachable, fail open: never block users by accident
        flags = { stream_gate_enabled: false, ads_enabled: false, pricing_screen: true, enforce_device_limit: false };
    }
    __flagCache = { at: now, flags };
    return flags;
}

function clearFlagCache() { __flagCache = { at: 0, flags: null }; }

// ── what this user has ─────────────────────────────────────────────────────
async function getUserEntitlements(user, env) {
    const flags = await getFlags(env);
    const features = { stream_instant: false, download_full_speed: false, no_ads: false };
    let isOwner = false;
    let plan = null;

    if (user && user.id) {
        const u = await tursoQueryOne('SELECT is_owner FROM users WHERE id = ?', [user.id], env);
        isOwner = !!(u && Number(u.is_owner) === 1);

        if (isOwner) {
            for (const k of PREMIUM_FEATURE_KEYS) features[k] = true;
        } else {
            const rows = await tursoQueryAll(
                `SELECT pf.feature_key AS k, pf.enabled AS e
                   FROM subscriptions s
                   JOIN plans p           ON p.id = s.plan_id
                   JOIN plan_features pf  ON pf.plan_id = p.id
                  WHERE s.user_id = ?
                    AND s.status IN ('active','grace')
                    AND (s.current_period_end IS NULL OR s.current_period_end > unixepoch())`,
                [user.id], env);
            for (const r of rows) features[r.k] = Number(r.e) === 1;
            plan = await currentPlan(user.id, env);
        }
    }

    return { flags, features, isOwner, plan };
}

async function currentPlan(userId, env) {
    const row = await tursoQueryOne(
        `SELECT p.code, p.name, p.tagline, p.price_cents, p.currency, p.billing_period,
                s.status, s.current_period_end, s.provider
           FROM subscriptions s JOIN plans p ON p.id = s.plan_id
          WHERE s.user_id = ? AND s.status IN ('active','grace')
          ORDER BY s.current_period_end DESC LIMIT 1`,
        [userId], env);
    if (!row) {
        const free = await tursoQueryOne("SELECT code, name, tagline, price_cents, currency, billing_period FROM plans WHERE code = 'free'", [], env);
        return free ? { ...free, status: 'free', current_period_end: null } : null;
    }
    return row;
}

// ── the endpoint the app reads on every launch ─────────────────────────────
// Registered as PUBLIC on purpose: a guest must still be able to see the plans
// screen and the benefit copy. If a valid token is present we resolve the user.
async function handleUserEntitlements(request, env) {
    let user = null;
    try { user = await getRequestUser(request, env); } catch (_) { user = null; }

    const ent = await getUserEntitlements(user, env);
    const plans = await tursoQueryAll(
        'SELECT id, code, name, tagline, price_cents, currency, billing_period, is_featured, max_devices, max_streams FROM plans WHERE is_active = 1 ORDER BY sort_order',
        [], env);
    const benefits = await tursoQueryAll(
        'SELECT key, name, description, icon FROM features ORDER BY sort_order', [], env);
    const matrix = await tursoQueryAll(
        'SELECT plan_id, feature_key, enabled FROM plan_features', [], env);

    return json({
        flags: ent.flags,
        plan: ent.plan,
        is_owner: ent.isOwner,
        features: ent.features,
        ads: { show: ent.flags.ads_enabled === true && ent.features.no_ads !== true },
        benefits,
        plans: plans.map(p => ({
            ...p,
            includes: matrix.filter(m => m.plan_id === p.id)
                             .reduce((acc, m) => (acc[m.feature_key] = Number(m.enabled) === 1, acc), {})
        }))
    }, 200, env);
}

// ── the gate: one helper, used by both link handlers ───────────────────────
// Returns null when allowed, or a 402 Response the app renders as the lock.
async function premiumGate(user, featureKey, env) {
    const ent = await getUserEntitlements(user, env);
    if (!ent.flags.stream_gate_enabled) return null;          // gate off: everything free
    if (ent.isOwner || ent.features[featureKey] === true) return null;
    return json({
        error: 'premium_required',
        feature: featureKey,
        benefits: [
            { key: 'stream_instant',      name: 'Stream instantly',      description: 'Every movie and series, in your browser. Subtitles, audio choices, no waiting.' },
            { key: 'download_full_speed', name: 'Full-speed downloads',  description: 'Direct downloads at the fastest speed your connection can take.' },
            { key: 'no_ads',              name: 'Zero ads',              description: 'Nothing between you and the film. Anywhere, on any device.' }
        ]
    }, 402, env);
}

// ── replacements for the two link handlers ────────────────────────────────
// Drop-in swaps: same response shape on success, so nothing else changes.
async function handleGetMovieLinksGated(request, env, params) {
    let user = null;
    try { user = await getRequestUser(request, env); } catch (_) { user = null; }

    const gate = await premiumGate(user, 'stream_instant', env);
    if (gate) return gate;

    const rows = await tursoQueryAll(
        `SELECT ${MOVIE_LINK_COLS} FROM movie_links WHERE movie_id = ? ORDER BY created_at DESC`,
        [params.id], env);
    return json({ items: rows }, 200, env);
}

async function handleGetEpisodeLinksGated(request, env, params) {
    let user = null;
    try { user = await getRequestUser(request, env); } catch (_) { user = null; }

    const gate = await premiumGate(user, 'stream_instant', env);
    if (gate) return gate;

    const rows = await tursoQueryAll(
        `SELECT ${EPISODE_LINK_COLS} FROM episode_links WHERE episode_id = ? ORDER BY created_at DESC`,
        [params.id], env);
    return json({ items: rows }, 200, env);
}

// ── admin: flip the model from the portal instead of SQL ──────────────────
async function handleAdminSetFlag(request, env) {
    const body = await readBody(request);
    const key = String(body.key || '').trim();
    const enabled = body.enabled ? 1 : 0;
    if (!key) return errJson('key is required', 400, env);
    await tursoRun('UPDATE feature_flags SET enabled = ?, updated_at = unixepoch() WHERE key = ?', [enabled, key], env);
    clearFlagCache();
    return json({ ok: true, key, enabled }, 200, env);
}

// ============================================================================
// THE THREE EDITS
//
// 1) In the ROUTES array (near `['GET', '/auth/me', handleMe, 'auth'],`) add:
//
//    ['GET',  '/user/entitlements', handleUserEntitlements, false],
//    ['POST', '/admin/flags',       handleAdminSetFlag,      'admin'],
//
// 2) Replace the whole handleGetMovieLinks function with handleGetMovieLinksGated
//    (keep the name handleGetMovieLinks, or point the ROUTES entry at the new name).
//
// 3) Do the same for handleGetEpisodeLinks / handleGetEpisodeLinksGated.
//
// Leave the Telegram / download_files handlers exactly as they are - downloads
// stay free for everyone. Only "stream_instant" is gated here; "download_full_speed"
// and "no_ads" are read by the app from /user/entitlements.
// ============================================================================