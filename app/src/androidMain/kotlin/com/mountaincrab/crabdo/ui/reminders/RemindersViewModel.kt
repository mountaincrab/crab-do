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

    val completedOneOffs: StateFlow<List<OneOffReminderEntity>> =
        reminderRepository.observeCompletedOneOffs(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedOneOffs: StateFlow<List<OneOffReminderEntity>> =
        reminderRepository.observeDeletedOneOffs(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringReminders: StateFlow<List<RecurringReminderEntity>> =
        reminderRepository.observeRecurring(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedRecurring: StateFlow<List<RecurringReminderEntity>> =
        reminderRepository.observeDeletedRecurring(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSyncing: StateFlow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow("sync")
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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
