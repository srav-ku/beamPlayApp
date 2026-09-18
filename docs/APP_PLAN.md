# BeamBot - app information architecture and build order

## Navigation: 4 tabs + a search overlay

Bottom bar: **Home · Browse · Library · Profile**. Search is a **top-bar icon opening a full-screen
overlay**, not a tab.

Why not five tabs with Search: a search *field* is a verb, not a destination. Netflix, Prime and Disney
all keep it as an overlay so the fourth slot can go to something people browse. Community is the one
casualty - it moves into Profile as a sub-tab (Discover / Feed / Following / Followers) until it earns
prime space, because social without a populated feed is the emptiest tab in any app.

This is one file (`ui/App.kt` + a nav bar composable). If you disagree after using it, it is a
20-minute change.

## Screens, by build order

| # | Screen | Contents | Phase |
|---|---|---|---|
| 0 | **App shell** | design tokens, bottom nav, screen transitions, account state, entitlements client | 1 |
| 1 | **Detail** (movie / series / episode) | hero, 4 ratings, cast, trailer, **Stream** and **Download**, add-to-list, mark watched, rating, notes, tags, similar, more like this | 1 |
| 2 | **Library** | tabs: Continue watching · Collections · Watchlist · Favourites · History | 1 |
| 3 | **Collection detail** | grid/list toggle, sort, filter, reorder, bulk select, export/import, pin, merge, duplicate | 1 |
| 4 | **Plans** | the three pillars, free vs paid, activation by reference | 1 (inert until you flip) |
| 5 | **Downloads** | Telegram options free; direct download appears when `download_full_speed` | 2 |
| 6 | **Person** | photo, bio, birthday, filmography with roles | 2 |
| 7 | **Browse** | genre / decade / country / studio / franchise / anime / Asian drama, pagination | 2 |
| 8 | **Search** | overlay, recent searches, type pills, advanced filters, results with list/grid | 2 |
| 9 | **Profile** | overview stats, rating distribution, watchlist progress, top actors/directors | 2 |
| 10 | **Year in Review** | year picker, monthly bars, top rated, top genres | 3 |
| 11 | **Community** | discover, feed, following, followers, public profile | 3 |
| 12 | **Templates** | the ~20 curated lists, one tap to create | 3 |
| 13 | **Requests / reports** | submit a title, report a bad link, see status | 3 |

The **player is already done** - it stays as it is and gets two new hooks: it reports position to the
server for cross-device resume, and it reads the entitlement so a locked title never opens.

## Design language

Take the warmth, not the features. Concretely, the token set to sit at the shell level so every screen
inherits it:

- Background `#0B0B0D`, surface `#141418`, raised `#1B1B21`, hairline `#26262E`
- Accent amber `#F5A623` (already the player's progress colour - keep it as the single accent)
- Text `#F2F2F4`, muted `#8F8F9A`, success `#22C55E`
- Radii: cards 20, images 14, chips 999. Spacing steps 4/8/12/16/24/32
- Poster grid: 2 columns phone, 3 on wide phone, never smaller than 100 dp
- Every list has four states designed: loading (skeleton matching the shape), empty (icon + one line +
  a CTA), error (message + retry), and content

Cinephile gets one thing right that most apps miss: **every empty state has an action**. Keep that rule
and the app will feel finished even when it's half built.

## Build order - and why this order

1. **App shell first.** Navigation, tokens, account state, entitlements client. Everything else plugs
   into it, and it is the fastest way to feel whether the app is heading the right way.
2. **Detail screen second.** It is the most visited screen and the only place the paywall exists. Get
   it right and the premium story is real.
3. **Library third.** Watchlist / collections / continue watching / history. This is what makes people
   come back daily.
4. **Browse + Search fourth.** Discovery. High value, low risk, purely server-driven.
5. **Profile + stats fifth.** Year in Review is the retention hook.
6. **Community last.** Needs other users to be worth anything.

## The rules of working, so we do not "do it all at once and mess up"

- One step per turn. Build, install, verify on the device, then move on.
- No step is "done" until it runs on the phone and I have said what I could not check.
- Screens ship with their loading / empty / error states in the same pass - never as a follow-up.
- Nothing is deleted. Old screens stay until their replacement works.
- The database and worker are frozen now; if a screen needs a field that does not exist, I tell you
  before writing code, not after.

## First step, defined

**App shell**, and it is done when:

- the app opens on a 4-tab bottom bar with the token set above
- Home renders real rows from the existing API (continue watching, trending) with skeleton, empty and
  error states
- the entitlements call is made once per launch, cached, and exposed to screens
- the player still opens and plays exactly as it does today
- it is installed over USB and I have reported what I could and could not verify

After that: Detail. After that: Library.