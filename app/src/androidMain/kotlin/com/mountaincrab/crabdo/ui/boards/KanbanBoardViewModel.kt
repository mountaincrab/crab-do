package com.mountaincrab.crabdo.ui.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.crabdo.data.local.dao.SubtaskCountAggregate
import com.mountaincrab.crabdo.data.local.entity.BoardEntity
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import com.mountaincrab.crabdo.data.local.entity.SubtaskEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.data.repository.BoardRepository
import com.mountaincrab.crabdo.data.repository.SubtaskRepository
import com.mountaincrab.crabdo.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

class KanbanBoardViewModel(
    private val boardId: String,
    private val boardRepository: BoardRepository,
    private val taskRepository: TaskRepository,
    private val subtaskRepository: SubtaskRepository,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val board: StateFlow<BoardEntity?> =
        boardRepository.observeBoard(boardId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val columns: StateFlow<List<ColumnEntity>> =
        boardRepository.observeColumns(boardId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasksByColumn: StateFlow<Map<String, List<TaskEntity>>> =
        columns.flatMapLatest { cols ->
            if (cols.isEmpty()) flowOf(emptyMap())
            else combine(cols.map { col ->
                taskRepository.observeTasksByColumn(col.id)
                    .map { tasks -> col.id to tasks }
            }) { pairs -> pairs.toMap() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val subtaskCounts: StateFlow<Map<String, SubtaskCountAggregate>> =
        subtaskRepository.observeSubtaskCountsForBoard(boardId)
            .map { list -> list.associateBy { it.taskId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun createTask(
        columnId: String,
        title: String,
        description: String = "",
        reminderTimeMillis: Long? = null,
        reminderStyle: TaskEntity.ReminderStyle = TaskEntity.ReminderStyle.ALARM
    ) {
        viewModelScope.launch {
            taskRepository.createTask(
                boardId, columnId, title, description, reminderTimeMillis, reminderStyle
            )
        }
    }

    fun moveTask(taskId: String, targetColumnId: String, orderBefore: Double, orderAfter: Double) {
        viewModelScope.launch {
            taskRepository.moveTask(taskId, targetColumnId, orderBefore, orderAfter)
        }
    }

    fun saveTaskChanges(
        taskId: String,
        title: String,
        description: String,
        reminderTimeMillis: Long?,
        reminderStyle: TaskEntity.ReminderStyle,
        toColumnId: String
    ) {
        viewModelScope.launch {
            val existing = tasksByColumn.value.values.flatten()
                .firstOrNull { it.id == taskId } ?: return@launch
            val updated = existing.copy(
                title = title,
                description = description,
                reminderTimeMillis = reminderTimeMillis,
                reminderStyle = reminderStyle,
            )
            taskRepository.updateTask(updated)
            if (toColumnId != existing.columnId) {
                val targetTasks = tasksByColumn.value[toColumnId] ?: emptyList()
                val minOrder = targetTasks.minOfOrNull { it.order } ?: 1.0
                taskRepository.moveTask(taskId, toColumnId, minOrder - 2.0, minOrder)
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch { taskRepository.deleteTask(taskId) }
    }

    fun observeSubtasks(taskId: String): Flow<List<SubtaskEntity>> =
        subtaskRepository.observeSubtasks(taskId)
            .map { list ->
                // Incomplete first (by order); completed below, most-recently-completed
                // on top so a just-ticked item lands at the top of the done group.
                list.sortedWith(Comparator { a, b ->
                    when {
                        a.isCompleted != b.isCompleted -> if (a.isCompleted) 1 else -1
                        !a.isCompleted -> a.order.compareTo(b.order)
                        else -> b.updatedAt.compareTo(a.updatedAt)
                    }
                })
            }

    fun addSubtask(taskId: String, title: String) {
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

    fun addColumn(title: String) {
        viewModelScope.launch { boardRepository.createColumn(boardId, title) }
    }

    fun renameColumn(column: ColumnEntity, newTitle: String) {
        viewModelScope.launch { boardRepository.updateColumn(column.copy(title = newTitle)) }
    }

    fun deleteColumn(columnId: String) {
        viewModelScope.launch { boardRepository.deleteColumn(columnId) }
    }

    fun reorderColumns(newOrderedIds: List<String>) {
        viewModelScope.launch { boardRepository.reorderColumns(boardId, newOrderedIds) }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                boardRepository.refreshBoard(boardId)
            } catch (e: Exception) {
                Log.e("KanbanBoardVM", "Failed to refresh board", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
