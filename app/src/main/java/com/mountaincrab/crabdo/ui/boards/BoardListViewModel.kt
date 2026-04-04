package com.mountaincrab.crabdo.ui.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.local.entity.BoardEntity
import com.mountaincrab.crabdo.data.repository.BoardRepository
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardListViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val userId = authRepository.currentUserId ?: ""

    val boards: StateFlow<List<BoardEntity>> =
        boardRepository.observeBoards(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedBoardId: StateFlow<String?> =
        prefsRepository.pinnedBoardId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
}
