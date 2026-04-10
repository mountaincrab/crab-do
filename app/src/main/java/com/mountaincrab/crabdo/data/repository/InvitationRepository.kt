package com.mountaincrab.crabdo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mountaincrab.crabdo.data.local.dao.*
import com.mountaincrab.crabdo.data.local.entity.BoardAccessEntity
import com.mountaincrab.crabdo.data.model.Invitation
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.data.remote.toBoardEntity
import com.mountaincrab.crabdo.data.remote.toColumnEntity
import com.mountaincrab.crabdo.data.remote.toTaskEntity
import com.mountaincrab.crabdo.data.remote.toSubtaskEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvitationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val boardDao: BoardDao,
    private val columnDao: ColumnDao,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val boardAccessDao: BoardAccessDao
) {
    private val invitationsRef = firestore.collection("invitations")

    fun observePendingInvitations(): Flow<List<Invitation>> = callbackFlow {
        val email = auth.currentUser?.email?.lowercase()?.trim()
        if (email == null) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }

        val registration: ListenerRegistration = invitationsRef
            .whereEqualTo("inviteeEmail", email)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val invitations = snapshot.documents.mapNotNull { doc ->
                    Invitation(
                        id = doc.id,
                        ownerUid = doc.getString("ownerUid") ?: return@mapNotNull null,
                        ownerDisplayName = doc.getString("ownerDisplayName") ?: "",
                        boardId = doc.getString("boardId") ?: return@mapNotNull null,
                        boardTitle = doc.getString("boardTitle") ?: "",
                        inviteeEmail = doc.getString("inviteeEmail") ?: "",
                        status = doc.getString("status") ?: "pending",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                }
                trySend(invitations)
            }

        awaitClose { registration.remove() }
    }

    suspend fun sendInvitation(boardId: String, boardTitle: String, inviteeEmail: String) {
        val user = auth.currentUser ?: return
        val email = inviteeEmail.lowercase().trim()
        val docId = "${boardId}_${email}"

        invitationsRef.document(docId).set(
            mapOf(
                "ownerUid" to user.uid,
                "ownerDisplayName" to (user.displayName ?: ""),
                "boardId" to boardId,
                "boardTitle" to boardTitle,
                "inviteeEmail" to email,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun acceptInvitation(invitation: Invitation) {
        val user = auth.currentUser ?: return

        // 1. Update invitation status to accepted
        invitationsRef.document(invitation.id)
            .update(
                "status", "accepted",
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

        // 2. Add ourselves to the board's collaborators map
        val boardRef = firestore.collection("users").document(invitation.ownerUid)
            .collection("boards").document(invitation.boardId)

        boardRef.update(
            "collaborators.${user.uid}", mapOf(
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "role" to "editor",
                "addedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // 3. Pull the full board and its subcollections into local Room
        pullSharedBoard(invitation.ownerUid, invitation.boardId)

        // 4. Create local board_access entry
        boardAccessDao.upsert(
            BoardAccessEntity(
                boardId = invitation.boardId,
                userId = user.uid,
                ownerUserId = invitation.ownerUid,
                role = "editor"
            )
        )
    }

    suspend fun declineInvitation(invitationId: String) {
        invitationsRef.document(invitationId)
            .update(
                "status", "declined",
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
    }

    private suspend fun pullSharedBoard(ownerUid: String, boardId: String) {
        val boardRef = firestore.collection("users").document(ownerUid)
            .collection("boards").document(boardId)

        // Pull the board document
        val boardDoc = boardRef.get().await()
        if (!boardDoc.exists()) return
        boardDao.upsert(
            boardDoc.toBoardEntity(ownerUid).copy(
                syncStatus = SyncStatus.SYNCED,
                isShared = true
            )
        )

        // Pull columns
        boardRef.collection("columns").get().await().documents.forEach { doc ->
            columnDao.upsert(doc.toColumnEntity().copy(syncStatus = SyncStatus.SYNCED))
        }

        // Pull tasks and their subtasks
        boardRef.collection("tasks").get().await().documents.forEach { taskDoc ->
            taskDao.upsert(taskDoc.toTaskEntity().copy(syncStatus = SyncStatus.SYNCED))

            boardRef.collection("tasks").document(taskDoc.id)
                .collection("subtasks").get().await().documents.forEach { subDoc ->
                    subtaskDao.upsert(subDoc.toSubtaskEntity().copy(syncStatus = SyncStatus.SYNCED))
                }
        }
    }
}
