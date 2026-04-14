import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useReminders } from '../hooks/useReminders'
import { Reminder } from '../types'

// ── Helpers ──────────────────────────────────────────────────────────────────

function formatTriggerTime(ms: number): string {
  const date = new Date(ms)
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const tomorrowStart = todayStart + 86_400_000
  const targetStart = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })

  if (targetStart === todayStart) return `Today at ${timeStr}`
  if (targetStart === tomorrowStart) return `Tomorrow at ${timeStr}`
  return (
    date.toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' }) +
    ` at ${timeStr}`
  )
}

function millisToDatetimeLocal(ms: number): string {
  const d = new Date(ms)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function defaultDatetimeLocal(): string {
  // default to next hour, rounded up
  const d = new Date(Date.now() + 3_600_000)
  d.setMinutes(0, 0, 0)
  return millisToDatetimeLocal(d.getTime())
}

// ── Sub-components ────────────────────────────────────────────────────────────

interface ReminderDialogProps {
  initial?: Reminder
  onSave: (title: string, triggerMs: number, style: 'ALARM' | 'NOTIFICATION') => Promise<void>
  onClose: () => void
}

function ReminderDialog({ initial, onSave, onClose }: ReminderDialogProps) {
  const [title, setTitle] = useState(initial?.title ?? '')
  const [datetime, setDatetime] = useState(
    initial ? millisToDatetimeLocal(initial.nextTriggerMillis) : defaultDatetimeLocal(),
  )
  const [style, setStyle] = useState<'ALARM' | 'NOTIFICATION'>(initial?.reminderStyle ?? 'NOTIFICATION')
  const [saving, setSaving] = useState(false)

  const handleSave = async () => {
    if (!title.trim() || !datetime) return
    setSaving(true)
    await onSave(title.trim(), new Date(datetime).getTime(), style)
    onClose()
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-surface-raised rounded-2xl p-6 w-full max-w-sm shadow-2xl">
        <h2 className="text-lg font-semibold mb-4">{initial ? 'Edit Reminder' : 'New Reminder'}</h2>

        <div className="space-y-3 mb-5">
          <input
            autoFocus
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') handleSave() }}
            placeholder="Reminder title"
            className="w-full bg-surface-high border border-white/10 rounded-lg px-3 py-2.5 text-white placeholder-slate-500 outline-none focus:border-indigo-500"
          />

          <input
            type="datetime-local"
            value={datetime}
            onChange={(e) => setDatetime(e.target.value)}
            className="w-full bg-surface-high border border-white/10 rounded-lg px-3 py-2.5 text-white outline-none focus:border-indigo-500 [color-scheme:dark]"
          />

          <div className="flex gap-3">
            {(['NOTIFICATION', 'ALARM'] as const).map((s) => (
              <button
                key={s}
                onClick={() => setStyle(s)}
                className={`flex-1 py-2 rounded-lg text-sm font-medium border transition-colors ${
                  style === s
                    ? 'bg-indigo-500 border-indigo-500 text-white'
                    : 'border-white/10 text-slate-400 hover:text-white'
                }`}
              >
                {s === 'NOTIFICATION' ? '🔔 Notification' : '⏰ Alarm'}
              </button>
            ))}
          </div>
        </div>

        <div className="flex justify-end gap-2">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-lg text-slate-400 hover:text-white transition-colors text-sm"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={saving || !title.trim() || !datetime}
            className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 disabled:opacity-40 text-white rounded-lg text-sm font-medium transition-colors"
          >
            {saving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  )
}

interface ReminderRowProps {
  reminder: Reminder
  onEdit: () => void
  onDelete: () => void
}

function ReminderRow({ reminder, onEdit, onDelete }: ReminderRowProps) {
  const isPast = reminder.nextTriggerMillis < Date.now()
  return (
    <div className="bg-surface-raised rounded-xl px-4 py-3 flex items-center gap-3 group">
      <div className="flex-1 min-w-0">
        <p className="font-medium text-white truncate">{reminder.title}</p>
        <p className={`text-sm mt-0.5 ${isPast ? 'text-red-400' : 'text-slate-400'}`}>
          {formatTriggerTime(reminder.nextTriggerMillis)}
        </p>
      </div>
      <span className="text-xs text-slate-500 shrink-0">
        {reminder.reminderStyle === 'ALARM' ? '⏰' : '🔔'}
      </span>
      <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          onClick={onEdit}
          className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-surface-high transition-colors"
          title="Edit"
        >
          <svg className="w-4 h-4" viewBox="0 0 20 20" fill="currentColor">
            <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
          </svg>
        </button>
        <button
          onClick={onDelete}
          className="p-1.5 rounded-lg text-slate-400 hover:text-red-400 hover:bg-surface-high transition-colors"
          title="Delete"
        >
          <svg className="w-4 h-4" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
        </button>
      </div>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function RemindersPage() {
  const { user, signOut } = useAuth()
  const { reminders, completedReminders, loading, createReminder, updateReminder, deleteReminder } =
    useReminders(user!.uid)

  const [showCreate, setShowCreate] = useState(false)
  const [editing, setEditing] = useState<Reminder | null>(null)
  const [showCompleted, setShowCompleted] = useState(false)

  return (
    <div className="min-h-screen bg-surface text-white">
      {/* Header */}
      <header className="border-b border-white/10 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <span className="text-xl">🦀</span>
            <span className="font-semibold text-lg">Crab Do</span>
          </div>
          <nav className="flex items-center gap-1 ml-2">
            <Link
              to="/"
              className="text-sm text-slate-400 hover:text-white transition-colors px-3 py-1.5 rounded-lg hover:bg-surface-high"
            >
              Boards
            </Link>
            <span className="text-sm text-white font-medium px-3 py-1.5 rounded-lg bg-surface-high">
              Reminders
            </span>
          </nav>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-slate-400 text-sm">{user?.email}</span>
          <button
            onClick={signOut}
            className="text-sm text-slate-400 hover:text-white transition-colors"
          >
            Sign out
          </button>
        </div>
      </header>

      <main className="max-w-2xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-2xl font-bold">Reminders</h1>
          <button
            onClick={() => setShowCreate(true)}
            className="bg-indigo-500 hover:bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
          >
            + New reminder
          </button>
        </div>

        {loading ? (
          <div className="text-slate-500 text-center py-20">Loading…</div>
        ) : reminders.length === 0 && completedReminders.length === 0 ? (
          <div className="text-slate-500 text-center py-20">
            <p className="text-4xl mb-4">🔔</p>
            <p>No reminders yet. Create your first one.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {reminders.length === 0 ? (
              <p className="text-slate-500 text-center py-8">No upcoming reminders.</p>
            ) : (
              reminders.map((r) => (
                <ReminderRow
                  key={r.id}
                  reminder={r}
                  onEdit={() => setEditing(r)}
                  onDelete={() => deleteReminder(r.id)}
                />
              ))
            )}

            {completedReminders.length > 0 && (
              <div className="pt-4">
                <button
                  onClick={() => setShowCompleted((v) => !v)}
                  className="text-sm text-slate-500 hover:text-slate-300 transition-colors flex items-center gap-1 mb-2"
                >
                  <svg
                    className={`w-3 h-3 transition-transform ${showCompleted ? 'rotate-90' : ''}`}
                    viewBox="0 0 20 20"
                    fill="currentColor"
                  >
                    <path
                      fillRule="evenodd"
                      d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"
                      clipRule="evenodd"
                    />
                  </svg>
                  Completed ({completedReminders.length})
                </button>
                {showCompleted && (
                  <div className="space-y-2 opacity-50">
                    {completedReminders.map((r) => (
                      <ReminderRow
                        key={r.id}
                        reminder={r}
                        onEdit={() => setEditing(r)}
                        onDelete={() => deleteReminder(r.id)}
                      />
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </main>

      {showCreate && (
        <ReminderDialog
          onSave={createReminder}
          onClose={() => setShowCreate(false)}
        />
      )}

      {editing && (
        <ReminderDialog
          initial={editing}
          onSave={(title, triggerMs, style) => updateReminder(editing.id, title, triggerMs, style)}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  )
}
