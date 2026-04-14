package com.mountaincrab.crabdo.ui.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.local.entity.BoardEntity
import com.mountaincrab.crabdo.data.model.Invitation
import com.mountaincrab.crabdo.data.repository.BoardRepository
import com.mountaincrab.crabdo.data.repository.InvitationRepository
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BoardListViewModel(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val invitationRepository: InvitationRepository,
    private val workManager: WorkManager,
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    private val userId = authRepository.currentUserId ?: ""

    init {
        if (userId.isNotEmpty()) reminderRepository.startFirestoreListener(userId)
    }

    val boards: StateFlow<List<BoardEntity>> =
        boardRepository.observeBoards(userId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pinnedBoardId: StateFlow<String?> =
        prefsRepository.pinnedBoardId
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pendingInvitations: StateFlow<List<Invitation>> =
        invitationRepository.observePendingInvitations()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isSyncing: StateFlow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow("sync")
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun createBoard(title: String) {
        viewModelScope.launch { boardRepository.createBoard(userId, title) }
    }

    fun renameBoard(board: BoardEntity, newTitle: String) {
        viewModelScope.launch { boardRepository.updateBoard(board.copy(title = newTitle)) }
    }

    fun deleteBoard(boardId: String) {
        viewModelScope.launch { boardRepository.deleteBoard(boardId) }
    }

    fun pinBoard(boardId: String) {
        viewModelScope.launch { prefsRepository.setPinnedBoardId(boardId) }
    }

    fun unpinBoard() {
        viewModelScope.launch { prefsRepository.setPinnedBoardId(null) }
    }

    fun sync() {
        boardRepository.triggerSync()
    }

    fun acceptInvitation(invitation: Invitation) {
        viewModelScope.launch {
            try {
                invitationRepository.acceptInvitation(invitation)
                boardRepository.triggerSync()
            } catch (e: Exception) {
                android.util.Log.e("BoardListVM", "Failed to accept invitation", e)
            }
        }
    }

    fun declineInvitation(invitation: Invitation) {
        viewModelScope.launch {
            try {
                invitationRepository.declineInvitation(invitation.id)
            } catch (e: Exception) {
                android.util.Log.e("BoardListVM", "Failed to decline invitation", e)
            }
        }
    }

    fun shareBoard(boardId: String, boardTitle: String, email: String) {
        viewModelScope.launch {
            try {
                invitationRepository.sendInvitation(boardId, boardTitle, email)
            } catch (e: Exception) {
                android.util.Log.e("BoardListVM", "Failed to send invitation", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        reminderRepository.stopFirestoreListener()
    }
}
