import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useTask } from '../hooks/useTask'

export default function TaskDetailPage() {
  const { boardId, taskId } = useParams<{ boardId: string; taskId: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()
  const { task, subtasks, updateTask, addSubtask, toggleSubtask, deleteSubtask } = useTask(
    user!.uid, boardId!, taskId!,
  )

  const [titleDraft, setTitleDraft] = useState('')
  const [descDraft, setDescDraft] = useState('')
  const [dirty, setDirty] = useState(false)
  const [newSubtaskTitle, setNewSubtaskTitle] = useState('')
  const [saving, setSaving] = useState(false)

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

  const completedCount = subtasks.filter((s) => s.isCompleted).length

  return (
    <div className="min-h-screen bg-surface text-white flex flex-col">
      {/* Header */}
      <header className="border-b border-white/10 px-6 py-3 flex items-center gap-4 shrink-0">
        <button
          onClick={() => navigate(-1)}
          className="text-slate-400 hover:text-white transition-colors"
        >
          ← Back
        </button>
        <span className="font-semibold flex-1 truncate">{task?.title ?? '…'}</span>
      </header>

      <div className="flex-1 max-w-2xl mx-auto w-full px-6 py-8 flex flex-col gap-6">
        {/* Title */}
        <div>
          <label className="text-xs text-slate-500 uppercase tracking-wider mb-1.5 block">Title</label>
          <input
            value={titleDraft}
            onChange={(e) => { setTitleDraft(e.target.value); setDirty(true) }}
            className="w-full bg-surface-raised border border-white/10 rounded-xl px-4 py-3 text-white outline-none focus:border-indigo-500 transition-colors"
          />
        </div>

        {/* Description */}
        <div>
          <label className="text-xs text-slate-500 uppercase tracking-wider mb-1.5 block">Description</label>
          <textarea
            value={descDraft}
            onChange={(e) => { setDescDraft(e.target.value); setDirty(true) }}
            rows={4}
            placeholder="Add a description…"
            className="w-full bg-surface-raised border border-white/10 rounded-xl px-4 py-3 text-white placeholder-slate-600 outline-none focus:border-indigo-500 transition-colors resize-none"
          />
        </div>

        {/* Save button */}
        {dirty && (
          <div className="flex justify-end">
            <button
              onClick={handleSave}
              disabled={saving || !titleDraft.trim()}
              className="px-5 py-2 bg-indigo-500 hover:bg-indigo-600 disabled:opacity-40 text-white rounded-lg text-sm font-medium transition-colors"
            >
              {saving ? 'Saving…' : 'Save changes'}
            </button>
          </div>
        )}

        <hr className="border-white/10" />

        {/* Checklist */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <span className="text-sm font-medium text-slate-300">Checklist</span>
            {subtasks.length > 0 && (
              <span className="text-xs text-slate-500">
                {completedCount}/{subtasks.length}
              </span>
            )}
          </div>

          {subtasks.length > 0 && (
            <div className="mb-3 h-1 bg-surface-high rounded-full overflow-hidden">
              <div
                className="h-full bg-indigo-500 rounded-full transition-all"
                style={{ width: `${(completedCount / subtasks.length) * 100}%` }}
              />
            </div>
          )}

          <div className="flex flex-col gap-1">
            {subtasks.map((s) => (
              <div key={s.id} className="flex items-center gap-3 group py-1">
                <button
                  onClick={() => toggleSubtask(s.id, !s.isCompleted)}
                  className={`w-5 h-5 rounded border-2 flex items-center justify-center shrink-0 transition-colors ${
                    s.isCompleted
                      ? 'bg-indigo-500 border-indigo-500'
                      : 'border-slate-600 hover:border-slate-400'
                  }`}
                >
                  {s.isCompleted && (
                    <svg viewBox="0 0 10 8" className="w-3 h-3 fill-none stroke-white stroke-2">
                      <path d="M1 4l3 3 5-6" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  )}
                </button>
                <span className={`flex-1 text-sm ${s.isCompleted ? 'line-through text-slate-500' : 'text-slate-200'}`}>
                  {s.title}
                </span>
                <button
                  onClick={() => deleteSubtask(s.id)}
                  className="opacity-0 group-hover:opacity-100 text-slate-600 hover:text-red-400 transition-all text-xs px-1"
                  aria-label="Remove"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>

          {/* New subtask */}
          <div className="flex gap-2 mt-3">
            <input
              value={newSubtaskTitle}
              onChange={(e) => setNewSubtaskTitle(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') handleAddSubtask() }}
              placeholder="Add checklist item…"
              className="flex-1 bg-surface-raised border border-white/10 rounded-lg px-3 py-2 text-white placeholder-slate-600 outline-none focus:border-indigo-500 text-sm transition-colors"
            />
            <button
              onClick={handleAddSubtask}
              disabled={!newSubtaskTitle.trim()}
              className="px-3 py-2 bg-surface-high hover:bg-surface-raised disabled:opacity-40 text-slate-300 rounded-lg text-sm transition-colors border border-white/10"
            >
              Add
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
