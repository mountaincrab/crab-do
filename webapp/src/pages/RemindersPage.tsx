import { useState } from 'react'
import { Bell, AlarmClock, Repeat, Pencil, Trash2, ChevronRight } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useReminders } from '../hooks/useReminders'
import { Reminder, RecurringReminder } from '../types'
import AppHeader from '../components/AppHeader'

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
  const d = new Date(Date.now() + 3_600_000)
  d.setMinutes(0, 0, 0)
  return millisToDatetimeLocal(d.getTime())
}

function describeRecurrence(ruleJson: string, reminderTime: string): string {
  try {
    const rule = JSON.parse(ruleJson) as {
      type: 'DAILY' | 'WEEKLY' | 'EVERY_N_DAYS' | 'MONTHLY'
      interval?: number
      daysOfWeek?: number[]
      dayOfMonth?: number
    }
    const interval = rule.interval ?? 1
    const DAY_NAMES = ['', 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
    switch (rule.type) {
      case 'DAILY':
        return interval === 1 ? `Every day at ${reminderTime}` : `Every ${interval} days at ${reminderTime}`
      case 'EVERY_N_DAYS':
        return `Every ${interval} days at ${reminderTime}`
      case 'WEEKLY': {
        const days = (rule.daysOfWeek ?? []).map((d) => DAY_NAMES[d] ?? '?').join(', ')
        return interval === 1 ? `Every ${days} at ${reminderTime}` : `Every ${interval} weeks on ${days} at ${reminderTime}`
      }
      case 'MONTHLY': {
        const day = rule.dayOfMonth ?? 1
        const suffix =
          day >= 11 && day <= 13 ? 'th' : day % 10 === 1 ? 'st' : day % 10 === 2 ? 'nd' : day % 10 === 3 ? 'rd' : 'th'
        return `Monthly on the ${day}${suffix} at ${reminderTime}`
      }
      default:
        return reminderTime
    }
  } catch {
    return reminderTime
  }
}

// ── Toggle switch ─────────────────────────────────────────────────────────────

function EnableToggle({ enabled, onToggle }: { enabled: boolean; onToggle: () => void }) {
  return (
    <button
      onClick={onToggle}
      title={enabled ? 'Disable' : 'Enable'}
      className={`relative w-9 h-5 rounded-full transition-colors shrink-0 ${
        enabled ? 'bg-accent' : 'bg-fg-disabled'
      }`}
    >
      <span
        className={`absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform ${
          enabled ? 'translate-x-4' : 'translate-x-0.5'
        }`}
      />
    </button>
  )
}

// ── Reminder style icon tile ─────────────────────────────────────────────────

function StyleTile({ style }: { style: 'ALARM' | 'NOTIFICATION' | 'RECURRING' }) {
  if (style === 'ALARM') {
    return (
      <span className="w-7 h-7 rounded-lg flex items-center justify-center shrink-0 bg-[rgba(245,158,11,0.10)] text-warning">
        <AlarmClock size={16} />
      </span>
    )
  }
  if (style === 'RECURRING') {
    return (
      <span className="w-7 h-7 rounded-lg flex items-center justify-center shrink-0 bg-surface-high text-fg-muted">
        <Repeat size={16} />
      </span>
    )
  }
  return (
    <span className="w-7 h-7 rounded-lg flex items-center justify-center shrink-0 bg-accent-soft text-accent-text">
      <Bell size={16} />
    </span>
  )
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
    initial ? millisToDatetimeLocal(initial.scheduledAt) : defaultDatetimeLocal(),
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
      <div className="bg-surface-raised border border-DEFAULT rounded-2xl p-6 w-full max-w-sm shadow-dialog">
        <h2 className="text-lg font-bold mb-4 text-fg">{initial ? 'Edit Reminder' : 'New Reminder'}</h2>

        <div className="space-y-3 mb-5">
          <input
            autoFocus
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') handleSave() }}
            placeholder="Reminder title"
            className="w-full bg-surface-raised border border-DEFAULT rounded-lg px-3 py-2.5 text-fg placeholder:text-fg-faint outline-none focus:border-accent transition-colors"
          />

          <input
            type="datetime-local"
            value={datetime}
            onChange={(e) => setDatetime(e.target.value)}
            className="w-full bg-surface-raised border border-DEFAULT rounded-lg px-3 py-2.5 text-fg outline-none focus:border-accent transition-colors [color-scheme:dark]"
          />

          <div className="flex gap-2">
            {(['NOTIFICATION', 'ALARM'] as const).map((s) => (
              <button
                key={s}
                onClick={() => setStyle(s)}
                className={`flex-1 py-2 rounded-lg text-sm font-semibold border transition-colors flex items-center justify-center gap-1.5 ${
                  style === s
                    ? 'bg-accent border-accent text-accent-fg'
                    : 'border-DEFAULT text-fg-muted hover:text-fg'
                }`}
              >
                {s === 'NOTIFICATION' ? <Bell size={14} /> : <AlarmClock size={14} />}
                {s === 'NOTIFICATION' ? 'Notification' : 'Alarm'}
              </button>
            ))}
          </div>
        </div>

        <div className="flex justify-end gap-2">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-fg-muted hover:text-fg transition-colors text-sm font-semibold"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={saving || !title.trim() || !datetime}
            className="px-4 py-2 bg-accent hover:bg-accent-hover disabled:opacity-40 text-accent-fg rounded-xl text-sm font-semibold transition-colors"
          >
            {saving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  )
}

interface OneOffRowProps {
  reminder: Reminder
  onEdit: () => void
  onDelete: () => void
  dimmed?: boolean
}

function OneOffReminderRow({ reminder, onEdit, onDelete, dimmed }: OneOffRowProps) {
  const now = Date.now()
  const isSnoozed = reminder.snoozedUntilMillis != null && reminder.snoozedUntilMillis > now
  const isPast = !isSnoozed && reminder.scheduledAt < now

  return (
    <div className={`bg-surface-raised border border-transparent hover:border-DEFAULT rounded-xl px-3 py-2.5 flex items-center gap-3 group transition-colors ${dimmed ? 'opacity-50' : ''}`}>
      <StyleTile style={reminder.reminderStyle === 'ALARM' ? 'ALARM' : 'NOTIFICATION'} />
      <div className="flex-1 min-w-0">
        <p className="font-semibold truncate text-fg">{reminder.title}</p>
        {isSnoozed ? (
          <p className="text-xs mt-0.5 text-success-text font-medium font-mono">
            Snoozing until {new Date(reminder.snoozedUntilMillis!).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </p>
        ) : (
          <p className={`text-xs mt-0.5 font-mono ${isPast ? 'text-danger-text' : 'text-fg-muted'}`}>
            {formatTriggerTime(reminder.scheduledAt)}
          </p>
        )}
      </div>
      <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          onClick={onEdit}
          className="p-1.5 rounded-lg text-fg-muted hover:text-fg hover:bg-surface-high transition-colors"
          title="Edit"
        >
          <Pencil size={14} />
        </button>
        <button
          onClick={onDelete}
          className="p-1.5 rounded-lg text-fg-muted hover:text-danger-text hover:bg-surface-high transition-colors"
          title="Delete"
        >
          <Trash2 size={14} />
        </button>
      </div>
    </div>
  )
}

interface RecurringRowProps {
  reminder: RecurringReminder
  onDelete: () => void
  onToggleEnabled: () => void
  dimmed?: boolean
}

function RecurringReminderRow({ reminder, onDelete, onToggleEnabled, dimmed }: RecurringRowProps) {
  const now = Date.now()
  const isSnoozed = reminder.snoozedUntilMillis != null && reminder.snoozedUntilMillis > now
  const recurrenceDesc = describeRecurrence(reminder.recurrenceRuleJson, reminder.reminderTime)

  return (
    <div className={`bg-surface-raised border border-transparent hover:border-DEFAULT rounded-xl px-3 py-2.5 flex items-center gap-3 group transition-colors ${dimmed ? 'opacity-50' : ''}`}>
      <StyleTile style="RECURRING" />
      <div className="flex-1 min-w-0">
        <p className={`font-semibold truncate ${reminder.isEnabled ? 'text-fg' : 'text-fg-faint'}`}>
          {reminder.title}
        </p>
        <p className="text-xs mt-0.5 text-fg-muted truncate font-mono">{recurrenceDesc}</p>
        {isSnoozed ? (
          <p className="text-xs mt-0.5 text-success-text font-medium font-mono">
            Snoozing until {new Date(reminder.snoozedUntilMillis!).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </p>
        ) : (
          <p className="text-xs mt-0.5 text-accent-text font-medium font-mono">
            Next: {formatTriggerTime(reminder.nextFireAt)}
          </p>
        )}
      </div>
      <EnableToggle enabled={reminder.isEnabled} onToggle={onToggleEnabled} />
      <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          onClick={onDelete}
          className="p-1.5 rounded-lg text-fg-muted hover:text-danger-text hover:bg-surface-high transition-colors"
          title="Delete"
        >
          <Trash2 size={14} />
        </button>
      </div>
    </div>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

type ActiveEntry =
  | { kind: 'one-off'; sortKey: number; data: Reminder }
  | { kind: 'recurring'; sortKey: number; data: RecurringReminder }

export default function RemindersPage() {
  const { user } = useAuth()
  const {
    reminders,
    completedReminders,
    recurringReminders,
    loading,
    createReminder,
    updateReminder,
    deleteReminder,
    deleteRecurringReminder,
    toggleRecurringEnabled,
  } = useReminders(user!.uid)

  const [showCreate, setShowCreate] = useState(false)
  const [editing, setEditing] = useState<Reminder | null>(null)
  const [showCompleted, setShowCompleted] = useState(false)

  const activeEntries: ActiveEntry[] = [
    ...reminders.map((r) => ({
      kind: 'one-off' as const,
      sortKey: r.snoozedUntilMillis ?? r.scheduledAt,
      data: r,
    })),
    ...recurringReminders.map((r) => ({
      kind: 'recurring' as const,
      sortKey: r.snoozedUntilMillis ?? r.nextFireAt,
      data: r,
    })),
  ].sort((a, b) => a.sortKey - b.sortKey)

  const isEmpty = activeEntries.length === 0 && completedReminders.length === 0

  return (
    <div className="min-h-screen bg-bg text-fg">
      <AppHeader active="reminders" />

      <main className="max-w-2xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-extrabold tracking-tightish">Reminders</h1>
          <button
            onClick={() => setShowCreate(true)}
            className="bg-accent hover:bg-accent-hover text-accent-fg px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
          >
            + New reminder
          </button>
        </div>

        {loading ? (
          <div className="text-fg-faint text-center py-20">Loading…</div>
        ) : isEmpty ? (
          <div className="text-fg-muted text-center py-20">
            <Bell size={40} className="mx-auto mb-4 text-fg-faint" />
            <p>No reminders yet. Create your first one.</p>
          </div>
        ) : (
          <div className="space-y-1.5">
            {activeEntries.length === 0 ? (
              <p className="text-fg-faint text-center py-8">No upcoming reminders.</p>
            ) : (
              activeEntries.map((entry) =>
                entry.kind === 'one-off' ? (
                  <OneOffReminderRow
                    key={entry.data.id}
                    reminder={entry.data}
                    onEdit={() => setEditing(entry.data)}
                    onDelete={() => deleteReminder(entry.data.id)}
                  />
                ) : (
                  <RecurringReminderRow
                    key={entry.data.id}
                    reminder={entry.data}
                    onDelete={() => deleteRecurringReminder(entry.data.id)}
                    onToggleEnabled={() => toggleRecurringEnabled(entry.data)}
                  />
                ),
              )
            )}

            {completedReminders.length > 0 && (
              <div className="pt-4">
                <button
                  onClick={() => setShowCompleted((v) => !v)}
                  className="text-sm text-fg-faint hover:text-fg-muted transition-colors flex items-center gap-1 mb-2 font-semibold"
                >
                  <ChevronRight
                    size={14}
                    className={`transition-transform ${showCompleted ? 'rotate-90' : ''}`}
                  />
                  Completed ({completedReminders.length})
                </button>
                {showCompleted && (
                  <div className="space-y-1.5">
                    {completedReminders.map((r) => (
                      <OneOffReminderRow
                        key={r.id}
                        reminder={r}
                        onEdit={() => setEditing(r)}
                        onDelete={() => deleteReminder(r.id)}
                        dimmed
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
