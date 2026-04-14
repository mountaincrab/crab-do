package com.mountaincrab.crabdo.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersViewModel(
    private val reminderRepository: ReminderRepository,
    private val authRepository: AuthRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    private val userId = authRepository.currentUserId ?: ""

    val reminders: StateFlow<List<ReminderEntity>> =
        reminderRepository.observeReminders(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedReminders: StateFlow<List<ReminderEntity>> =
        reminderRepository.observeCompletedReminders(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSyncing: StateFlow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow("sync")
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun sync() { reminderRepository.triggerSync() }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch { reminderRepository.deleteReminder(reminderId) }
    }

    fun toggleEnabled(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderRepository.updateReminder(reminder.copy(isEnabled = !reminder.isEnabled))
        }
    }
}
