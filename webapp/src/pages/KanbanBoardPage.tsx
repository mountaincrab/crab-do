import { useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Check, ChevronDown, ChevronRight, ChevronUp, ListChecks, MoreHorizontal, Plus, Settings2 } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useBoard, SubtaskCount } from '../hooks/useBoard'
import { Column, Subtask, Task } from '../types'
import AppShell from '../components/AppShell'
import TaskModal from '../components/TaskModal'

export default function KanbanBoardPage() {
  const { boardId } = useParams<{ boardId: string }>()
  const { user } = useAuth()
  const {
    board, columns, tasksByColumn, subtaskCounts, subtasksByTask, loading,
    addColumn, renameColumn, deleteColumn, reorderColumns,
    addTask, moveTask, deleteTask, toggleSubtask,
  } = useBoard(user!.uid, boardId!)

  const [showAddColumn, setShowAddColumn] = useState(false)
  const [newColTitle, setNewColTitle] = useState('')
  const [renamingCol, setRenamingCol] = useState<Column | null>(null)
  const [renameValue, setRenameValue] = useState('')
  const [draggingTaskId, setDraggingTaskId] = useState<string | null>(null)
  const [showManageColumns, setShowManageColumns] = useState(false)
  const [editingTaskId, setEditingTaskId] = useState<string | null>(null)
  const [editingIsNew, setEditingIsNew] = useState(false)
  const [expandedTasks, setExpandedTasks] = useState<Set<string>>(new Set())

  const toggleExpand = (taskId: string) => {
    setExpandedTasks((prev) => {
      const next = new Set(prev)
      if (next.has(taskId)) next.delete(taskId)
      else next.add(taskId)
      return next
    })
  }

  const openTask = (taskId: string) => { setEditingTaskId(taskId); setEditingIsNew(false) }
  const closeTask = () => { setEditingTaskId(null); setEditingIsNew(false) }
  const createAndOpenTask = async (columnId: string) => {
    const id = await addTask(columnId, '', '')
    setEditingTaskId(id)
    setEditingIsNew(true)
  }

  if (loading) {
    return (
      <AppShell>
        <div className="flex-1 flex items-center justify-center text-fg-faint">Loading…</div>
      </AppShell>
    )
  }

  const handleAddColumn = async () => {
    if (!newColTitle.trim()) return
    await addColumn(newColTitle.trim())
    setNewColTitle('')
    setShowAddColumn(false)
  }

  const handleRenameSubmit = async () => {
    if (!renamingCol || !renameValue.trim()) return
    await renameColumn(renamingCol.id, renameValue.trim())
    setRenamingCol(null)
  }

  const handleMoveUp = (index: number) => {
    if (index === 0) return
    const ids = columns.map((c) => c.id)
    ;[ids[index - 1], ids[index]] = [ids[index], ids[index - 1]]
    reorderColumns(ids)
  }

  const handleMoveDown = (index: number) => {
    if (index === columns.length - 1) return
    const ids = columns.map((c) => c.id)
    ;[ids[index], ids[index + 1]] = [ids[index + 1], ids[index]]
    reorderColumns(ids)
  }

  return (
    <AppShell>
      <header className="border-b border-DEFAULT bg-surface px-6 h-14 flex items-center gap-4 shrink-0">
        <h1 className="font-bold text-lg tracking-tightish flex-1 truncate">{board?.title ?? '…'}</h1>
        <button
          onClick={() => setShowAddColumn(true)}
          className="flex items-center gap-1.5 bg-accent hover:bg-accent-hover text-accent-fg px-3 py-1.5 rounded-xl text-sm font-semibold transition-colors"
        >
          <Plus size={15} /> Add column
        </button>
        <button
          onClick={() => setShowManageColumns(true)}
          className="text-fg-muted hover:text-fg transition-colors p-1 rounded"
          title="Manage columns"
        >
          <Settings2 size={18} />
        </button>
      </header>

      <div className="flex-1 overflow-x-auto">
        <div className="flex gap-4 p-6 h-full items-stretch">
          {columns.length === 0 ? (
            <div className="flex-1 flex items-center justify-center text-fg-faint text-sm">
              No columns yet. Add one to get started.
            </div>
          ) : (
            columns.map((col) => (
              <KanbanColumnView
                key={col.id}
                column={col}
                tasks={tasksByColumn[col.id] ?? []}
                subtaskCounts={subtaskCounts}
                subtasksByTask={subtasksByTask}
                onToggleSubtask={toggleSubtask}
                expandedTasks={expandedTasks}
                onToggleExpand={toggleExpand}
                allColumns={columns}
                draggingTaskId={draggingTaskId}
                onDragStart={(taskId) => setDraggingTaskId(taskId)}
                onDragEnd={() => setDraggingTaskId(null)}
                onAddTask={() => createAndOpenTask(col.id)}
                onMoveTask={moveTask}
                onDeleteTask={deleteTask}
                onRename={() => { setRenamingCol(col); setRenameValue(col.title) }}
                onDelete={() => deleteColumn(col.id)}
                onTaskClick={openTask}
              />
            ))
          )}
        </div>
      </div>

      {showAddColumn && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-surface-raised border border-DEFAULT rounded-2xl p-6 w-full max-w-sm shadow-dialog">
            <h2 className="text-lg font-bold mb-4 text-fg">Add Column</h2>
            <input
              autoFocus
              value={newColTitle}
              onChange={(e) => setNewColTitle(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleAddColumn()
                if (e.key === 'Escape') { setShowAddColumn(false); setNewColTitle('') }
              }}
              placeholder="Column name"
              className="w-full bg-surface-raised border border-DEFAULT rounded-lg px-3 py-2.5 text-fg placeholder:text-fg-faint outline-none focus:border-accent mb-4 transition-colors"
            />
            <div className="flex justify-end gap-2">
              <button
                onClick={() => { setShowAddColumn(false); setNewColTitle('') }}
                className="px-4 py-2 text-fg-muted hover:text-fg text-sm rounded-xl font-semibold transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleAddColumn}
                disabled={!newColTitle.trim()}
                className="px-4 py-2 bg-accent hover:bg-accent-hover disabled:opacity-40 text-accent-fg text-sm rounded-xl font-semibold transition-colors"
              >
                Add
              </button>
            </div>
          </div>
        </div>
      )}

      {renamingCol && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-surface-raised border border-DEFAULT rounded-2xl p-6 w-full max-w-sm shadow-dialog">
            <h2 className="text-lg font-bold mb-4 text-fg">Rename Column</h2>
            <input
              autoFocus
              value={renameValue}
              onChange={(e) => setRenameValue(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') handleRenameSubmit() }}
              className="w-full bg-surface-raised border border-DEFAULT rounded-lg px-3 py-2.5 text-fg outline-none focus:border-accent mb-4 transition-colors"
            />
            <div className="flex justify-end gap-2">
              <button onClick={() => setRenamingCol(null)} className="px-4 py-2 text-fg-muted hover:text-fg text-sm rounded-xl font-semibold transition-colors">Cancel</button>
              <button onClick={handleRenameSubmit} className="px-4 py-2 bg-accent hover:bg-accent-hover text-accent-fg text-sm rounded-xl font-semibold transition-colors">Save</button>
            </div>
          </div>
        </div>
      )}

      {showManageColumns && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-surface-raised border border-DEFAULT rounded-2xl p-6 w-full max-w-sm shadow-dialog">
            <h2 className="text-lg font-bold mb-4 text-fg">Manage Columns</h2>
            {columns.length === 0 ? (
              <p className="text-sm text-fg-faint mb-4">No columns yet.</p>
            ) : (
              <div className="flex flex-col gap-1 mb-4">
                {columns.map((col, i) => (
                  <div key={col.id} className="flex items-center gap-1 py-1.5 px-2 rounded-lg hover:bg-surface-high group">
                    <span className="flex-1 text-sm text-fg truncate">{col.title}</span>
                    <button
                      onClick={() => handleMoveUp(i)}
                      disabled={i === 0}
                      className="p-1 text-fg-faint hover:text-fg disabled:opacity-25 transition-colors rounded"
                      title="Move up"
                    >
                      <ChevronUp size={15} />
                    </button>
                    <button
                      onClick={() => handleMoveDown(i)}
                      disabled={i === columns.length - 1}
                      className="p-1 text-fg-faint hover:text-fg disabled:opacity-25 transition-colors rounded"
                      title="Move down"
                    >
                      <ChevronDown size={15} />
                    </button>
                  </div>
                ))}
              </div>
            )}
            <div className="flex justify-end">
              <button
                onClick={() => setShowManageColumns(false)}
                className="px-4 py-2 bg-accent hover:bg-accent-hover text-accent-fg text-sm rounded-xl font-semibold transition-colors"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {editingTaskId && (
        <TaskModal
          userId={user!.uid}
          boardId={boardId!}
          taskId={editingTaskId}
          isNew={editingIsNew}
          onClose={closeTask}
          onDiscard={() => deleteTask(editingTaskId)}
        />
      )}
    </AppShell>
  )
}

function orderForGap(gap: number, tasks: Task[]): number {
  const prev = tasks[gap - 1]
  const next = tasks[gap]
  if (!prev && !next) return 1
  if (!prev) return next.order - 1
  if (!next) return prev.order + 1
  return (prev.order + next.order) / 2
}

function DropIndicator() {
  return (
    <div className="py-0.5 pointer-events-none">
      <div className="h-[3px] w-full rounded-full bg-accent" />
    </div>
  )
}

interface ColumnViewProps {
  column: Column
  tasks: Task[]
  subtaskCounts: Record<string, SubtaskCount>
  subtasksByTask: Record<string, Subtask[]>
  onToggleSubtask: (taskId: string, subtaskId: string, isCompleted: boolean) => void
  expandedTasks: Set<string>
  onToggleExpand: (taskId: string) => void
  allColumns: Column[]
  draggingTaskId: string | null
  onDragStart: (taskId: string) => void
  onDragEnd: () => void
  onAddTask: () => void
  onMoveTask: (taskId: string, targetColumnId: string, newOrder: number) => void
  onDeleteTask: (taskId: string) => void
  onRename: () => void
  onDelete: () => void
  onTaskClick: (taskId: string) => void
}

function KanbanColumnView({
  column, tasks, subtaskCounts, subtasksByTask, onToggleSubtask, expandedTasks, onToggleExpand, allColumns,
  draggingTaskId,
  onDragStart, onDragEnd,
  onAddTask, onMoveTask, onDeleteTask,
  onRename, onDelete, onTaskClick,
}: ColumnViewProps) {
  const [showColMenu, setShowColMenu] = useState(false)
  const [hoverGap, setHoverGap] = useState<number | null>(null)
  const tasksRef = useRef<HTMLDivElement>(null)

  const visibleTasks = draggingTaskId
    ? tasks.filter((t) => t.id !== draggingTaskId)
    : tasks

  const isDragging = draggingTaskId !== null

  const getGapFromEvent = (e: React.DragEvent): number => {
    const cards = tasksRef.current?.querySelectorAll('[data-task-card]') ?? []
    for (let i = 0; i < cards.length; i++) {
      const rect = cards[i].getBoundingClientRect()
      if (e.clientY < rect.top + rect.height / 2) return i
    }
    return cards.length
  }

  const handleDragOver = (e: React.DragEvent) => {
    if (!isDragging) return
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    setHoverGap(getGapFromEvent(e))
  }

  const handleDragLeave = (e: React.DragEvent) => {
    if (!e.currentTarget.contains(e.relatedTarget as Node)) {
      setHoverGap(null)
    }
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    const taskId = e.dataTransfer.getData('taskId')
    const gap = getGapFromEvent(e)
    setHoverGap(null)
    if (taskId) {
      onMoveTask(taskId, column.id, orderForGap(gap, visibleTasks))
    }
    onDragEnd()
  }

  return (
    <div
      className={`flex-1 min-w-[240px] flex flex-col rounded-xl p-1 transition-colors ${
        isDragging && hoverGap !== null ? 'bg-accent-soft ring-1 ring-accent/30' : ''
      }`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <div className="flex items-center justify-between px-1 shrink-0">
        <div className="flex items-center gap-2">
          <span className="font-bold text-sm text-fg">{column.title}</span>
          <span className="text-xs text-fg-muted bg-surface-high px-2 py-0.5 rounded-full font-semibold">{tasks.length}</span>
        </div>
        <div className="relative">
          <button
            onClick={() => setShowColMenu((v) => !v)}
            className="text-fg-faint hover:text-fg p-1 rounded transition-colors leading-none"
          >
            <MoreHorizontal size={16} />
          </button>
          {showColMenu && (
            <div className="absolute right-0 top-full mt-1 bg-surface-high border border-DEFAULT rounded-lg shadow-dialog z-20 min-w-32 py-1">
              <button
                onClick={() => { onRename(); setShowColMenu(false) }}
                className="w-full text-left px-4 py-2 text-sm text-fg hover:bg-white/5 transition-colors"
              >
                Rename
              </button>
              <button
                onClick={() => { onDelete(); setShowColMenu(false) }}
                className="w-full text-left px-4 py-2 text-sm text-danger-text hover:bg-white/5 transition-colors"
              >
                Delete column
              </button>
            </div>
          )}
        </div>
      </div>

      <div ref={tasksRef} className="flex-1 overflow-y-auto flex flex-col gap-2 mt-2 min-h-0">
        {visibleTasks.map((task, i) => (
          <div key={task.id}>
            {isDragging && hoverGap === i && <DropIndicator />}
            <TaskCardView
              task={task}
              subtaskCount={subtaskCounts[task.id]}
              subtasks={subtasksByTask[task.id] ?? []}
              onToggleSubtask={onToggleSubtask}
              expanded={expandedTasks.has(task.id)}
              onToggleExpand={() => onToggleExpand(task.id)}
              allColumns={allColumns}
              onDragStart={onDragStart}
              onDragEnd={onDragEnd}
              onMove={(taskId, colId) => onMoveTask(taskId, colId, orderForGap(visibleTasks.length, tasks.filter((t) => t.columnId === colId && t.id !== taskId)))}
              onDelete={onDeleteTask}
              onClick={() => onTaskClick(task.id)}
            />
          </div>
        ))}
        {isDragging && hoverGap === visibleTasks.length && <DropIndicator />}
        {isDragging && visibleTasks.length === 0 && hoverGap === null && (
          <div className="h-16 rounded-lg border-2 border-dashed border-accent/25 flex items-center justify-center text-xs text-accent/50">
            Drop here
          </div>
        )}
      </div>

      <button
        onClick={onAddTask}
        className="w-full text-left text-fg-faint hover:text-fg px-2 py-1.5 rounded-lg hover:bg-surface-raised transition-colors text-sm font-semibold mt-1 shrink-0"
      >
        + Add task
      </button>
    </div>
  )
}

interface TaskCardProps {
  task: Task
  subtaskCount?: SubtaskCount
  subtasks: Subtask[]
  onToggleSubtask: (taskId: string, subtaskId: string, isCompleted: boolean) => void
  expanded: boolean
  onToggleExpand: () => void
  allColumns: Column[]
  onDragStart: (taskId: string) => void
  onDragEnd: () => void
  onMove: (taskId: string, columnId: string) => void
  onDelete: (taskId: string) => void
  onClick: () => void
}

function TaskCardView({ task, subtaskCount, subtasks, onToggleSubtask, expanded, onToggleExpand, allColumns, onDragStart, onDragEnd, onMove, onDelete, onClick }: TaskCardProps) {
  const [showMenu, setShowMenu] = useState(false)
  const [showMoveMenu, setShowMoveMenu] = useState(false)
  const otherColumns = allColumns.filter((c) => c.id !== task.columnId)
  const hasSubtasks = subtaskCount && subtaskCount.total > 0
  const allDone = hasSubtasks && subtaskCount!.completed === subtaskCount!.total
  const incompleteSubtasks = subtasks.filter((s) => !s.isCompleted)

  const handleDragStart = (e: React.DragEvent) => {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('taskId', task.id)
    e.dataTransfer.setData('sourceColumnId', task.columnId)
    setTimeout(() => onDragStart(task.id), 0)
  }

  return (
    <div
      data-task-card
      draggable
      onDragStart={handleDragStart}
      onDragEnd={onDragEnd}
      className="bg-surface-raised hover:bg-surface-high border border-DEFAULT rounded-xl p-3 cursor-grab active:cursor-grabbing transition-colors relative group select-none"
      onClick={onClick}
    >
      <p className="text-sm text-fg leading-snug font-medium pr-5">{task.title}</p>
      {task.description && (
        <p className="text-xs text-fg-faint mt-1 line-clamp-2">{task.description}</p>
      )}

      {hasSubtasks && (
        <button
          onClick={(e) => { e.stopPropagation(); onToggleExpand() }}
          className={`inline-flex items-center gap-1 mt-2 text-xs font-medium font-mono px-1.5 py-0.5 rounded-md transition-colors ${
            allDone ? 'bg-accent-soft text-success-text' : 'bg-surface-high text-fg-muted hover:text-fg'
          }`}
        >
          <ListChecks size={12} />
          {subtaskCount!.completed}/{subtaskCount!.total}
          {expanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
        </button>
      )}

      {expanded && hasSubtasks && (
        <div className="mt-2 flex flex-col gap-0.5" onClick={(e) => e.stopPropagation()}>
          {incompleteSubtasks.length === 0 ? (
            <span className="text-xs text-fg-faint py-1">All items complete</span>
          ) : (
            incompleteSubtasks.map((s) => (
              <div key={s.id} className="flex items-center gap-2 py-0.5 group/sub">
                <button
                  onClick={(e) => { e.stopPropagation(); onToggleSubtask(task.id, s.id, true) }}
                  className="w-4 h-4 rounded border border-fg-muted hover:border-accent hover:bg-accent-soft flex items-center justify-center shrink-0 transition-colors"
                  style={{ borderWidth: '1.5px' }}
                  aria-label="Complete subtask"
                >
                  <Check size={10} strokeWidth={3} className="text-accent opacity-0 group-hover/sub:opacity-60 transition-opacity" />
                </button>
                <span className="text-xs text-fg-muted leading-snug">{s.title}</span>
              </div>
            ))
          )}
        </div>
      )}

      <div
        className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={() => { setShowMenu((v) => !v); setShowMoveMenu(false) }}
          className="text-fg-faint hover:text-fg p-1 rounded leading-none"
        >
          <MoreHorizontal size={14} />
        </button>

        {showMenu && (
          <div className="absolute right-0 top-full mt-1 bg-surface-high border border-DEFAULT rounded-lg shadow-dialog z-20 min-w-36 py-1">
            {otherColumns.length > 0 && (
              <div className="relative">
                <button
                  onClick={() => setShowMoveMenu((v) => !v)}
                  className="w-full text-left px-4 py-2 text-sm text-fg hover:bg-white/5 transition-colors flex items-center justify-between"
                >
                  Move to <ChevronRight size={14} className="text-fg-faint" />
                </button>
                {showMoveMenu && (
                  <div className="absolute left-full top-0 ml-1 bg-surface-high border border-DEFAULT rounded-lg shadow-dialog z-30 min-w-36 py-1">
                    {otherColumns.map((col) => (
                      <button
                        key={col.id}
                        onClick={() => { onMove(task.id, col.id); setShowMenu(false); setShowMoveMenu(false) }}
                        className="w-full text-left px-4 py-2 text-sm text-fg hover:bg-white/5 transition-colors"
                      >
                        {col.title}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
            <button
              onClick={() => { onDelete(task.id); setShowMenu(false) }}
              className="w-full text-left px-4 py-2 text-sm text-danger-text hover:bg-white/5 transition-colors"
            >
              Delete
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
