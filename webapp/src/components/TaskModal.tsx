import { useRef, useState, useEffect } from 'react'
import { Check, X } from 'lucide-react'
import { useTask } from '../hooks/useTask'
import { Subtask } from '../types'

interface TaskModalProps {
  userId: string
  boardId: string
  taskId: string
  isNew: boolean
  onClose: () => void
  onDiscard: () => void
}

export default function TaskModal({ userId, boardId, taskId, isNew, onClose, onDiscard }: TaskModalProps) {
  const { task, subtasks, updateTask, addSubtask, toggleSubtask, deleteSubtask, renameSubtask, reorderSubtask } = useTask(
    userId, boardId, taskId,
  )

  const [titleDraft, setTitleDraft] = useState('')
  const [descDraft, setDescDraft] = useState('')
  const [saveStatus, setSaveStatus] = useState<'idle' | 'saving' | 'saved'>('idle')
  const [newSubtaskTitle, setNewSubtaskTitle] = useState('')
  const [draggingSubtaskId, setDraggingSubtaskId] = useState<string | null>(null)
  const [hoverGap, setHoverGap] = useState<number | null>(null)
  const subtaskListRef = useRef<HTMLDivElement>(null)
  const titleRef = useRef<HTMLTextAreaElement>(null)
  const saveTimer = useRef<ReturnType<typeof setTimeout>>()
  const hydratedTaskId = useRef<string | null>(null)

  // Hydrate drafts once per task; remote echoes of our own autosaves must not clobber the editor.
  useEffect(() => {
    if (task && hydratedTaskId.current !== task.id) {
      setTitleDraft(task.title)
      setDescDraft(task.description)
      hydratedTaskId.current = task.id
    }
  }, [task])

  useEffect(() => {
    if (isNew) titleRef.current?.focus()
  }, [isNew])

  useEffect(() => () => { if (saveTimer.current) clearTimeout(saveTimer.current) }, [])

  const scheduleSave = (title: string, desc: string) => {
    if (saveTimer.current) clearTimeout(saveTimer.current)
    if (!title.trim()) {
      setSaveStatus('idle')
      return
    }
    setSaveStatus('saving')
    saveTimer.current = setTimeout(async () => {
      await updateTask({ title: title.trim(), description: desc.trim() })
      setSaveStatus('saved')
    }, 600)
  }

  const flushSave = async () => {
    if (saveTimer.current) clearTimeout(saveTimer.current)
    if (!titleDraft.trim()) return
    await updateTask({ title: titleDraft.trim(), description: descDraft.trim() })
  }

  const handleClose = async () => {
    if (saveTimer.current) clearTimeout(saveTimer.current)
    const empty = !titleDraft.trim() && !descDraft.trim() && subtasks.length === 0
    if (isNew && empty) {
      onDiscard()
    } else {
      await flushSave()
    }
    onClose()
  }

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') handleClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  })

  const handleAddSubtask = async () => {
    if (!newSubtaskTitle.trim()) return
    await addSubtask(newSubtaskTitle.trim())
    setNewSubtaskTitle('')
  }

  const sortedSubtasks = [...subtasks].sort(
    (a, b) => (a.isCompleted ? 1 : 0) - (b.isCompleted ? 1 : 0) || a.order - b.order,
  )
  const visibleSubtasks = draggingSubtaskId
    ? sortedSubtasks.filter((s) => s.id !== draggingSubtaskId)
    : sortedSubtasks

  const getGapFromEvent = (e: React.DragEvent): number => {
    const rows = subtaskListRef.current?.querySelectorAll('[data-subtask-row]') ?? []
    for (let i = 0; i < rows.length; i++) {
      const rect = rows[i].getBoundingClientRect()
      if (e.clientY < rect.top + rect.height / 2) return i
    }
    return rows.length
  }

  const handleDragOver = (e: React.DragEvent) => {
    if (!draggingSubtaskId) return
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    setHoverGap(getGapFromEvent(e))
  }

  const handleDragLeave = (e: React.DragEvent) => {
    if (!e.currentTarget.contains(e.relatedTarget as Node)) setHoverGap(null)
  }

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault()
    const subtaskId = e.dataTransfer.getData('subtaskId')
    const gap = getGapFromEvent(e)
    setHoverGap(null)
    setDraggingSubtaskId(null)
    if (!subtaskId) return
    const prev = visibleSubtasks[gap - 1]
    const next = visibleSubtasks[gap]
    const orderBefore = prev?.order ?? (next ? next.order - 2 : 0)
    const orderAfter = next?.order ?? orderBefore + 2
    await reorderSubtask(subtaskId, orderBefore, orderAfter)
  }

  const completedCount = subtasks.filter((s) => s.isCompleted).length

  return (
    <div
      className="fixed inset-0 bg-black/60 z-50 flex items-start justify-center p-4 overflow-y-auto"
      onClick={handleClose}
    >
      <div
        className="bg-surface-raised border border-DEFAULT rounded-2xl shadow-dialog w-full max-w-2xl mt-[6vh] mb-8"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-end gap-3 px-4 h-12 border-b border-DEFAULT">
          <span className="text-xs text-fg-faint transition-opacity">
            {saveStatus === 'saving' ? 'Saving…' : saveStatus === 'saved' ? 'Saved' : ''}
          </span>
          <button
            onClick={handleClose}
            className="text-fg-faint hover:text-fg transition-colors p-1 rounded-lg hover:bg-surface-high"
            aria-label="Close"
          >
            <X size={20} />
          </button>
        </div>

        <div className="px-6 py-5 flex flex-col gap-5">
          <textarea
            ref={titleRef}
            value={titleDraft}
            onChange={(e) => { const v = e.target.value; setTitleDraft(v); scheduleSave(v, descDraft) }}
            onBlur={flushSave}
            rows={1}
            placeholder="Task title"
            className="w-full bg-transparent text-fg text-xl font-bold leading-snug placeholder:text-fg-faint outline-none resize-none"
          />

          <textarea
            value={descDraft}
            onChange={(e) => { const v = e.target.value; setDescDraft(v); scheduleSave(titleDraft, v) }}
            onBlur={flushSave}
            rows={3}
            placeholder="Add a description…"
            className="w-full bg-surface border border-DEFAULT rounded-xl px-4 py-3 text-fg text-sm placeholder:text-fg-faint outline-none focus:border-accent transition-colors resize-none"
          />

          <hr className="border-DEFAULT" />

          <div>
            <div className="flex items-center justify-between mb-3">
              <span className="text-sm font-bold text-fg">Checklist</span>
              {subtasks.length > 0 && (
                <span className="text-xs text-fg-muted font-mono">
                  {completedCount}/{subtasks.length}
                </span>
              )}
            </div>

            {subtasks.length > 0 && (
              <div className="mb-3 h-1 bg-surface-high rounded-full overflow-hidden">
                <div
                  className="h-full bg-accent rounded-full transition-all"
                  style={{ width: `${(completedCount / subtasks.length) * 100}%` }}
                />
              </div>
            )}

            <div
              ref={subtaskListRef}
              className="flex flex-col"
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
            >
              {visibleSubtasks.map((s, i) => (
                <div key={s.id}>
                  {draggingSubtaskId && hoverGap === i && <SubtaskDropIndicator />}
                  <SubtaskRow
                    subtask={s}
                    onToggle={() => toggleSubtask(s.id, !s.isCompleted)}
                    onDelete={() => deleteSubtask(s.id)}
                    onRename={(title) => renameSubtask(s.id, title)}
                    onDragStart={(e) => {
                      e.dataTransfer.effectAllowed = 'move'
                      e.dataTransfer.setData('subtaskId', s.id)
                      setTimeout(() => setDraggingSubtaskId(s.id), 0)
                    }}
                    onDragEnd={() => { setDraggingSubtaskId(null); setHoverGap(null) }}
                  />
                </div>
              ))}
              {draggingSubtaskId && hoverGap === visibleSubtasks.length && <SubtaskDropIndicator />}
            </div>

            <div className="flex gap-2 mt-3">
              <input
                value={newSubtaskTitle}
                onChange={(e) => setNewSubtaskTitle(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') handleAddSubtask() }}
                placeholder="Add checklist item…"
                className="flex-1 bg-surface border border-DEFAULT rounded-lg px-3 py-2 text-fg placeholder:text-fg-faint outline-none focus:border-accent text-sm transition-colors"
              />
              <button
                onClick={handleAddSubtask}
                disabled={!newSubtaskTitle.trim()}
                className="px-3 py-2 bg-surface-high hover:bg-surface-raised disabled:opacity-40 text-fg rounded-lg text-sm font-semibold transition-colors border border-DEFAULT"
              >
                Add
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function SubtaskDropIndicator() {
  return (
    <div className="py-0.5 pointer-events-none">
      <div className="h-[3px] w-full rounded-full bg-accent" />
    </div>
  )
}

interface SubtaskRowProps {
  subtask: Subtask
  onToggle: () => void
  onDelete: () => void
  onRename: (title: string) => void
  onDragStart: (e: React.DragEvent) => void
  onDragEnd: () => void
}

function SubtaskRow({ subtask, onToggle, onDelete, onRename, onDragStart, onDragEnd }: SubtaskRowProps) {
  const [editing, setEditing] = useState(false)
  const [editValue, setEditValue] = useState(subtask.title)
  const inputRef = useRef<HTMLInputElement>(null)

  const commitRename = () => {
    const trimmed = editValue.trim()
    if (trimmed && trimmed !== subtask.title) onRename(trimmed)
    else setEditValue(subtask.title)
    setEditing(false)
  }

  useEffect(() => {
    if (editing) inputRef.current?.focus()
  }, [editing])

  return (
    <div
      data-subtask-row
      draggable
      onDragStart={onDragStart}
      onDragEnd={onDragEnd}
      className={`flex items-center gap-3 group py-1 cursor-grab active:cursor-grabbing select-none ${
        subtask.isCompleted ? 'opacity-45' : ''
      }`}
    >
      <button
        onMouseDown={(e) => e.stopPropagation()}
        onClick={(e) => { e.stopPropagation(); onToggle() }}
        className={`w-5 h-5 rounded-md border flex items-center justify-center shrink-0 transition-colors ${
          subtask.isCompleted
            ? 'bg-accent border-accent text-accent-fg'
            : 'border-fg-muted hover:border-fg-faint'
        }`}
        style={subtask.isCompleted ? undefined : { borderWidth: '1.5px' }}
      >
        {subtask.isCompleted && <Check size={12} strokeWidth={3} />}
      </button>

      {editing ? (
        <input
          ref={inputRef}
          value={editValue}
          onChange={(e) => setEditValue(e.target.value)}
          onBlur={commitRename}
          onKeyDown={(e) => {
            if (e.key === 'Enter') commitRename()
            if (e.key === 'Escape') { setEditValue(subtask.title); setEditing(false) }
          }}
          onMouseDown={(e) => e.stopPropagation()}
          className="flex-1 bg-transparent border-b border-accent outline-none text-sm text-fg py-0.5"
        />
      ) : (
        <span
          onMouseDown={(e) => e.stopPropagation()}
          onClick={(e) => {
            e.stopPropagation()
            if (!subtask.isCompleted) { setEditValue(subtask.title); setEditing(true) }
          }}
          className={`flex-1 text-sm cursor-text ${
            subtask.isCompleted ? 'line-through text-fg-faint' : 'text-fg'
          }`}
        >
          {subtask.title}
        </span>
      )}

      <button
        onMouseDown={(e) => e.stopPropagation()}
        onClick={(e) => { e.stopPropagation(); onDelete() }}
        className="opacity-0 group-hover:opacity-100 text-fg-faint hover:text-danger-text transition-all p-0.5"
        aria-label="Remove"
      >
        <X size={14} />
      </button>
    </div>
  )
}
