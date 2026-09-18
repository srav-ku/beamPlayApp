# Phase 2 — Design System & Theme (done, verified)

Goal: the app must render with the website's real fonts, palette and shapes.
No screen layouts changed in this phase — Phases 3+ build screens on top of
these primitives.

## New files

```
composeApp/src/commonMain/composeResources/font/
  geist_mono_regular.ttf  geist_mono_medium.ttf  geist_mono_semibold.ttf
  inter_semibold.ttf      inter_bold.ttf         inter_extrabold.ttf

composeApp/src/commonMain/kotlin/app/cinephile/core/ui/theme/
  Colors.kt    exact html.dark palette + Material3 darkColorScheme
  Shapes.kt    8 / 12 / 16 / 20 / 26 dp + PillShape
  Type.kt      GeistMono + Inter FontFamily + BeamTypography
  Theme.kt     BeamTheme(), LocalBeamColors, `Beam.colors`

composeApp/src/commonMain/kotlin/app/cinephile/core/ui/components/
  CategoryPill.kt      rounded filter chip (LinkSelector.tsx)
  SectionHeader.kt     title + Movie/TV tabs + scroll chevrons (Home.tsx)
  LoadingSkeleton.kt   shimmer + PosterSkeleton + LineSkeleton

composeApp/src/commonMain/kotlin/app/cinephile/core/util/
  TimeFormat.kt        formatRuntime / formatSeconds / formatProgress
  ImageUrl.kt          now also has tmdbImageUrlSized()
```

## Changed files

| File | Change |
| --- | --- |
| `ui/Theme.kt` (legacy) | `BeamColors` now points at the exact Phase 2 tokens, and `BeamTheme` applies `BeamColorScheme` + `BeamTypography` + `BeamShapes`. This is what makes the existing screens pick up Geist Mono/Inter and the true palette. |

## Font provenance

- **Geist Mono** — static TTFs from `vercel/geist-font` (`fonts/GeistMono/ttf`).
- **Inter** — static TTFs from the official Inter 4.1 release (`extras/ttf`).
- Both shipped as real static instances rather than variable fonts so every
  weight renders exactly, including on older Android versions.

## Verify

```powershell
cd C:\Users\srava\Desktop\beamPlayApp
.\gradlew.bat :composeApp:testDebugUnitTest   # 23 tests, all green
.\gradlew.bat :composeApp:assembleDebug
.\gradlew.bat :composeApp:installDebug
```

Checks already run:
- Every `Color(0xFF...)` in `Colors.kt` matches `globals.css` → 11/11 core
  tokens plus the three amber accents; `--card-radius: 26px` and
  `--pill-radius: 9999px` match too.
- All six `.ttf` files are present in the built APK under
  `assets/composeResources/.../font/`.
- `BUILD SUCCESSFUL`; 23/23 unit tests pass.

## Still to come

Phase 3 replaces the Home screen (hero slider, trending rank row, content rows,
bottom nav) and is the first phase where the app visibly matches the site.
