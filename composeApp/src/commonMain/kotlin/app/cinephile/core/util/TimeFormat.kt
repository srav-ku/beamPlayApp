package app.cinephile.core.util

/**
 * Formatting helpers. Implemented without `String.format` because that is
 * JVM-only and this file lives in `commonMain`.
 */

private fun two(value: Long): String = value.toString().padStart(2, '0')

/** `142` -> `"2h 22m"`, `48` -> `"48m"`, `null`/`0` -> `null`. */
fun formatRuntime(minutes: Int?): String? {
    if (minutes == null || minutes <= 0) return null
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

/** `3725` -> `"1:02:05"`, `125` -> `"2:05"`, `0` -> `"0:00"`. */
fun formatSeconds(totalSeconds: Long): String {
    if (totalSeconds <= 0) return "0:00"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "$hours:${two(minutes)}:${two(seconds)}" else "$minutes:${two(seconds)}"
}

/** `42` -> `"42% watched"`. */
fun formatProgress(percent: Int): String = "$percent% watched"
