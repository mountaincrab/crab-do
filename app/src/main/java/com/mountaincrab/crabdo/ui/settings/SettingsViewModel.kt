package com.mountaincrab.crabdo.ui.settings

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.local.entity.BoardEntity
import com.mountaincrab.crabdo.data.repository.BoardRepository
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val boardRepository: BoardRepository,
    private val prefsRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val userId = authRepository.currentUserId ?: ""

    val boards: StateFlow<List<BoardEntity>> =
        boardRepository.observeBoards(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedBoardId: StateFlow<String?> =
        prefsRepository.pinnedBoardId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userEmail: String? get() = authRepository.currentUser?.email
    val userDisplayName: String? get() = authRepository.currentUser?.displayName

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            authRepository.signOut(context)
            onDone()
        }
    }

    val canScheduleExactAlarms: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() ?: false
        } else true

    fun setPinnedBoard(boardId: String?) {
        viewModelScope.launch { prefsRepository.setPinnedBoardId(boardId) }
    }
}
