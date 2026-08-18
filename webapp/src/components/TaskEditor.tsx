import { ReactNode, useEffect, useRef, useState } from 'react'
import { AlarmClock, Bell, Check, Pencil, X } from 'lucide-react'
import { Subtask } from '../types'
import { Linkified, makeLinkPasteHandler, makeLinkKeyHandler } from '../lib/linkify'

export function millisToDatetimeLocal(ms: number): string {
  const d = new Date(ms)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function defaultReminderDatetimeLocal(): string {
  const d = new Date(Date.now() + 3_600_000)
  d.setMinutes(0, 0, 0)
  return millisToDatetimeLocal(d.getTime())
}

function formatReminderTime(ms: number): string {
  return new Date(ms).toLocaleString([], {
    weekday: 'short', day: 'numeric', month: 'short',
    hour: '2-digit', minute: '2-digit',
  })
}

/** Midpoint ordering, matching the other surfaces. */
export function orderBetween(orderBefore: number, orderAfter: number): number {
  return orderAfter <= orderBefore ? orderBefore + 1 : (orderBefore + orderAfter) / 2
}

interface TaskEditorProps {
  title: string
  description: string
  onTitleChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  /**
   * Called when Enter is pressed in the title field (Shift+Enter still inserts
   * a newline). Supplied by the create flow so the return key submits the task
   * the way the "Add task" button does; omit it to keep Enter as a newline.
   */
  onSubmitTitle?: () => void
  onFieldBlur?: () => void
  autoFocusTitle?: boolean
  reminderTimeMillis: number | null
  reminderStyle: 'ALARM' | 'NOTIFICATION'
  onSaveReminder: (millis: number, style: 'ALARM' | 'NOTIFICATION') => void
  onClearReminder: () => void
  subtasks: Subtask[]
  onAddSubtask: (title: string) => void
  onToggleSubtask: (subtaskId: string, isCompleted: boolean) => void
  onDeleteSubtask: (subtaskId: string) => void
  onRenameSubtask: (subtaskId: string, title: string) => void
  onReorderSubtask: (subtaskId: string, orderBefore: number, orderAfter: number) => void
  /** Rendered left of the close button (e.g. the autosave indicator). */
  headerStatus?: ReactNode
  /** Rendered under the checklist (e.g. Cancel / Add task for a new task). */
  footer?: ReactNode
  onClose: () => void
}

/**
 * Presentational task editor dialog. It owns no persistence: both the
 * autosaving editor (`TaskModal`) and the create-on-submit draft
 * (`NewTaskModal`) render this and supply the handlers.
 */
export default function TaskEditor({
  title, description, onTitleChange, onDescriptionChange, onSubmitTitle, onFieldBlur, autoFocusTitle,
  reminderTimeMillis, reminderStyle, onSaveReminder, onClearReminder,
  subtasks, onAddSubtask, onToggleSubtask, onDeleteSubtask, onRenameSubtask, onReorderSubtask,
  headerStatus, footer, onClose,
}: TaskEditorProps) {
  const [newSubtaskTitle, setNewSubtaskTitle] = useState('')
  const [editingReminder, setEditingReminder] = useState(false)
  const [reminderDraft, setReminderDraft] = useState(defaultReminderDatetimeLocal())
  const [reminderStyleDraft, setReminderStyleDraft] = useState<'ALARM' | 'NOTIFICATION'>('ALARM')
  const [draggingSubtaskId, setDraggingSubtaskId] = useState<string | null>(null)
  const [hoverGap, setHoverGap] = useState<number | null>(null)
  const subtaskListRef = useRef<HTMLDivElement>(null)
  const titleRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    if (autoFocusTitle) titleRef.current?.focus()
  }, [autoFocusTitle])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  })

  const openReminderEditor = () => {
    setReminderDraft(
      reminderTimeMillis != null
        ? millisToDatetimeLocal(reminderTimeMillis)
        : defaultReminderDatetimeLocal(),
    )
    setReminderStyleDraft(reminderStyle)
    setEditingReminder(true)
  }

  const saveReminder = () => {
    if (!reminderDraft) return
    onSaveReminder(new Date(reminderDraft).getTime(), reminderStyleDraft)
    setEditingReminder(false)
  }

  const clearReminder = () => {
    onClearReminder()
    setEditingReminder(false)
  }

  const handleAddSubtask = () => {
    if (!newSubtaskTitle.trim()) return
    onAddSubtask(newSubtaskTitle.trim())
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

  const handleDrop = (e: React.DragEvent) => {
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
    onReorderSubtask(subtaskId, orderBefore, orderAfter)
  }

  const completedCount = subtasks.filter((s) => s.isCompleted).length

  return (
    <div
      className="fixed inset-0 bg-black/60 z-50 flex items-start justify-center p-4 overflow-y-auto"
      onClick={onClose}
    >
      <div
        className="bg-surface-raised border border-DEFAULT rounded-2xl shadow-dialog w-full max-w-2xl mt-[6vh] mb-8"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-end gap-3 px-4 h-12 border-b border-DEFAULT">
          {headerStatus}
          <button
            onClick={onClose}
            className="text-fg-faint hover:text-fg transition-colors p-1 rounded-lg hover:bg-surface-high"
            aria-label="Close"
          >
            <X size={20} />
          </button>
        </div>

        <div className="px-6 py-5 flex flex-col gap-5">
          <textarea
            ref={titleRef}
            value={title}
            onChange={(e) => onTitleChange(e.target.value)}
            onPaste={makeLinkPasteHandler(title, onTitleChange)}
            onKeyDown={(e) => {
              makeLinkKeyHandler(title, onTitleChange)(e)
              if (e.defaultPrevented || !onSubmitTitle) return
              if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
                e.preventDefault()
                onSubmitTitle()
              }
            }}
            onBlur={onFieldBlur}
            rows={1}
            placeholder="Task title"
            className="w-full bg-transparent text-fg text-xl font-bold leading-snug placeholder:text-fg-faint outline-none resize-none"
          />

          <textarea
            value={description}
            onChange={(e) => onDescriptionChange(e.target.value)}
            onPaste={makeLinkPasteHandler(description, onDescriptionChange)}
            onKeyDown={makeLinkKeyHandler(description, onDescriptionChange)}
            onBlur={onFieldBlur}
            rows={3}
            placeholder="Add a description… (select text and paste a URL, or press ⌘K, to add a link)"
            className="w-full bg-surface border border-DEFAULT rounded-xl px-4 py-3 text-fg text-sm placeholder:text-fg-faint outline-none focus:border-accent transition-colors resize-none"
          />

          <hr className="border-DEFAULT" />

          <div>
            <span className="text-sm font-bold text-fg">Reminder</span>
            <div className="mt-3">
              {editingReminder ? (
                <div className="flex flex-col gap-3 bg-surface border border-DEFAULT rounded-xl p-3">
                  <input
                    type="datetime-local"
                    value={reminderDraft}
                    onChange={(e) => setReminderDraft(e.target.value)}
                    className="w-full bg-surface-raised border border-DEFAULT rounded-lg px-3 py-2 text-fg outline-none focus:border-accent transition-colors text-sm [color-scheme:dark]"
                  />
                  <div className="flex gap-2">
                    {(['NOTIFICATION', 'ALARM'] as const).map((s) => (
                      <button
                        key={s}
                        onClick={() => setReminderStyleDraft(s)}
                        className={`flex-1 py-2 rounded-lg text-sm font-semibold border transition-colors flex items-center justify-center gap-1.5 ${
                          reminderStyleDraft === s
                            ? 'bg-accent border-accent text-accent-fg'
                            : 'border-DEFAULT text-fg-muted hover:text-fg'
                        }`}
                      >
                        {s === 'NOTIFICATION' ? <Bell size={14} /> : <AlarmClock size={14} />}
                        {s === 'NOTIFICATION' ? 'Notification' : 'Alarm'}
                      </button>
                    ))}
                  </div>
                  <div className="flex justify-end gap-2">
                    {reminderTimeMillis != null && (
                      <button
                        onClick={clearReminder}
                        className="mr-auto px-3 py-1.5 rounded-lg text-danger-text hover:bg-surface-high text-sm font-semibold transition-colors"
                      >
                        Clear
                      </button>
                    )}
                    <button
                      onClick={() => setEditingReminder(false)}
                      className="px-3 py-1.5 rounded-lg text-fg-muted hover:text-fg text-sm font-semibold transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={saveReminder}
                      disabled={!reminderDraft}
                      className="px-3 py-1.5 bg-accent hover:bg-accent-hover disabled:opacity-40 text-accent-fg rounded-lg text-sm font-semibold transition-colors"
                    >
                      Save
                    </button>
                  </div>
                </div>
              ) : reminderTimeMillis != null ? (
                <div className="flex items-center gap-3 bg-surface border border-DEFAULT rounded-xl px-3 py-2.5">
                  <span className="w-7 h-7 rounded-lg flex items-center justify-center shrink-0 bg-accent-soft text-accent-text">
                    {reminderStyle === 'ALARM' ? <AlarmClock size={16} /> : <Bell size={16} />}
                  </span>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-fg font-medium font-mono">{formatReminderTime(reminderTimeMillis)}</p>
                    <p className="text-xs text-fg-muted mt-0.5">{reminderStyle === 'ALARM' ? 'Alarm' : 'Notification'}</p>
                  </div>
                  <button
                    onClick={openReminderEditor}
                    className="p-1.5 rounded-lg text-fg-muted hover:text-fg hover:bg-surface-high transition-colors"
                    title="Edit reminder"
                  >
                    <Pencil size={14} />
                  </button>
                  <button
                    onClick={clearReminder}
                    className="p-1.5 rounded-lg text-fg-muted hover:text-danger-text hover:bg-surface-high transition-colors"
                    title="Clear reminder"
                  >
                    <X size={14} />
                  </button>
                </div>
              ) : (
                <button
                  onClick={openReminderEditor}
                  className="flex items-center gap-2 text-fg-muted hover:text-fg px-3 py-2 rounded-lg border border-dashed border-DEFAULT hover:border-strong transition-colors text-sm font-semibold w-full"
                >
                  <Bell size={15} /> Set reminder
                </button>
              )}
            </div>
          </div>

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
                    onToggle={() => onToggleSubtask(s.id, !s.isCompleted)}
                    onDelete={() => onDeleteSubtask(s.id)}
                    onRename={(t) => onRenameSubtask(s.id, t)}
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
                onPaste={makeLinkPasteHandler(newSubtaskTitle, setNewSubtaskTitle)}
                onKeyDown={(e) => {
                  makeLinkKeyHandler(newSubtaskTitle, setNewSubtaskTitle)(e)
                  if (e.defaultPrevented || e.nativeEvent.isComposing) return
                  if (e.key === 'Enter') {
                    e.preventDefault()
                    handleAddSubtask()
                  }
                }}
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

          {footer}
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
  // The ⌘K link prompt blurs the field; committing on that blur would close the
  // editor and drop the link being inserted, so suppress the blur-commit while
  // the prompt is up (cleared a frame later, once focus has been restored).
  const linkingRef = useRef(false)

  const commitRename = () => {
    if (linkingRef.current) return
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
          onPaste={makeLinkPasteHandler(editValue, setEditValue)}
          onBlur={commitRename}
          onKeyDown={(e) => {
            if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
              linkingRef.current = true
              makeLinkKeyHandler(editValue, setEditValue)(e)
              requestAnimationFrame(() => { linkingRef.current = false })
              return
            }
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
          className={`flex-1 text-sm ${
            subtask.isCompleted ? 'line-through text-fg-faint' : 'text-fg'
          }`}
        >
          <Linkified text={subtask.title} />
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
