import { useRef, useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Check, X } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useTask } from '../hooks/useTask'
import { Subtask } from '../types'

export default function TaskDetailPage() {
  const { boardId, taskId } = useParams<{ boardId: string; taskId: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()
  const { task, subtasks, updateTask, addSubtask, toggleSubtask, deleteSubtask, renameSubtask, reorderSubtask } = useTask(
    user!.uid, boardId!, taskId!,
  )

  const [titleDraft, setTitleDraft] = useState('')
  const [descDraft, setDescDraft] = useState('')
  const [dirty, setDirty] = useState(false)
  const [newSubtaskTitle, setNewSubtaskTitle] = useState('')
  const [saving, setSaving] = useState(false)
  const [draggingSubtaskId, setDraggingSubtaskId] = useState<string | null>(null)
  const [hoverGap, setHoverGap] = useState<number | null>(null)
  const subtaskListRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (task && !dirty) {
      setTitleDraft(task.title)
      setDescDraft(task.description)
    }
  }, [task, dirty])

  const handleSave = async () => {
    if (!titleDraft.trim()) return
    setSaving(true)
    await updateTask({ title: titleDraft.trim(), description: descDraft.trim() })
    setDirty(false)
    setSaving(false)
  }

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
    <div className="min-h-screen bg-bg text-fg flex flex-col">
      <header className="border-b border-DEFAULT bg-surface px-6 py-3 flex items-center gap-4 shrink-0">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-1 text-fg-muted hover:text-fg transition-colors text-sm font-semibold"
        >
          <ArrowLeft size={16} /> Back
        </button>
        <span className="font-bold flex-1 truncate text-fg">{task?.title ?? '…'}</span>
      </header>

      <div className="flex-1 max-w-2xl mx-auto w-full px-6 py-8 flex flex-col gap-6">
        <div>
          <label className="ds-eyebrow mb-1.5 block">Title</label>
          <input
            value={titleDraft}
            onChange={(e) => { setTitleDraft(e.target.value); setDirty(true) }}
            className="w-full bg-surface-raised border border-DEFAULT rounded-xl px-4 py-3 text-fg outline-none focus:border-accent transition-colors"
          />
        </div>

        <div>
          <label className="ds-eyebrow mb-1.5 block">Description</label>
          <textarea
            value={descDraft}
            onChange={(e) => { setDescDraft(e.target.value); setDirty(true) }}
            rows={4}
            placeholder="Add a description…"
            className="w-full bg-surface-raised border border-DEFAULT rounded-xl px-4 py-3 text-fg placeholder:text-fg-faint outline-none focus:border-accent transition-colors resize-none"
          />
        </div>

        {dirty && (
          <div className="flex justify-end">
            <button
              onClick={handleSave}
              disabled={saving || !titleDraft.trim()}
              className="px-5 py-2 bg-accent hover:bg-accent-hover disabled:opacity-40 text-accent-fg rounded-xl text-sm font-semibold transition-colors"
            >
              {saving ? 'Saving…' : 'Save changes'}
            </button>
          </div>
        )}

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
              className="flex-1 bg-surface-raised border border-DEFAULT rounded-lg px-3 py-2 text-fg placeholder:text-fg-faint outline-none focus:border-accent text-sm transition-colors"
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
