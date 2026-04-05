package com.mountaincrab.crabdo.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    // Fallback ID used when Firebase is unreachable (local testing without emulator).
    // All data saved under this ID stays local — nothing syncs to Firestore.
    private var offlineUserId: String? = null

    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUserId: String? get() = auth.currentUser?.uid ?: offlineUserId

    suspend fun ensureAuthenticated(): String {
        val user = auth.currentUser
        if (user != null) return user.uid
        return try {
            val result = withTimeout(5_000) { auth.signInAnonymously().await() }
            result.user!!.uid
        } catch (e: Exception) {
            // Firebase unreachable (no emulator, no real google-services.json).
            // Fall back to a stable local ID so the app is usable offline.
            val fallback = offlineUserId ?: "local-user-offline"
            offlineUserId = fallback
            fallback
        }
    }

    suspend fun linkWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.currentUser!!.linkWithCredential(credential).await()
    }

    fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous ?: true

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}
