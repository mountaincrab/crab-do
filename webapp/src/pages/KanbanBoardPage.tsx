import { useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, ChevronRight, GripVertical, MoreHorizontal } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useBoard } from '../hooks/useBoard'
import { Column, Task } from '../types'

export default function KanbanBoardPage() {
  const { boardId } = useParams<{ boardId: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()
  const {
    board, columns, tasksByColumn, loading,
    addColumn, renameColumn, deleteColumn, moveColumn,
    addTask, moveTask, deleteTask,
  } = useBoard(user!.uid, boardId!)

  const [showAddColumn, setShowAddColumn] = useState(false)
  const [newColTitle, setNewColTitle] = useState('')
  const [renamingCol, setRenamingCol] = useState<Column | null>(null)
  const [renameValue, setRenameValue] = useState('')
  const [draggingTaskId, setDraggingTaskId] = useState<string | null>(null)
  const [draggingColId, setDraggingColId] = useState<string | null>(null)
  const [hoverColGap, setHoverColGap] = useState<number | null>(null)
  const colsContainerRef = useRef<HTMLDivElement>(null)

  if (loading) {
    return (
      <div className="min-h-screen bg-bg flex items-center justify-center text-fg-faint">
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

  const getColGapFromEvent = (e: React.DragEvent): number => {
    const colEls = colsContainerRef.current?.querySelectorAll('[data-column-card]') ?? []
    for (let i = 0; i < colEls.length; i++) {
      const rect = colEls[i].getBoundingClientRect()
      if (e.clientX < rect.left + rect.width / 2) return i
    }
    return colEls.length
  }

  const orderForColGap = (gap: number): number => {
    const prev = columns[gap - 1]
    const next = columns[gap]
    if (!prev && !next) return 1
    if (!prev) return next.order - 1
    if (!next) return prev.order + 1
    return (prev.order + next.order) / 2
  }

  const handleColDragOver = (e: React.DragEvent) => {
    if (!e.dataTransfer.types.includes('columnid')) return
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    setHoverColGap(getColGapFromEvent(e))
  }

  const handleColDragLeave = (e: React.DragEvent) => {
    if (!colsContainerRef.current?.contains(e.relatedTarget as Node)) {
      setHoverColGap(null)
    }
  }

  const handleColDrop = (e: React.DragEvent) => {
    e.preventDefault()
    const columnId = e.dataTransfer.getData('columnId')
    if (columnId && hoverColGap !== null) {
      moveColumn(columnId, orderForColGap(hoverColGap))
    }
    setDraggingColId(null)
    setHoverColGap(null)
  }

  return (
    <div className="min-h-screen bg-bg text-fg flex flex-col">
      <header className="border-b border-DEFAULT bg-surface px-6 py-3 flex items-center gap-4 shrink-0">
        <button
          onClick={() => navigate('/')}
          className="flex items-center gap-1 text-fg-muted hover:text-fg transition-colors text-sm font-semibold"
          aria-label="Back"
        >
          <ArrowLeft size={16} /> Back
        </button>
        <h1 className="font-bold text-lg tracking-tightish">{board?.title ?? '…'}</h1>
      </header>

      <div className="flex-1 overflow-x-auto">
        <div
          ref={colsContainerRef}
          className="flex gap-4 p-6 h-full items-start"
          style={{ minHeight: 'calc(100vh - 57px)' }}
          onDragOver={handleColDragOver}
          onDragLeave={handleColDragLeave}
          onDrop={handleColDrop}
        >
          {columns.map((col, i) => (
            <>
              {draggingColId && hoverColGap === i && <ColDropIndicator key={`gap-${i}`} />}
              <KanbanColumnView
                key={col.id}
                column={col}
                tasks={tasksByColumn[col.id] ?? []}
                allColumns={columns}
                draggingTaskId={draggingTaskId}
                isDraggingThis={col.id === draggingColId}
                onDragStart={(taskId) => setDraggingTaskId(taskId)}
                onDragEnd={() => setDraggingTaskId(null)}
                onColDragStart={(colId) => setDraggingColId(colId)}
                onColDragEnd={() => { setDraggingColId(null); setHoverColGap(null) }}
                onAddTask={(title, desc) => addTask(col.id, title, desc)}
                onMoveTask={moveTask}
                onDeleteTask={deleteTask}
                onRename={() => { setRenamingCol(col); setRenameValue(col.title) }}
                onDelete={() => deleteColumn(col.id)}
                onTaskClick={(taskId) => navigate(`/board/${boardId}/task/${taskId}`)}
              />
            </>
          ))}
          {draggingColId && hoverColGap === columns.length && <ColDropIndicator key="gap-end" />}

          <div className="shrink-0 w-64">
            {showAddColumn ? (
              <div className="bg-surface-raised border border-DEFAULT rounded-xl p-3">
                <input
                  autoFocus
                  value={newColTitle}
                  onChange={(e) => setNewColTitle(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleAddColumn()
                    if (e.key === 'Escape') { setShowAddColumn(false); setNewColTitle('') }
                  }}
                  placeholder="Column name"
                  className="w-full bg-surface-raised border border-DEFAULT rounded-lg px-3 py-2 text-fg placeholder:text-fg-faint outline-none focus:border-accent text-sm mb-2 transition-colors"
                />
                <div className="flex gap-2">
                  <button
                    onClick={handleAddColumn}
                    className="px-3 py-1.5 bg-accent hover:bg-accent-hover text-accent-fg rounded-xl text-sm font-semibold transition-colors"
                  >
                    Add
                  </button>
                  <button
                    onClick={() => { setShowAddColumn(false); setNewColTitle('') }}
                    className="px-3 py-1.5 text-fg-muted hover:text-fg rounded-xl text-sm font-semibold transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <button
                onClick={() => setShowAddColumn(true)}
                className="w-full text-left text-fg-muted hover:text-fg px-3 py-2.5 rounded-xl hover:bg-surface-raised transition-colors text-sm font-semibold"
              >
                + Add column
              </button>
            )}
          </div>
        </div>
      </div>

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
    </div>
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

function ColDropIndicator() {
  return <div className="w-[3px] min-h-[100px] self-stretch rounded-full bg-accent shrink-0 pointer-events-none" />
}

interface ColumnViewProps {
  column: Column
  tasks: Task[]
  allColumns: Column[]
  draggingTaskId: string | null
  isDraggingThis: boolean
  onDragStart: (taskId: string) => void
  onDragEnd: () => void
  onColDragStart: (colId: string) => void
  onColDragEnd: () => void
  onAddTask: (title: string, description: string) => void
  onMoveTask: (taskId: string, targetColumnId: string, newOrder: number) => void
  onDeleteTask: (taskId: string) => void
  onRename: () => void
  onDelete: () => void
  onTaskClick: (taskId: string) => void
}

function KanbanColumnView({
  column, tasks, allColumns,
  draggingTaskId, isDraggingThis,
  onDragStart, onDragEnd,
  onColDragStart, onColDragEnd,
  onAddTask, onMoveTask, onDeleteTask,
  onRename, onDelete, onTaskClick,
}: ColumnViewProps) {
  const [showAdd, setShowAdd] = useState(false)
  const [newTaskTitle, setNewTaskTitle] = useState('')
  const [newTaskDesc, setNewTaskDesc] = useState('')
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
    if (!e.dataTransfer.types.includes('taskid')) return
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
    if (!e.dataTransfer.types.includes('taskid')) return
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
      data-column-card
      className={`shrink-0 w-64 flex flex-col gap-2 rounded-xl p-1 transition-colors group ${
        isDragging && hoverGap !== null ? 'bg-accent-soft ring-1 ring-accent/30' : ''
      } ${isDraggingThis ? 'opacity-30 pointer-events-none' : ''}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <div className="flex items-center justify-between px-1">
        <div className="flex items-center gap-1.5">
          <div
            draggable
            onDragStart={(e) => {
              e.stopPropagation()
              e.dataTransfer.effectAllowed = 'move'
              e.dataTransfer.setData('columnId', column.id)
              onColDragStart(column.id)
            }}
            onDragEnd={(e) => { e.stopPropagation(); onColDragEnd() }}
            className="opacity-0 group-hover:opacity-100 transition-opacity cursor-grab active:cursor-grabbing text-fg-faint hover:text-fg p-0.5 -ml-0.5 rounded"
            title="Drag to reorder column"
          >
            <GripVertical size={14} />
          </div>
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
          <div className="h-16 rounded-lg border-2 border-dashed border-accent/25 flex items-center justify-center text-xs text-accent/50">
            Drop here
          </div>
        )}
      </div>

      {showAdd ? (
        <div className="bg-surface-raised border border-DEFAULT rounded-xl p-3 mt-1">
          <input
            autoFocus
            value={newTaskTitle}
            onChange={(e) => setNewTaskTitle(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleAddTask()
              if (e.key === 'Escape') { setShowAdd(false); setNewTaskTitle(''); setNewTaskDesc('') }
            }}
            placeholder="Task title"
            className="w-full bg-surface-raised border border-DEFAULT rounded-lg px-3 py-2 text-fg placeholder:text-fg-faint outline-none focus:border-accent text-sm mb-2 transition-colors"
          />
          <textarea
            value={newTaskDesc}
            onChange={(e) => setNewTaskDesc(e.target.value)}
            placeholder="Description (optional)"
            rows={2}
            className="w-full bg-surface-raised border border-DEFAULT rounded-lg px-3 py-2 text-fg placeholder:text-fg-faint outline-none focus:border-accent text-sm mb-2 resize-none transition-colors"
          />
          <div className="flex gap-2">
            <button onClick={handleAddTask} className="px-3 py-1.5 bg-accent hover:bg-accent-hover text-accent-fg rounded-xl text-sm font-semibold transition-colors">Add</button>
            <button onClick={() => { setShowAdd(false); setNewTaskTitle(''); setNewTaskDesc('') }} className="px-3 py-1.5 text-fg-muted hover:text-fg rounded-xl text-sm font-semibold transition-colors">Cancel</button>
          </div>
        </div>
      ) : (
        <button
          onClick={() => setShowAdd(true)}
          className="w-full text-left text-fg-faint hover:text-fg px-2 py-1.5 rounded-lg hover:bg-surface-raised transition-colors text-sm font-semibold mt-1"
        >
          + Add task
        </button>
      )}
    </div>
  )
}

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
    e.stopPropagation()
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
      <p className="text-sm text-fg leading-snug font-medium">{task.title}</p>
      {task.description && (
        <p className="text-xs text-fg-faint mt-1 line-clamp-2">{task.description}</p>
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
