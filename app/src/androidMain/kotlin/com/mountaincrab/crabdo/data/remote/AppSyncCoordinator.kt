package com.mountaincrab.crabdo.data.remote

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.mountaincrab.crabdo.data.repository.BoardRepository
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Owns the lifecycle of the real-time Firestore listeners at **application scope**,
 * driven by whether the app is in the foreground and who is signed in.
 *
 * Why app-scoped: previously the reminders listener was started in
 * `BoardListViewModel.init`, so it only ran when the Boards tab's ViewModel happened
 * to be alive — landing on the Reminders tab first meant no listener at all, and board
 * data had no listener whatsoever. This coordinator fixes both by starting the reminder
 * **and** board listeners whenever the app is foregrounded and a user is signed in.
 *
 * On entering the foreground it also kicks a one-shot [BoardRepository.triggerSync] and
 * reschedules alarms, so remote changes made while the app was closed are pulled in
 * immediately on open (not only after a manual pull-to-refresh).
 *
 * Listeners are stopped when the app is backgrounded to avoid background Firestore cost;
 * true delivery-while-closed would require FCM/WorkManager and is intentionally out of scope.
 */
class AppSyncCoordinator(
    private val auth: FirebaseAuth,
    private val boardRepository: BoardRepository,
    private val reminderRepository: ReminderRepository,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isForeground = false
    private var listeningUid: String? = null

    private val authListener = FirebaseAuth.AuthStateListener { reconcile() }

    /** Call once from Application.onCreate. */
    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        auth.addAuthStateListener(authListener)
    }

    override fun onStart(owner: LifecycleOwner) {
        isForeground = true
        reconcile()
    }

    override fun onStop(owner: LifecycleOwner) {
        isForeground = false
        reconcile()
    }

    @Synchronized
    private fun reconcile() {
        val uid = auth.currentUser?.uid
        if (isForeground && uid != null) {
            if (listeningUid != uid) {
                // (Re)attach listeners for the current user.
                listeningUid = uid
                reminderRepository.startFirestoreListener(uid)
                boardRepository.startFirestoreListener(uid)
            }
            // Catch up on anything that changed while we weren't listening.
            boardRepository.triggerSync()
            scope.launch { reminderRepository.rescheduleAllReminders() }
        } else if (listeningUid != null) {
            listeningUid = null
            reminderRepository.stopFirestoreListener()
            boardRepository.stopFirestoreListener()
        }
    }
}
