# Phase 1 — Networking Layer (done, verified)

Scope: **data plumbing only.** No UI was touched. Phases 2–3 are where the app
starts to *look* like the website.

## What was added (all new files)

```
composeApp/src/commonMain/kotlin/app/cinephile/core/
  network/HttpClientFactory.kt          Ktor client factory (expect), JSON config, BeamTokenStore, auth plugin
  network/BeamApi.kt                    every beamplay.beam-api.workers.dev endpoint
  network/TmdbApi.kt                    direct TMDB v3 calls (Home screen data)
  network/VidaraApi.kt                  embed URL -> IP-locked .m3u8 extractor
  network/ServiceContainer.kt           poor-man's DI holder
  network/Services.kt                   `services` global + initServices()
  model/Models.kt                       worker models + response wrappers
  model/TmdbModels.kt                   TMDB models
  util/ImageUrl.kt                      TMDB CDN URL builder

composeApp/src/androidMain/kotlin/app/cinephile/core/network/
  HttpClientFactory.android.kt          OkHttp engine + cookie jar + timeouts + logging

composeApp/src/androidUnitTest/kotlin/app/cinephile/core/
  network/HttpClientFactoryTest.kt      auth header + live worker health
  network/BeamApiTest.kt                live worker endpoints
  network/TmdbApiTest.kt                live TMDB endpoints
  network/VidaraApiTest.kt              real embed -> real .m3u8
  model/ModelSerializationTest.kt       regression guards for real JSON shapes
```

## What was changed (existing files)

| File | Change |
| --- | --- |
| `composeApp/build.gradle.kts` | added `ktor-client-logging`, `androidUnitTest` deps (`kotlin-test`, `coroutines-test`, `ktor-client-mock`), `buildFeatures { buildConfig = true }` |
| `gradle/libs.versions.toml` | added those 3 library entries |
| `MainActivity.kt` | calls `initServices(BuildConfig.DEBUG)` before `setContent {}` |
| `data/Api.kt` (legacy) | **bug fix:** `TMDB_KEY` was a truncated placeholder (`"5701ac…c9aa"`), so the old home screen fetched nothing. Restored the full key. |

Nothing else in `data/` or `ui/` was modified.

## Verify it yourself

```powershell
cd C:\Users\srava\Desktop\beamPlayApp
.\gradlew.bat :composeApp:testDebugUnitTest    # 23 tests, all green
.\gradlew.bat :composeApp:assembleDebug        # -> composeApp\build\outputs\apk\debug\composeApp-debug.apk
```

Install on a USB-connected phone (USB debugging on):

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
.\gradlew.bat :composeApp:installDebug
```

## Notes / known deviations

- The playbook's `ReportBody`, `RequestBody` and `LibraryItem` shapes do **not**
  match the deployed worker. The code follows the deployed `beam-worker.js`
  (authoritative); see the review notes for details.
- `PlaybackItem.is_finished` must be `Int` — the worker returns SQLite `0`/`1`.
- The Beam JWT is only sent to `*.beam-api.workers.dev` (never to Vidara).
- `vidaraa.cc` (current live host) and `vidara.to` (legacy) both work; the API
  origin is derived from the embed URL.
