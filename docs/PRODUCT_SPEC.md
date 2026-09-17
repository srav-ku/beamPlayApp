# BeamBot - product spec (what the app should be)

Cinephile is the **quality bar**, not a merge target. It shows the level of feature depth and
clarity a viewer now expects; BeamBot is the streaming product that has to meet it, and beat it
where streaming makes that possible.

## 0. The bar we hold ourselves to

- Playback starts instantly and never dead-ends: every failure has a readable message and a retry.
- Nothing is a decorative button. If it is on screen, it does something.
- Works offline-first for state, online for discovery.
- Every list has real empty / loading / error states, not blank space.
- Runs properly on a mid-range Android phone, not just the developer's device.

## 1. Accounts and identity (exists, extend)

Google + guest login (keep as-is - we are comfortable here). Add: display name, avatar,
guest -> account upgrade without losing local state, device list, last seen.

## 2. Playback (already strong - protect it)

Per-stream resume, continue watching, history, 10 speeds with pitch preserved, hold-to-boost,
per-video audio and aspect memory, full subtitle suite, brightness / volume gestures,
rolling buffer, immersive landscape. This is the differentiator; keep it stable.

## 3. Cross-device sync (the reason to sign in)

Start on the phone, continue on the laptop at the same second.
- One row per (user, title, episode): position, duration, updated_at, device, completed
- Push on pause / leave / background, plus at most once per 15-20 s while playing
- Newest wins, with a furthest-ahead guard so a viewer is never sent backwards
- Sync watchlist, favourites, settings, and last-used audio / subtitle language

## 4. Library and collections

Watchlist, favourites, custom lists, list covers, reorder, public/private,
**templates** (the curated presets: Oscar Winners, Hidden Gems, Mind-Bending,
Cult Classics, ...), import/export, notes and tags per title.

## 5. History and tracking

Full history with date and watch count, mark watched, rewatch counting,
personal rating 1-10, favourites, per-episode watched state for series.

## 6. Discovery - where a cinephile app is won

Trending / Popular / Top Rated / Latest / Upcoming / **Hidden Gems** / Best of Year,
filters by **genre, decade, country, studio, franchise**, anime and Asian drama shelves,
"Because you watched", **Pick for Me** (pick one from the watchlist), recommendations from
your own ratings. Server-cached so third-party quotas are never the bottleneck.

## 7. Search

Global search across movies, series and people, recent searches, suggestions, instant results,
cached server-side with graceful fallback when a provider is down.

## 8. Stats and Year in Review

Hours watched, titles completed, streak, favourite genre, decade spread, longest session.
Cheap to compute, very high retention value.

## 9. Integrations

**Trakt** import / export (`users.trakt_access_token` already exists). Import existing history so
nobody starts from zero; export so nobody is locked in. Anime list sync later if it earns its keep.

## 10. Community (design the data now, ship later)

Follows, activity feed, public lists, shared collections. Deliberately after library + history:
social without content is empty, and moderation has a real cost. Schema supports it from day one.

## 11. Admin and ops

Content ops with link management, requests queue, reports queue with status, worker health,
feature flags, and a way to fix a bad stream link without a redeploy.

## 12. Anti-goals

No self-hosted video, no DRM, no scraping pipeline, no ads, no DMs, no gamified noise.
Streaming links stay exactly what the current backend provides.

## 13. Order of work

1. Schema v2 on the branch, verified, then main
2. Accounts polished + settings synced
3. Library, history, ratings (the "my stuff" layer)
4. Discovery + filters + Pick for Me + templates
5. Cross-device resume
6. Stats / Year in Review
7. Trakt
8. Community