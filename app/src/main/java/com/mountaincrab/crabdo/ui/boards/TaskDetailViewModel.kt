package com.mountaincrab.crabdo.ui.boards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.crabdo.data.local.entity.SubtaskEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.data.repository.SubtaskRepository
import com.mountaincrab.crabdo.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val subtaskRepository: SubtaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    val task: StateFlow<TaskEntity?> =
        taskRepository.observeTask(taskId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subtasks: StateFlow<List<SubtaskEntity>> =
        subtaskRepository.observeSubtasks(taskId)
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

    fun deleteTask(onDeleted: () -> Unit) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            onDeleted()
        }
    }
}
