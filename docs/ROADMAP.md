# BeamPlay - Product & Architecture Roadmap

_Last reviewed: 2026-09-17_

## 1. Where we are

The Android app (Kotlin Multiplatform + Compose, Media3) now has a genuinely complete
player: resume per stream, continue watching, history, per-video speed / audio / aspect,
subtitle suite with a style editor, hold-to-boost, brightness / volume gestures, roll-up
buffer, orientation handling. That part is solid.

Everything around the player is still a shallow clone of the website: discovery, library,
accounts and any notion of "my stuff" are either absent or decorative. That is the actual
product gap, not the player.

## 2. The decision

Finish the working thing, or build the real product.

**Decision: build the real product.** A free, no-limits OTT app for cinephiles, phone
first, then desktop / TV, with sync so a viewer can start on the phone and continue on
the laptop without thinking about it.

## 3. What we already own (the collection project)

The Next.js project in `workspace-bc070201...tar` is far more mature than BeamPlay and
most of it is directly reusable:

| Layer | What exists | Reuse for BeamPlay |
|---|---|---|
| Stack | Next.js 16, React 19, Tailwind, shadcn/ui, framer-motion, TanStack Query/Table | Web + desktop client, and reuse the component language |
| Data | Prisma on libSQL (Turso) - free tier friendly, edge reachable | One database for both products |
| Metadata | One normalised `Movie`: `tmdbId` / `imdbId` / `tvmazeId` / `jikanId`, `mediaType` movie\u007Ctv\u007Canime, ratings, genres, runtime | Exactly the metadata layer streaming needs |
| Library | `Collection` + `CollectionItem` (bulk actions, merge, duplicate, import, export, templates), watch-later | Watchlists, "my collections", franchise shelves |
| Tracking | `UserMovieData`: watched, watchedDate, watchCount, personalRating, notes, isFavorite, customTags | History, ratings, notes, favourites |
| Social | follow / followers / feed, public profiles, stats | Sharing, activity, taste-based discovery |
| Ops | `SearchCache`, `ApiRateLimit`, multi-source fallback | Staying inside free API quotas |

## 4. What is genuinely missing

1. **Playback state** - the collection project has no resume concept (`position` there is
   sort order, not a timestamp). Cross-device resume is a new domain.
2. **Episodes as first-class rows** - series need season / episode structure for
   per-episode resume and "next episode".
3. **The player contract on the server** - the phone currently keeps everything locally.

## 5. Cross-device resume - the design

The wrong answer is "log every second". The right answer is: write rarely, from a
well-defined rule, and let the server hold one row per title.

**One row per (user, title, episode)** with `positionMs`, `durationMs`, `updatedAt`,
`deviceId`, `completed`.

**Push rules (client):**
- on pause, on leaving the player, on app background
- while playing, at most one push per 15-20 s *and* only when the position moved > 10 s
- immediately when crossing the completion threshold (>= 99 percent)

**Resolve rules (server, on read):**
- newest `updatedAt` wins, with one guard: if the other device is >= 30 s further ahead,
  prefer the further position (never send a viewer back)
- `completed` removes it from Continue Watching and files it under History
- a position within 5 s of the server value is treated as the same place

**Cost:** a few writes per minute only while something is playing, one row per title. At
that volume the free database tiers are not a constraint even with many users, and the
client keeps working offline because local storage is still the first source.

## 6. How we stay inside free tiers

- One database, one metadata cache, one provider per media type with fallback
  (TMDB primary, IMDb/OMDb for ratings, TVmaze for episodes, Jikan for anime).
- Server-side cache in front of every third-party call.
- No websockets for sync in phase one: fetch on open + push on state change covers the
  real use case. Realtime only if a genuine "hand off to my other device" need appears.
- Never poll third-party metadata from the device; the server owns that.

## 7. Phases

**Phase 0 - foundation (contract + data)**
- `docs/BEAM_API_CONTRACT.md`: endpoints, shapes, error cases, so phone and web implement
  the same thing.
- Server: `PlaybackState`, `Episode`, and read endpoints for Continue Watching / History.
- Accounts: one identity across both projects.

**Phase 1 - phone becomes the real app**
- Sign-in, Continue Watching and History driven by the server when logged in, local-only
  when not (current behaviour stays as the fallback).
- Detail page: seasons / episodes, next-episode entry point.
- Library: watchlist, favourites, collections, personal rating.

**Phase 2 - discovery that earns trust**
- Taste-based rows (from ratings, history, collections) rather than generic trending.
- Search across all providers with a proper empty / loading / error state.
- Franchise and "because you watched" shelves.

**Phase 3 - the cinephile layer**
- Trakt import / export so existing viewing history is not lost, and logging is one tap.
- Notes, ratings, custom tags, stats (hours, streaks, decade spread).
- Sharing: public profile, shareable collections.

**Phase 4 - second screen**
- Desktop / web player using the same server contract and the same scene config, so a
  resume on the laptop picks up the phone's position.
- Keyboard shortcuts, picture-in-picture.

**Phase 5 - scale and polish**
- Downloads / offline, TV layout, accessibility pass, performance budgets.

## 8. How we work

- Contract first. No feature is built twice from guesswork; the server shape is written
  down before either client implements it.
- Every change ships with a build, an install and a stated result - or an explicit note
  that it was not verified.
- One phase at a time, verified before moving on. No half-built features.