package app.cinephile.ui

/**
 * Genres the user picked right after signing up.
 *
 * Kept in local storage for now (like the playback memory), so the picker works
 * before any server-side profile exists. Swapping this for the DB later is a
 * one-file change because the rest of the app only calls these two functions.
 */
expect fun saveInterests(genres: List<String>)

expect fun loadInterests(): List<String>