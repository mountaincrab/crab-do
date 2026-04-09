package com.mountaincrab.crabdo.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.mountaincrab.crabdo.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: AppDatabase
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUserId: String? get() = auth.currentUser?.uid

    /**
     * Launches the Credential Manager Google Sign-In flow and signs the resulting
     * Google ID token into Firebase Auth. On success, [FirebaseAuth.currentUser] is
     * populated and persisted to disk, so subsequent launches (including offline ones)
     * pick up the same user without another network round-trip.
     *
     * [serverClientId] must be the *web* OAuth 2.0 client ID from the Firebase console
     * (Project Settings → General → Your apps → SDK setup → "Web client (auto created
     * by Google Service)"). Google Sign-In via Credential Manager always uses the web
     * client ID, even on Android — this is counter-intuitive but required.
     */
    suspend fun signInWithGoogle(context: Context, serverClientId: String): Result<FirebaseUser> =
        runCatching {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                // false → show every Google account on the device, not only ones that
                // have already signed into this app. True would silently fail for
                // first-time users.
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(
                    googleCredential.idToken, null
                )
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                authResult.user ?: error("Firebase returned null user after sign-in")
            } else {
                error("Unexpected credential type: ${credential::class.java.name}")
            }
        }

    suspend fun signOut(context: Context) {
        auth.signOut()
        // Clear local data so the next user doesn't see stale rows from this account.
        withContext(Dispatchers.IO) { database.clearAllTables() }
        // Also clear Credential Manager state so the user isn't silently re-signed in
        // on the next getCredential() call.
        runCatching {
            CredentialManager.create(context)
                .clearCredentialState(ClearCredentialStateRequest())
        }
    }

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}
