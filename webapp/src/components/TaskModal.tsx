import { useRef, useState, useEffect } from 'react'
import { useTask } from '../hooks/useTask'
import TaskEditor from './TaskEditor'

interface TaskModalProps {
  userId: string
  boardId: string
  taskId: string
  onClose: () => void
}

/** Editor for an existing task: every change autosaves, there is no Save button. */
export default function TaskModal({ userId, boardId, taskId, onClose }: TaskModalProps) {
  const { task, subtasks, updateTask, addSubtask, toggleSubtask, deleteSubtask, renameSubtask, reorderSubtask } = useTask(
    userId, boardId, taskId,
  )

  const [titleDraft, setTitleDraft] = useState('')
  const [descDraft, setDescDraft] = useState('')
  const [saveStatus, setSaveStatus] = useState<'idle' | 'saving' | 'saved'>('idle')
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
    await flushSave()
    onClose()
  }

  return (
    <TaskEditor
      title={titleDraft}
      description={descDraft}
      onTitleChange={(v) => { setTitleDraft(v); scheduleSave(v, descDraft) }}
      onDescriptionChange={(v) => { setDescDraft(v); scheduleSave(titleDraft, v) }}
      onFieldBlur={flushSave}
      reminderTimeMillis={task?.reminderTimeMillis ?? null}
      reminderStyle={task?.reminderStyle ?? 'ALARM'}
      onSaveReminder={(millis, style) => updateTask({ reminderTimeMillis: millis, reminderStyle: style })}
      onClearReminder={() => updateTask({ reminderTimeMillis: null })}
      subtasks={subtasks}
      onAddSubtask={addSubtask}
      onToggleSubtask={toggleSubtask}
      onDeleteSubtask={deleteSubtask}
      onRenameSubtask={renameSubtask}
      onReorderSubtask={reorderSubtask}
      headerStatus={
        <span className="text-xs text-fg-faint transition-opacity">
          {saveStatus === 'saving' ? 'Saving…' : saveStatus === 'saved' ? 'Saved' : ''}
        </span>
      }
      onClose={handleClose}
    />
  )
}
