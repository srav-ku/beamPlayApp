package app.cinephile.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google sign-in via Credential Manager, then a Firebase credential exchange so the
 * ID token we hand the worker is one Firebase will vouch for.
 */
actual suspend fun signInWithGoogle(): GoogleUser? {
    val ctx: Context = app.cinephile.ui.AppContextHolder.ctx ?: return null

    val option = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(GOOGLE_WEB_CLIENT_ID)
        .setAutoSelectEnabled(false)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(option)
        .build()

    val result = try {
        CredentialManager.create(ctx).getCredential(context = ctx, request = request)
    } catch (t: Throwable) {
        return null
    }

    val credential = result.credential
    if (credential !is CustomCredential ||
        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        return null
    }

    val googleId = try {
        GoogleIdTokenCredential.createFrom(credential.data)
    } catch (t: Throwable) {
        return null
    }

    // The google-services plugin did not run in this KMP module, so nothing generated the
    // config resources FirebaseApp normally reads. Initialise explicitly instead - same
    // values, no build-plugin dependency, and idempotent across calls.
    if (FirebaseApp.getApps(ctx).isEmpty()) {
        FirebaseApp.initializeApp(
            ctx,
            FirebaseOptions.Builder()
                .setApplicationId("1:683803915198:android:760bdfbb5dc5814bb03fef")
                .setProjectId("beam-20a1a")
                .setGcmSenderId("683803915198")
                .setApiKey("AIzaSyBcXX5gZHNwtIeeUuIFv4l41Zjbh0mHnng")
                .build(),
        )
    }

    val auth = FirebaseAuth.getInstance()
    val firebaseCredential = GoogleAuthProvider.getCredential(googleId.idToken, null)

    val authResult = suspendCancellableCoroutine<com.google.firebase.auth.FirebaseUser?> { cont ->
        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cont.resume(task.result?.user)
                } else {
                    cont.resume(null)
                }
            }
    } ?: return null

    val idToken = try {
        suspendCancellableCoroutine<String?> { cont ->
            authResult.getIdToken(false).addOnCompleteListener { task ->
                cont.resume(if (task.isSuccessful) task.result?.token else null)
            }
        }
    } catch (t: Throwable) {
        null
    } ?: return null

    return GoogleUser(
        idToken = idToken,
        email = authResult.email ?: return null,
        displayName = authResult.displayName,
        firebaseUid = authResult.uid,
    )
}