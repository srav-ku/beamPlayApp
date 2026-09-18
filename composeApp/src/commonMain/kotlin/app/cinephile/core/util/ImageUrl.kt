package app.cinephile.core.util

import app.cinephile.core.network.TMDB_IMAGE_BASE

/** Common TMDB poster/backdrop sizes. */
object TmdbSize {
    const val POSTER_SMALL = "w185"
    const val POSTER = "w342"
    const val POSTER_LARGE = "w500"
    const val BACKDROP = "w780"
    const val ORIGINAL = "original"
}

/**
 * Builds a TMDB CDN URL. Returns `null` for blank paths so Compose/Coil can
 * fall back to a placeholder without firing a broken request.
 */
fun tmdbImageUrl(path: String?, size: String = TmdbSize.POSTER_LARGE): String? =
    if (path.isNullOrBlank()) null else "$TMDB_IMAGE_BASE/$size$path"

/**
 * Picks the smallest CDN bucket that is still >= [desiredWidthPx], so a 132dp
 * poster does not download a w780 file.
 */
fun tmdbImageUrlSized(path: String?, desiredWidthPx: Int): String? {
    if (path.isNullOrBlank()) return null
    val buckets = listOf(
        "w92" to 92,
        "w154" to 154,
        "w185" to 185,
        "w342" to 342,
        "w500" to 500,
        "w780" to 780,
        "original" to Int.MAX_VALUE,
    )
    val picked = buckets.firstOrNull { it.second >= desiredWidthPx } ?: buckets.last()
    return "$TMDB_IMAGE_BASE/${picked.first}$path"
}
