import { useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useBoard } from '../hooks/useBoard'
import { Column, Task } from '../types'

export default function KanbanBoardPage() {
  const { boardId } = useParams<{ boardId: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()
  const {
    board, columns, tasksByColumn, loading,
    addColumn, renameColumn, deleteColumn,
    addTask, moveTask, deleteTask,
  } = useBoard(user!.uid, boardId!)

  const [showAddColumn, setShowAddColumn] = useState(false)
  const [newColTitle, setNewColTitle] = useState('')
  const [renamingCol, setRenamingCol] = useState<Column | null>(null)
  const [renameValue, setRenameValue] = useState('')
  const [draggingTaskId, setDraggingTaskId] = useState<string | null>(null)

  if (loading) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center text-slate-500">
        Loading…
      </div>
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

  return (
    <div className="min-h-screen bg-surface text-white flex flex-col">
      {/* Header */}
      <header className="border-b border-white/10 px-6 py-3 flex items-center gap-4 shrink-0">
        <button
          onClick={() => navigate('/')}
          className="text-slate-400 hover:text-white transition-colors"
          aria-label="Back"
        >
          ← Back
        </button>
        <h1 className="font-semibold text-lg">{board?.title ?? '…'}</h1>
      </header>

      {/* Board */}
      <div className="flex-1 overflow-x-auto">
        <div className="flex gap-4 p-6 h-full items-start" style={{ minHeight: 'calc(100vh - 57px)' }}>
          {columns.map((col) => (
            <KanbanColumnView
              key={col.id}
              column={col}
              tasks={tasksByColumn[col.id] ?? []}
              allColumns={columns}
              draggingTaskId={draggingTaskId}
              onDragStart={(taskId) => setDraggingTaskId(taskId)}
              onDragEnd={() => setDraggingTaskId(null)}
              onAddTask={(title, desc) => addTask(col.id, title, desc)}
              onMoveTask={moveTask}
              onDeleteTask={deleteTask}
              onRename={() => { setRenamingCol(col); setRenameValue(col.title) }}
              onDelete={() => deleteColumn(col.id)}
              onTaskClick={(taskId) => navigate(`/board/${boardId}/task/${taskId}`)}
            />
          ))}

          {/* Add column */}
          <div className="shrink-0 w-64">
            {showAddColumn ? (
              <div className="bg-surface-raised rounded-xl p-3">
                <input
                  autoFocus
                  value={newColTitle}
                  onChange={(e) => setNewColTitle(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleAddColumn()
                    if (e.key === 'Escape') { setShowAddColumn(false); setNewColTitle('') }
                  }}
                  placeholder="Column name"
                  className="w-full bg-surface-high border border-white/10 rounded-lg px-3 py-2 text-white placeholder-slate-500 outline-none focus:border-indigo-500 text-sm mb-2"
                />
                <div className="flex gap-2">
                  <button
                    onClick={handleAddColumn}
                    className="px-3 py-1.5 bg-indigo-500 hover:bg-indigo-600 text-white rounded-lg text-sm font-medium transition-colors"
                  >
                    Add
                  </button>
                  <button
                    onClick={() => { setShowAddColumn(false); setNewColTitle('') }}
                    className="px-3 py-1.5 text-slate-400 hover:text-white rounded-lg text-sm transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <button
                onClick={() => setShowAddColumn(true)}
                className="w-full text-left text-slate-400 hover:text-white px-3 py-2.5 rounded-xl hover:bg-surface-raised transition-colors text-sm"
              >
                + Add column
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Rename dialog */}
      {renamingCol && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-surface-raised rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <h2 className="text-lg font-semibold mb-4">Rename Column</h2>
            <input
              autoFocus
              value={renameValue}
              onChange={(e) => setRenameValue(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') handleRenameSubmit() }}
              className="w-full bg-surface-high border border-white/10 rounded-lg px-3 py-2.5 text-white outline-none focus:border-indigo-500 mb-4"
            />
            <div className="flex justify-end gap-2">
              <button onClick={() => setRenamingCol(null)} className="px-4 py-2 text-slate-400 hover:text-white text-sm rounded-lg">Cancel</button>
              <button onClick={handleRenameSubmit} className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white text-sm rounded-lg font-medium">Save</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Compute a new order value for inserting at gap index `gap` in `tasks`. */
function orderForGap(gap: number, tasks: Task[]): number {
  const prev = tasks[gap - 1]
  const next = tasks[gap]
  if (!prev && !next) return 1
  if (!prev) return next.order - 1
  if (!next) return prev.order + 1
  return (prev.order + next.order) / 2
}

// ─── Drop indicator ──────────────────────────────────────────────────────────

function DropIndicator() {
  return (
    <div className="py-0.5 pointer-events-none">
      <div className="h-[3px] w-full rounded-full bg-[#4F7CFF]" />
    </div>
  )
}

// ─── Column component ────────────────────────────────────────────────────────

interface ColumnViewProps {
  column: Column
  tasks: Task[]
  allColumns: Column[]
  draggingTaskId: string | null
  onDragStart: (taskId: string) => void
  onDragEnd: () => void
  onAddTask: (title: string, description: string) => void
  onMoveTask: (taskId: string, targetColumnId: string, newOrder: number) => void
  onDeleteTask: (taskId: string) => void
  onRename: () => void
  onDelete: () => void
  onTaskClick: (taskId: string) => void
}

function KanbanColumnView({
  column, tasks, allColumns,
  draggingTaskId,
  onDragStart, onDragEnd,
  onAddTask, onMoveTask, onDeleteTask,
  onRename, onDelete, onTaskClick,
}: ColumnViewProps) {
  const [showAdd, setShowAdd] = useState(false)
  const [newTaskTitle, setNewTaskTitle] = useState('')
  const [newTaskDesc, setNewTaskDesc] = useState('')
  const [showColMenu, setShowColMenu] = useState(false)
  const [hoverGap, setHoverGap] = useState<number | null>(null)
  const tasksRef = useRef<HTMLDivElement>(null)

  // Hide the dragged card from this column's visible list so gaps close around it
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

  const handleAddTask = async () => {
    if (!newTaskTitle.trim()) return
    await onAddTask(newTaskTitle.trim(), newTaskDesc.trim())
    setNewTaskTitle('')
    setNewTaskDesc('')
    setShowAdd(false)
  }

  return (
    <div
      className={`shrink-0 w-64 flex flex-col gap-2 rounded-xl p-1 transition-colors ${
        isDragging && hoverGap !== null ? 'bg-indigo-500/5 ring-1 ring-[#4F7CFF]/30' : ''
      }`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      {/* Column header */}
      <div className="flex items-center justify-between px-1">
        <div className="flex items-center gap-2">
          <span className="font-medium text-sm text-slate-200">{column.title}</span>
          <span className="text-xs text-slate-500 bg-surface-high px-1.5 py-0.5 rounded-full">{tasks.length}</span>
        </div>
        <div className="relative">
          <button
            onClick={() => setShowColMenu((v) => !v)}
            className="text-slate-500 hover:text-white p-1 rounded transition-colors text-lg leading-none"
          >
            ⋯
          </button>
          {showColMenu && (
            <div className="absolute right-0 top-full mt-1 bg-surface-high border border-white/10 rounded-lg shadow-xl z-20 min-w-32 py-1">
              <button
                onClick={() => { onRename(); setShowColMenu(false) }}
                className="w-full text-left px-4 py-2 text-sm text-slate-300 hover:bg-white/5 hover:text-white transition-colors"
              >
                Rename
              </button>
              <button
                onClick={() => { onDelete(); setShowColMenu(false) }}
                className="w-full text-left px-4 py-2 text-sm text-red-400 hover:bg-white/5 transition-colors"
              >
                Delete column
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Tasks with inline drop indicators */}
      <div ref={tasksRef} className="flex flex-col gap-2">
        {visibleTasks.map((task, i) => (
          <div key={task.id}>
            {isDragging && hoverGap === i && <DropIndicator />}
            <TaskCardView
              task={task}
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
          <div className="h-16 rounded-lg border-2 border-dashed border-[#4F7CFF]/25 flex items-center justify-center text-xs text-[#4F7CFF]/40">
            Drop here
          </div>
        )}
      </div>

      {/* Add task */}
      {showAdd ? (
        <div className="bg-surface-raised rounded-xl p-3 mt-1">
          <input
            autoFocus
            value={newTaskTitle}
            onChange={(e) => setNewTaskTitle(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Escape') { setShowAdd(false); setNewTaskTitle(''); setNewTaskDesc('') } }}
            placeholder="Task title"
            className="w-full bg-surface-high border border-white/10 rounded-lg px-3 py-2 text-white placeholder-slate-500 outline-none focus:border-indigo-500 text-sm mb-2"
          />
          <textarea
            value={newTaskDesc}
            onChange={(e) => setNewTaskDesc(e.target.value)}
            placeholder="Description (optional)"
            rows={2}
            className="w-full bg-surface-high border border-white/10 rounded-lg px-3 py-2 text-white placeholder-slate-500 outline-none focus:border-indigo-500 text-sm mb-2 resize-none"
          />
          <div className="flex gap-2">
            <button onClick={handleAddTask} className="px-3 py-1.5 bg-indigo-500 hover:bg-indigo-600 text-white rounded-lg text-sm font-medium transition-colors">Add</button>
            <button onClick={() => { setShowAdd(false); setNewTaskTitle(''); setNewTaskDesc('') }} className="px-3 py-1.5 text-slate-400 hover:text-white rounded-lg text-sm transition-colors">Cancel</button>
          </div>
        </div>
      ) : (
        <button
          onClick={() => setShowAdd(true)}
          className="w-full text-left text-slate-500 hover:text-slate-300 px-2 py-1.5 rounded-lg hover:bg-surface-raised transition-colors text-sm mt-1"
        >
          + Add task
        </button>
      )}
    </div>
  )
}

// ─── Task card ───────────────────────────────────────────────────────────────

interface TaskCardProps {
  task: Task
  allColumns: Column[]
  onDragStart: (taskId: string) => void
  onDragEnd: () => void
  onMove: (taskId: string, columnId: string) => void
  onDelete: (taskId: string) => void
  onClick: () => void
}

function TaskCardView({ task, allColumns, onDragStart, onDragEnd, onMove, onDelete, onClick }: TaskCardProps) {
  const [showMenu, setShowMenu] = useState(false)
  const [showMoveMenu, setShowMoveMenu] = useState(false)
  const otherColumns = allColumns.filter((c) => c.id !== task.columnId)

  const handleDragStart = (e: React.DragEvent) => {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('taskId', task.id)
    e.dataTransfer.setData('sourceColumnId', task.columnId)
    // Delay so browser captures ghost before card is hidden from source column
    setTimeout(() => onDragStart(task.id), 0)
  }

  return (
    <div
      data-task-card
      draggable
      onDragStart={handleDragStart}
      onDragEnd={onDragEnd}
      className="bg-surface-raised hover:bg-surface-high rounded-xl p-3 cursor-grab active:cursor-grabbing transition-colors relative group select-none"
      onClick={onClick}
    >
      <p className="text-sm text-white leading-snug">{task.title}</p>
      {task.description && (
        <p className="text-xs text-slate-500 mt-1 line-clamp-2">{task.description}</p>
      )}

      {/* Card action menu */}
      <div
        className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={() => { setShowMenu((v) => !v); setShowMoveMenu(false) }}
          className="text-slate-500 hover:text-white p-1 rounded text-sm leading-none"
        >
          ⋯
        </button>

        {showMenu && (
          <div className="absolute right-0 top-full mt-1 bg-surface-high border border-white/10 rounded-lg shadow-xl z-20 min-w-36 py-1">
            {otherColumns.length > 0 && (
              <div className="relative">
                <button
                  onClick={() => setShowMoveMenu((v) => !v)}
                  className="w-full text-left px-4 py-2 text-sm text-slate-300 hover:bg-white/5 transition-colors flex items-center justify-between"
                >
                  Move to <span className="text-slate-500">›</span>
                </button>
                {showMoveMenu && (
                  <div className="absolute left-full top-0 ml-1 bg-surface-high border border-white/10 rounded-lg shadow-xl z-30 min-w-36 py-1">
                    {otherColumns.map((col) => (
                      <button
                        key={col.id}
                        onClick={() => { onMove(task.id, col.id); setShowMenu(false); setShowMoveMenu(false) }}
                        className="w-full text-left px-4 py-2 text-sm text-slate-300 hover:bg-white/5 transition-colors"
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
              className="w-full text-left px-4 py-2 text-sm text-red-400 hover:bg-white/5 transition-colors"
            >
              Delete
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
