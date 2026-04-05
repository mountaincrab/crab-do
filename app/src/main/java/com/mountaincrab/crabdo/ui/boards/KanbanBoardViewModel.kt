package com.mountaincrab.crabdo.ui.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mountaincrab.crabdo.data.local.entity.BoardEntity
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.data.repository.BoardRepository
import com.mountaincrab.crabdo.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = KanbanBoardViewModel.Factory::class)
class KanbanBoardViewModel @AssistedInject constructor(
    @Assisted private val boardId: String,
    private val boardRepository: BoardRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(boardId: String): KanbanBoardViewModel
    }

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

    fun createTask(columnId: String, title: String) {
        viewModelScope.launch { taskRepository.createTask(boardId, columnId, title) }
    }

    fun moveTask(taskId: String, targetColumnId: String, orderBefore: Double, orderAfter: Double) {
        viewModelScope.launch {
            taskRepository.moveTask(taskId, targetColumnId, orderBefore, orderAfter)
        }
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
}
