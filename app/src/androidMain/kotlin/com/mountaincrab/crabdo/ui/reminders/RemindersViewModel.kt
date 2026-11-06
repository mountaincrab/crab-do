package com.mountaincrab.crabdo.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.local.entity.OneOffReminderEntity
import com.mountaincrab.crabdo.data.local.entity.RecurringReminderEntity
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import com.mountaincrab.crabdo.data.repository.TaskRepository
import com.mountaincrab.crabdo.data.local.entity.TaskReminderSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersViewModel(
    private val reminderRepository: ReminderRepository,
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    private val userId = authRepository.currentUserId ?: ""

    val taskReminders: StateFlow<List<TaskReminderSummary>> =
        taskRepository.observeReminderSummary(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val oneOffReminders: StateFlow<List<OneOffReminderEntity>> =
        reminderRepository.observeOneOffs(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyPageStates = reminderRepository.historyPageStates

    private val completedLimit = MutableStateFlow(0)
    private val deletedLimit = MutableStateFlow(0)
    private val deletedRecurringLimit = MutableStateFlow(0)

    val completedCount = reminderRepository.observeCompletedCount(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val deletedCount = reminderRepository.observeDeletedCount(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val deletedRecurringCount = reminderRepository.observeDeletedRecurringCount(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun showCompleted(show: Boolean) { completedLimit.value = if (show) maxOf(50, completedLimit.value) else 0 }
    fun showDeleted(show: Boolean) { deletedLimit.value = if (show) maxOf(50, deletedLimit.value) else 0 }
    fun showDeletedRecurring(show: Boolean) { deletedRecurringLimit.value = if (show) maxOf(50, deletedRecurringLimit.value) else 0 }
    fun retryHistory(kind: String) {
        val limit = when (kind) {
            "completed" -> completedLimit.value
            "deleted" -> deletedLimit.value
            else -> deletedRecurringLimit.value
        }
        reminderRepository.setHistoryWindow(userId, kind, limit)
    }

    fun moreCompleted() { completedLimit.update { it + 50 } }
    fun moreDeleted() { deletedLimit.update { it + 50 } }
    fun moreDeletedRecurring() { deletedRecurringLimit.update { it + 50 } }

    val completedOneOffs: StateFlow<List<OneOffReminderEntity>> =
        completedLimit.flatMapLatest {
            reminderRepository.setHistoryWindow(userId, "completed", it)
            reminderRepository.observeCompletedOneOffs(userId, it)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedOneOffs: StateFlow<List<OneOffReminderEntity>> =
        deletedLimit.flatMapLatest {
            reminderRepository.setHistoryWindow(userId, "deleted", it)
            reminderRepository.observeDeletedOneOffs(userId, it)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringReminders: StateFlow<List<RecurringReminderEntity>> =
        reminderRepository.observeRecurring(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedRecurring: StateFlow<List<RecurringReminderEntity>> =
        deletedRecurringLimit.flatMapLatest {
            reminderRepository.setHistoryWindow(userId, "deletedRecurring", it)
            reminderRepository.observeDeletedRecurring(userId, it)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSyncing: StateFlow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow("sync")
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override fun onCleared() {
        reminderRepository.stopHistoryListeners()
        super.onCleared()
    }

    fun sync() { reminderRepository.triggerSync() }

    fun deleteOneOff(id: String) {
        viewModelScope.launch { reminderRepository.deleteOneOff(id) }
    }

    fun deleteTaskReminder(taskId: String) {
        viewModelScope.launch { taskRepository.clearTaskReminder(taskId) }
    }

    fun deleteRecurring(id: String) {
        viewModelScope.launch { reminderRepository.deleteRecurring(id) }
    }

    fun dismissRecurring(id: String) {
        viewModelScope.launch { reminderRepository.dismissNextRecurring(id) }
    }

    fun toggleRecurringEnabled(reminder: RecurringReminderEntity) {
        viewModelScope.launch {
            reminderRepository.updateRecurring(reminder.copy(isEnabled = !reminder.isEnabled))
        }
    }

    fun restoreOneOff(id: String) {
        viewModelScope.launch { reminderRepository.restoreOneOff(id) }
    }

    fun restoreRecurring(id: String) {
        viewModelScope.launch { reminderRepository.restoreRecurring(id) }
    }
}
