import { useState } from 'react'
import { Subtask } from '../types'
import TaskEditor, { orderBetween } from './TaskEditor'

export interface NewTaskDraft {
  title: string
  description: string
  reminderTimeMillis: number | null
  reminderStyle: 'ALARM' | 'NOTIFICATION'
  subtasks: { title: string; isCompleted: boolean }[]
}

interface NewTaskModalProps {
  onCreate: (draft: NewTaskDraft) => Promise<unknown>
  onClose: () => void
}

/**
 * Editor for a task that does not exist yet. Nothing is written to Firestore
 * until "Add task" is pressed, so opening and dismissing the dialog leaves no
 * empty task behind. Checklist items and the reminder are held in local state
 * and persisted alongside the task on submit.
 */
export default function NewTaskModal({ onCreate, onClose }: NewTaskModalProps) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [reminderTimeMillis, setReminderTimeMillis] = useState<number | null>(null)
  const [reminderStyle, setReminderStyle] = useState<'ALARM' | 'NOTIFICATION'>('ALARM')
  const [subtasks, setSubtasks] = useState<Subtask[]>([])
  const [submitting, setSubmitting] = useState(false)

  const canSubmit = title.trim().length > 0 && !submitting
  const isEmpty = !title.trim() && !description.trim() && subtasks.length === 0 && reminderTimeMillis == null

  const addSubtask = (subtaskTitle: string) => {
    setSubtasks((prev) => [
      ...prev,
      {
        id: crypto.randomUUID(),
        taskId: '',
        title: subtaskTitle,
        isCompleted: false,
        order: prev.length > 0 ? Math.max(...prev.map((s) => s.order)) + 1 : 1,
        updatedAt: Date.now(),
        isDeleted: false,
      },
    ])
  }

  const patchSubtask = (subtaskId: string, fields: Partial<Subtask>) => {
    setSubtasks((prev) => prev.map((s) => (s.id === subtaskId ? { ...s, ...fields } : s)))
  }

  const handleClose = () => {
    if (!isEmpty && !window.confirm('Discard this task?')) return
    onClose()
  }

  const handleCreate = async () => {
    if (!canSubmit) return
    setSubmitting(true)
    try {
      await onCreate({
        title: title.trim(),
        description: description.trim(),
        reminderTimeMillis,
        reminderStyle,
        subtasks: [...subtasks]
          .sort((a, b) => a.order - b.order)
          .map((s) => ({ title: s.title, isCompleted: s.isCompleted })),
      })
      onClose()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <TaskEditor
      title={title}
      description={description}
      onTitleChange={setTitle}
      onDescriptionChange={setDescription}
      autoFocusTitle
      reminderTimeMillis={reminderTimeMillis}
      reminderStyle={reminderStyle}
      onSaveReminder={(millis, style) => { setReminderTimeMillis(millis); setReminderStyle(style) }}
      onClearReminder={() => setReminderTimeMillis(null)}
      subtasks={subtasks}
      onAddSubtask={addSubtask}
      onToggleSubtask={(id, isCompleted) => patchSubtask(id, { isCompleted })}
      onDeleteSubtask={(id) => setSubtasks((prev) => prev.filter((s) => s.id !== id))}
      onRenameSubtask={(id, newTitle) => patchSubtask(id, { title: newTitle })}
      onReorderSubtask={(id, orderBefore, orderAfter) => patchSubtask(id, { order: orderBetween(orderBefore, orderAfter) })}
      headerStatus={<span className="text-sm font-bold text-fg mr-auto pl-2">New task</span>}
      footer={
        <div className="flex justify-end gap-2 border-t border-DEFAULT pt-4">
          <button
            onClick={handleClose}
            className="px-4 py-2 text-fg-muted hover:text-fg text-sm rounded-xl font-semibold transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleCreate}
            disabled={!canSubmit}
            className="px-4 py-2 bg-accent hover:bg-accent-hover disabled:opacity-40 text-accent-fg text-sm rounded-xl font-semibold transition-colors"
          >
            {submitting ? 'Adding…' : 'Add task'}
          </button>
        </div>
      }
      onClose={handleClose}
    />
  )
}
