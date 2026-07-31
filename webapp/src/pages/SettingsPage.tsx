import { useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useApiKeys } from '../hooks/useApiKeys'
import { useTheme, THEMES, Theme } from '../contexts/ThemeContext'
import { ApiKeyMeta } from '../types'
import AppShell from '../components/AppShell'

function formatDate(ms: number): string {
  return new Date(ms).toLocaleDateString([], { day: 'numeric', month: 'short', year: 'numeric' })
}

function GenerateKeyDialog({
  onClose,
  onGenerate,
}: {
  onClose: () => void
  onGenerate: (name: string) => Promise<string>
}) {
  const [name, setName] = useState('')
  const [generatedKey, setGeneratedKey] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [copied, setCopied] = useState(false)

  const handleSubmit = async () => {
    if (!name.trim()) return
    setLoading(true)
    try {
      const key = await onGenerate(name.trim())
      setGeneratedKey(key)
    } finally {
      setLoading(false)
    }
  }

  const handleCopy = () => {
    if (generatedKey) {
      navigator.clipboard.writeText(generatedKey)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-surface-raised border border-DEFAULT rounded-2xl shadow-dialog w-full max-w-md">
        <div className="px-6 pt-6 pb-4 border-b border-DEFAULT">
          <h2 className="text-lg font-bold text-fg">
            {generatedKey ? 'API Key Created' : 'New API Key'}
          </h2>
        </div>

        <div className="px-6 py-5">
          {!generatedKey ? (
            <>
              <label className="ds-eyebrow block mb-2">Key name</label>
              <input
                autoFocus
                type="text"
                className="w-full bg-surface-raised border border-DEFAULT rounded-lg px-3 py-2.5 text-fg text-sm outline-none focus:border-accent placeholder:text-fg-faint transition-colors"
                placeholder="e.g. Home automation script"
                value={name}
                onChange={(e) => setName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
              />
            </>
          ) : (
            <>
              <p className="text-sm text-warning mb-3 font-medium">
                Copy this key now — it will not be shown again.
              </p>
              <div className="flex items-center gap-2">
                <code className="flex-1 bg-bg text-success-text text-xs font-mono px-3 py-2.5 rounded-lg break-all select-all border border-DEFAULT">
                  {generatedKey}
                </code>
                <button
                  onClick={handleCopy}
                  className="shrink-0 bg-accent hover:bg-accent-hover text-accent-fg px-3 py-2.5 rounded-lg text-sm font-semibold transition-colors"
                >
                  {copied ? 'Copied!' : 'Copy'}
                </button>
              </div>
              <p className="text-xs text-fg-faint mt-3">
                Send this key in the <code className="text-fg-muted font-mono">x-api-key</code> header when calling the API.
              </p>
            </>
          )}
        </div>

        <div className="px-6 pb-5 flex justify-end gap-2">
          {!generatedKey ? (
            <>
              <button
                onClick={onClose}
                className="text-sm text-fg-muted hover:text-fg transition-colors px-4 py-2 font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={handleSubmit}
                disabled={!name.trim() || loading}
                className="bg-accent hover:bg-accent-hover disabled:opacity-40 disabled:cursor-not-allowed text-accent-fg px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
              >
                {loading ? 'Generating…' : 'Generate'}
              </button>
            </>
          ) : (
            <button
              onClick={onClose}
              className="bg-accent hover:bg-accent-hover text-accent-fg px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
            >
              Done
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

function RevokeConfirmDialog({
  keyMeta,
  onClose,
  onConfirm,
}: {
  keyMeta: ApiKeyMeta
  onClose: () => void
  onConfirm: () => Promise<void>
}) {
  const [loading, setLoading] = useState(false)

  const handleConfirm = async () => {
    setLoading(true)
    try {
      await onConfirm()
      onClose()
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-surface-raised border border-DEFAULT rounded-2xl shadow-dialog w-full max-w-sm">
        <div className="px-6 pt-6 pb-4">
          <h2 className="text-lg font-bold mb-2 text-fg">Revoke API Key</h2>
          <p className="text-sm text-fg-muted">
            Revoke <span className="text-fg font-semibold">"{keyMeta.name}"</span>? Any scripts using this key will stop working immediately.
          </p>
        </div>
        <div className="px-6 pb-5 flex justify-end gap-2">
          <button
            onClick={onClose}
            className="text-sm text-fg-muted hover:text-fg transition-colors px-4 py-2 font-semibold"
          >
            Cancel
          </button>
          <button
            onClick={handleConfirm}
            disabled={loading}
            className="bg-danger hover:bg-red-600 disabled:opacity-40 text-white px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
          >
            {loading ? 'Revoking…' : 'Revoke'}
          </button>
        </div>
      </div>
    </div>
  )
}

function ThemeSwatch({ themeId, label, accent, bg, active, onSelect }: {
  themeId: Theme
  label: string
  accent: string
  bg: string
  active: boolean
  onSelect: () => void
}) {
  return (
    <button
      onClick={onSelect}
      className={
        'rounded-2xl p-4 text-left border transition-colors ' +
        (active
          ? 'border-accent bg-surface-high'
          : 'border-DEFAULT bg-surface-raised hover:bg-surface-high')
      }
      aria-pressed={active}
      data-theme={themeId}
    >
      <div
        className="h-16 w-full rounded-lg mb-3 flex items-end p-2"
        style={{ background: bg }}
      >
        <div className="h-3 w-12 rounded-full" style={{ background: accent }} />
      </div>
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold text-fg">{label}</span>
        {active && <span className="text-xs text-accent-text font-medium">Active</span>}
      </div>
    </button>
  )
}

export default function SettingsPage() {
  const { user } = useAuth()
  const { keys, loading, generateKey, revokeKey } = useApiKeys(user!.uid)
  const { theme, setTheme } = useTheme()
  const [showGenerate, setShowGenerate] = useState(false)
  const [revokeTarget, setRevokeTarget] = useState<ApiKeyMeta | null>(null)

  return (
    <AppShell>
      <div className="flex-1 overflow-y-auto">
      <main className="max-w-3xl mx-auto px-6 py-10">
        {/* Appearance */}
        <section className="mb-12">
          <div className="ds-eyebrow mb-3">Appearance</div>
          <h2 className="text-xl font-bold mb-1">Theme</h2>
          <p className="text-fg-muted text-sm mb-5">Pick a theme. Affects accent colour and surfaces across the app.</p>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {THEMES.map((t) => (
              <ThemeSwatch
                key={t.id}
                themeId={t.id}
                label={t.label}
                accent={t.accent}
                bg={t.bg}
                active={theme === t.id}
                onSelect={() => setTheme(t.id)}
              />
            ))}
          </div>
        </section>

        {/* API Keys section */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <div className="ds-eyebrow mb-2">Integration</div>
            <h1 className="text-2xl font-extrabold tracking-tightish">API Keys</h1>
            <p className="text-fg-muted text-sm mt-1">
              Use an API key to create reminders programmatically via the REST API.
            </p>
          </div>
          <button
            onClick={() => setShowGenerate(true)}
            className="bg-accent hover:bg-accent-hover text-accent-fg px-4 py-2 rounded-xl text-sm font-semibold transition-colors shrink-0"
          >
            New API Key
          </button>
        </div>

        {loading ? (
          <p className="text-fg-faint text-sm">Loading…</p>
        ) : keys.length === 0 ? (
          <div className="border border-dashed border-strong rounded-xl px-6 py-10 text-center">
            <p className="text-fg-muted text-sm">No API keys yet.</p>
            <p className="text-fg-faint text-xs mt-1">Generate a key to start using the API.</p>
          </div>
        ) : (
          <div className="border border-DEFAULT rounded-xl overflow-hidden bg-surface-raised">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-DEFAULT text-fg-muted text-xs uppercase tracking-wider">
                  <th className="text-left px-5 py-3 font-bold">Name</th>
                  <th className="text-left px-5 py-3 font-bold">Created</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody>
                {keys.map((key) => (
                  <tr key={key.id} className="border-b border-subtle last:border-0 hover:bg-surface-high/50 transition-colors">
                    <td className="px-5 py-3.5 font-semibold text-fg">{key.name}</td>
                    <td className="px-5 py-3.5 text-fg-muted">{formatDate(key.createdAt)}</td>
                    <td className="px-5 py-3.5 text-right">
                      <button
                        onClick={() => setRevokeTarget(key)}
                        className="text-danger-text hover:text-danger transition-colors text-xs font-semibold"
                      >
                        Revoke
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* API reference */}
        <div className="mt-10 border border-DEFAULT rounded-xl p-6 bg-surface-raised">
          <h2 className="text-base font-bold mb-3 text-fg">API Reference</h2>
          <p className="text-fg-muted text-sm mb-4">
            POST to the endpoint below with your API key in the <code className="text-fg font-mono">x-api-key</code> header.
          </p>
          <code className="block bg-bg text-fg text-xs font-mono px-4 py-3 rounded-lg mb-5 select-all border border-DEFAULT">
            https://us-central1-crabban.cloudfunctions.net/createReminder
          </code>

          <h3 className="text-sm font-semibold text-fg mb-2">One-off reminder</h3>
          <pre className="bg-bg text-fg text-xs font-mono px-4 py-3 rounded-lg overflow-x-auto mb-5 border border-DEFAULT">{`{
  "type": "one_off",
  "title": "Take medication",
  "scheduledAt": 1714220400000,
  "reminderStyle": "NOTIFICATION"
}`}</pre>

          <h3 className="text-sm font-semibold text-fg mb-2">Recurring reminder</h3>
          <pre className="bg-bg text-fg text-xs font-mono px-4 py-3 rounded-lg overflow-x-auto border border-DEFAULT">{`{
  "type": "recurring",
  "title": "Weekly review",
  "recurrenceRule": {
    "type": "WEEKLY",
    "interval": 1,
    "daysOfWeek": [2],
    "dayOfMonth": 0
  },
  "startDate": 1714176000000,
  "reminderTime": "09:00",
  "reminderStyle": "NOTIFICATION"
}`}</pre>
        </div>

        {/* About */}
        <section className="mt-12">
          <div className="ds-eyebrow mb-3">About</div>
          <div className="border border-DEFAULT rounded-xl px-6 py-4 bg-surface-raised">
            <div className="text-sm font-bold text-fg">Crab Do</div>
            <div className="text-sm text-fg-muted mt-0.5">Version {__APP_VERSION__}</div>
          </div>
        </section>
      </main>
      </div>

      {showGenerate && (
        <GenerateKeyDialog
          onClose={() => setShowGenerate(false)}
          onGenerate={generateKey}
        />
      )}

      {revokeTarget && (
        <RevokeConfirmDialog
          keyMeta={revokeTarget}
          onClose={() => setRevokeTarget(null)}
          onConfirm={() => revokeKey(revokeTarget.keyHash, revokeTarget.id)}
        />
      )}
    </AppShell>
  )
}
