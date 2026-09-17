# The entitlement contract - how the app knows what is free, without an update

Your hard requirement: switch the premium model on later **without users updating the APK**.
That only works if the app ships with **zero** premium logic and reads everything at runtime.

## One endpoint, read on every launch

`GET /api/me/entitlements` (authenticated) returns everything the app needs:

```json
{
  "flags": {
    "stream_gate_enabled": false,
    "ads_enabled": false,
    "pricing_screen": true,
    "enforce_device_limit": false
  },
  "plan": { "code": "free", "name": "Free", "price_cents": 0, "currency": "INR",
            "period": "month", "renews_at": null },
  "features": {
    "stream_instant":      { "enabled": false, "reason": "premium" },
    "download_full_speed": { "enabled": false, "reason": "premium" },
    "no_ads":              { "enabled": false, "reason": "premium" }
  },
  "ads": { "show": false },
  "benefits": [
    { "key": "stream_instant", "name": "Stream instantly",
      "description": "Every movie and series, in your browser. Subtitles, audio choices, no waiting." },
    { "key": "download_full_speed", "name": "Full-speed downloads",
      "description": "Direct downloads at the fastest speed your connection can take." },
    { "key": "no_ads", "name": "Zero ads",
      "description": "Nothing between you and the film. Anywhere, on any device." }
  ],
  "plans": [ { "code": "free", "name": "Free", "price_cents": 0, "is_featured": false,
               "includes": ["stream_instant": false, "download_full_speed": false, "no_ads": false] } ]
}
```

Cache it (in memory + on disk) with a short TTL, so a cold start never blocks on the network and the
app works offline on the last known state.

## What the app does with it - no branching on hardcoded rules

| UI element | Rule |
|---|---|
| Detail screen **Stream** button | `flags.stream_gate_enabled == false` -> normal play. Otherwise gate on `features.stream_instant.enabled`. Locked state still tappable: shows the benefits and *See plans*. Never a dead button, never a raw error. |
| Detail screen **Download** button | Always present. Telegram path is free for everyone. Direct in-app download appears only when `features.download_full_speed.enabled`. |
| Ad slot | Renders only when `ads.show == true`. House ads, no SDK. |
| Plans screen | Built entirely from `plans` + `benefits` + `includes` - prices, names and copy are all server data. |
| Prices / copy change | You edit rows, users see it on next launch. **No APK.** |

## Where enforcement actually happens

Two layers, and the second is the one that matters:

1. **App** - renders locks and the paywall from the payload above. This is UX only.
2. **Worker / server** - the endpoint that resolves a Vidara link into a playable manifest refuses to
   produce a stream when `stream_gate_enabled` is on and the user lacks the entitlement. Same for the
   direct-download link.

Because layer 2 never produces the thing being sold, a patched APK cannot unlock streaming or
full-speed downloads. That is the whole reason to keep the flags server-side.

## Flipping it on - three rows, no deployment

```sql
UPDATE feature_flags SET enabled = 1 WHERE key = 'stream_gate_enabled';  -- start charging for stream
UPDATE feature_flags SET enabled = 1 WHERE key = 'ads_enabled';         -- start showing ads
UPDATE plans SET price_cents = 9900, name = 'Plus' WHERE code = 'plus'; -- change a price
```

Reverse it by setting them back to 0. Nothing in the APK changes, ever.

## Your own account

`UPDATE users SET is_owner = 1 WHERE email = '<your email>';`
The entitlement resolver returns everything unlocked for `is_owner`, so you are never metered and never
see ads.