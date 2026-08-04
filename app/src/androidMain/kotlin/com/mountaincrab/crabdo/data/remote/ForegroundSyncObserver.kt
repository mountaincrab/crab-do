package com.mountaincrab.crabdo.data.remote

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

/**
 * Kicks off a sync whenever the app comes to the foreground.
 *
 * Sync was otherwise only enqueued at login, on pull-to-refresh, or after a local
 * mutation — so returning to an app that was merely backgrounded (home press) never
 * pulled remote changes. Observing the *process* lifecycle rather than an activity's
 * gives exactly one trigger per foreground entry, whether or not the process was
 * killed in between.
 *
 * This is the backstop for what the Firestore listeners cannot cover: the window
 * before a listener attaches, and changes that landed while the process was dead.
 */
class ForegroundSyncObserver(
    private val auth: FirebaseAuth,
    private val workManager: WorkManager
) : DefaultLifecycleObserver {

    private var lastSyncAt = 0L

    override fun onStart(owner: LifecycleOwner) {
        if (auth.currentUser == null) return
        // Flicking away and back shouldn't re-sync on every return.
        val now = System.currentTimeMillis()
        if (now - lastSyncAt < MIN_INTERVAL_MS) return
        lastSyncAt = now

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // KEEP, not REPLACE: a foreground return must never cancel an in-flight sync
        // that is midway through pushing local pending writes.
        workManager.enqueueUniqueWork("sync", ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val MIN_INTERVAL_MS = 30_000L
    }
}
