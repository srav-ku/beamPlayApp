# Worker integration - gating premium in your real code

I read the worker you shared (Cloudflare-Worker style: `fetch` dispatcher, `tursoPipeline` /
`tursoQueryOne` / `tursoRun`, `signJWT` / `verifyJWT`, `getRequestUser` / `requireAuth` /
`requireAdmin`, and per-entity handlers).

## The detail that matters

The worker has **no `stream` / `m3u8` logic** - 0 occurrences. What it exposes is the **link**:

- `handleGetMovieLinks(request, env, params)`
- `handleGetEpisodeLinks(request, env, params)`

The app fetches one of those, gets a Vidara embed URL, and then resolves the `.m3u8` **on the device**
(`VidaraApi.getStreamMetadata` in the Android app). So there is no server-side manifest to gate.

**Therefore the enforcement point is the link endpoint, not a stream resolver.** If the endpoint
refuses to return the link, no stream can exist - on any client, patched or not. That is a better
choke point than what I described earlier, and it is 10 lines of code.

## Three changes, in this order

**1. An entitlements reader** (module-level cache, 60s TTL - one isolate lives for many requests):

```js
let flagCache = { at: 0, flags: null };
async function getFlags(env) {
  if (Date.now() - flagCache.at < 60_000) return flagCache.flags;
  const rows = await tursoQueryAll("SELECT key, enabled FROM feature_flags", [], env);
  const flags = {};
  for (const r of rows) flags[r.key] = Number(r.enabled) === 1;
  flagCache = { at: Date.now(), flags };
  return flags;
}

async function getEntitlements(userId, env) {
  const flags = await getFlags(env);
  if (!userId) return { flags, features: { stream_instant: false, download_full_speed: false, no_ads: false } };

  const user = await tursoQueryOne("SELECT is_owner FROM users WHERE id = ?", [userId], env);
  if (user && Number(user.is_owner) === 1) {
    return { flags, owner: true, features: { stream_instant: true, download_full_speed: true, no_ads: true } };
  }

  const rows = await tursoQueryAll(
    `SELECT pf.feature_key AS k, pf.enabled AS e
       FROM subscriptions s
       JOIN plans p         ON p.id = s.plan_id
       JOIN plan_features pf ON pf.plan_id = p.id
      WHERE s.user_id = ? AND s.status IN ('active','grace')
        AND (s.current_period_end IS NULL OR s.current_period_end > unixepoch())`,
    [userId], env);

  const features = { stream_instant: false, download_full_speed: false, no_ads: false };
  for (const r of rows) features[r.k] = Number(r.e) === 1;
  return { flags, features };
}
```

**2. The gate, inside the two link handlers** - the whole change:

```js
// inside handleGetMovieLinks / handleGetEpisodeLinks, after resolving the user
const ent = await getEntitlements(user && user.id, env);
const streamAllowed = !ent.flags.stream_gate_enabled || ent.features.stream_instant;
if (!streamAllowed) {
  return json({ error: 'premium_required', feature: 'stream_instant' }, 402);
}
// download links keep working for everyone; only the direct/full-speed path is gated
const directAllowed = !ent.flags.stream_gate_enabled || ent.features.download_full_speed;
```

`402` + `premium_required` is what the app renders as the lock you described. Never a 500 and never a
silent empty list - the app must be able to tell "you need premium" apart from "this title has no links".

**3. The endpoint the app reads on launch** `GET /api/me/entitlements` - reuse `handleMe`'s auth:

```js
async function handleEntitlements(request, env) {
  const user = await getRequestUser(request, env);
  const ent = await getEntitlements(user && user.id, env);
  const plans = await tursoQueryAll("SELECT code, name, tagline, price_cents, currency, is_featured FROM plans WHERE is_active = 1 ORDER BY sort_order", [], env);
  const benefits = await tursoQueryAll("SELECT key, name, description FROM features ORDER BY sort_order", [], env);
  const me = user ? await tursoQueryOne("SELECT id, email, role, is_owner FROM users WHERE id = ?", [user.id], env) : null;
  return json({ ...ent, plans, benefits, plan: me ? await currentPlan(me.id, env) : null });
}
```

Optional but nice: an admin route so you flip it from the admin portal instead of SQL -
`POST /api/admin/flags` behind the existing `requireAdmin`, writing to `feature_flags`. Clear the
60s cache after the write.

## Answer to "should I run the flag UPDATEs now?"

**No.** Two reasons, and the second is the important one:

1. Nothing reads `feature_flags` yet - the app has no entitlement code and the worker has no gate. Flipping
   now changes nothing at all.
2. Worse, it is *half* a changeover: if you flip `stream_gate_enabled` before the gate exists, and later
   ship an app that reads entitlements, free users lose streaming while the worker still hands out every
   link. That is the most annoying possible state - locked in the UI, open on the wire.

The real switchover is **two steps together**: worker gate deployed, then the app version that reads
entitlements, then flip the flag. Until then everything stays free, which is your plan anyway.

## What is already correct

- `download_files` and its Telegram flow are untouched and stay free for everyone.
- `users.id` is the key everything joins on, and `subscriptions.user_id` references it directly.
- `is_owner` on your account bypasses every gate.
- Prices, names, benefit copy and the four switches are all rows - admin-editable, no deploy.