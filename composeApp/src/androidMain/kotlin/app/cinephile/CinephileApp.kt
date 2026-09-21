package app.cinephile

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import okio.Path.Companion.toPath

/**
 * Application entry point.
 *
 * Exists for one reason: image caching must not be able to take the app down.
 *
 * Coil's default disk cache is unbounded and lives in a shared location. It once
 * grew into a state where its initialisation blocked for a long time, and every
 * other HTTP request in the app queued behind it - Home and Browse looked like
 * they were loading forever while the network itself was healthy. So the cache
 * is now capped and explicitly owned:
 *
 *   - 150MB ceiling, so a runaway cache cannot fill the device,
 *   - its own directory name, so it can be cleared in isolation,
 *   - image loading picks it up through the singleton loader below.
 */
class CinephileApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        // Paint from the last session immediately; anything past its TTL is ignored.
        app.cinephile.data.TtlCache.hydrate()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(150L * 1024L * 1024L)
                    .build()
            }
            .build()
}