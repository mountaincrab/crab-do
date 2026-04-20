package com.mountaincrab.crabdo.ui.boards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.crabdo.data.local.entity.SubtaskEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.data.repository.SubtaskRepository
import com.mountaincrab.crabdo.data.repository.TaskRepository
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    private val taskId: String,
    private val taskRepository: TaskRepository,
    private val subtaskRepository: SubtaskRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    var isTimeInputKeyboard by mutableStateOf(false)

    init {
        viewModelScope.launch {
            isTimeInputKeyboard = userPrefsRepository.timeInputKeyboard.first()
        }
    }

    fun updateTimeInputKeyboard(value: Boolean) {
        isTimeInputKeyboard = value
        viewModelScope.launch { userPrefsRepository.setTimeInputKeyboard(value) }
    }

    val task: StateFlow<TaskEntity?> =
        taskRepository.observeTask(taskId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subtasks: StateFlow<List<SubtaskEntity>> =
        subtaskRepository.observeSubtasks(taskId)
            .map { list -> list.sortedWith(compareBy({ it.isCompleted }, { it.order })) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTitle(title: String) {
        viewModelScope.launch {
            task.value?.let { taskRepository.updateTask(it.copy(title = title)) }
        }
    }

    fun updateDescription(description: String) {
        viewModelScope.launch {
            task.value?.let { taskRepository.updateTask(it.copy(description = description)) }
        }
    }

    fun setReminder(timeMillis: Long, style: TaskEntity.ReminderStyle) {
        viewModelScope.launch {
            task.value?.let {
                taskRepository.updateTask(it.copy(reminderTimeMillis = timeMillis, reminderStyle = style))
            }
        }
    }

    fun clearReminder() {
        viewModelScope.launch {
            task.value?.let {
                taskRepository.updateTask(it.copy(reminderTimeMillis = null))
            }
        }
    }

    fun addSubtask(title: String) {
        viewModelScope.launch { subtaskRepository.createSubtask(taskId, title) }
    }

    fun toggleSubtask(subtaskId: String, isCompleted: Boolean) {
        viewModelScope.launch { subtaskRepository.setCompleted(subtaskId, isCompleted) }
    }

    fun deleteSubtask(subtaskId: String) {
        viewModelScope.launch { subtaskRepository.deleteSubtask(subtaskId) }
    }

    fun renameSubtask(subtaskId: String, title: String) {
        viewModelScope.launch { subtaskRepository.renameSubtask(subtaskId, title) }
    }

    fun reorderSubtask(subtaskId: String, orderBefore: Double, orderAfter: Double) {
        viewModelScope.launch { subtaskRepository.reorderSubtask(subtaskId, orderBefore, orderAfter) }
    }

    fun deleteTask(onDeleted: () -> Unit) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            onDeleted()
        }
    }
}
