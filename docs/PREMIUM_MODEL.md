# Premium model - the three pillars

Your plan, and it is the right shape: three benefits, easy to explain, easy to feel.

| Pillar | Free | Premium |
|---|---|---|
| **Stream instantly** | blocked | included |
| **Full-speed downloads** | Telegram redirect (free, normal speed) | direct, full speed |
| **Zero ads** | ads | none |

## The rollout you asked for

Everything stays free today. Two switches in `feature_flags` control the changeover:

- `stream_gate_enabled` = **0** now → everyone streams. Set to **1** when you are ready
- `ads_enabled` = **0** now → nobody sees ads

Because these are rows in the database, you flip the paywall from the admin panel without shipping a
new APK. That matters more for you than for a store app: you cannot force an update.

## Where the gate must live

**Server side, at the moment the stream URL is resolved.** Not in the app.

The app calls your worker to turn a Vidara link into a playable manifest. That call is the gate:
if `stream_gate_enabled = 1` and the user lacks `stream_instant`, the endpoint returns a paywall
payload instead of a stream. A modified APK cannot fake this, because the stream is never produced.

Downloads stay open: resolve the Telegram link for everyone. Premium only changes *speed* - so the
free path keeps the Telegram redirect and the paid path returns a direct link.

Ads live in the app and check `no_ads` + `ads_enabled`. Without AdMob you can run house ads (your own
banners, "request a title", "join the channel") - free, no SDK, no store policy, no tracker.

## The plan screen (data-driven)

The app renders it from `plans` + `plan_features` + `features`, so prices and benefit copy are edited
in the admin panel. Three states to design for, and all three are in the mockup:

1. **Blocked** - a free user taps Play: no dead end, just "Streaming is a premium feature" + the three
   benefits + *See plans*. Never a raw error.
2. **Chooser** - Free / Plus / Pro cards, Plus marked most popular, and because you take payment
   outside the app a clear step: *pay on the support page → come back → enter your reference*.
3. **Active** - a state the user can open any time (Profile → Plan) showing the plan, what is unlocked,
   and when it renews.

## Copy

Use your exact words, they are good:

- **Stream instantly** - Every movie and series, in your browser. Subtitles, audio choices, no waiting.
- **Full-speed downloads** - Direct downloads at the fastest speed your connection can take.
- **Zero ads** - Nothing between you and the film. Anywhere, on any device.

## Rules I would not bend

- Never interrupt playback to sell. The gate is before playback starts, never during.
- A blocked action always explains and offers a way forward - no bare "premium only" toasts.
- `users.is_owner = 1` bypasses every gate so your own account is never metered.
- Everything is reversible: flip a flag off and the app is free again, instantly.