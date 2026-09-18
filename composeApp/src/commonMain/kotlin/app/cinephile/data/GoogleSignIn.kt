package app.cinephile.data

/** What a Google sign-in gives us: a Firebase ID token plus the claims the UI needs. */
data class GoogleUser(
    val idToken: String,
    val email: String,
    val displayName: String?,
    val firebaseUid: String,
)

/**
 * Web client ID from the Firebase project. Android needs this to return an ID token
 * rather than just an access token.
 */
const val GOOGLE_WEB_CLIENT_ID = "683803915198-lirga4492g4q4a159e4cv8brir3oab3e.apps.googleusercontent.com"

/**
 * Real Google sign-in. Returns null when the user dismisses the sheet or the flow fails -
 * callers must treat null as "nothing happened", never as "signed in".
 */
expect suspend fun signInWithGoogle(): GoogleUser?