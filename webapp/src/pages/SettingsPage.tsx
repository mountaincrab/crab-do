import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useApiKeys } from '../hooks/useApiKeys'
import { ApiKeyMeta } from '../types'

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
      <div className="bg-surface-high rounded-xl shadow-xl w-full max-w-md">
        <div className="px-6 pt-6 pb-4 border-b border-white/10">
          <h2 className="text-lg font-semibold">
            {generatedKey ? 'API Key Created' : 'New API Key'}
          </h2>
        </div>

        <div className="px-6 py-5">
          {!generatedKey ? (
            <>
              <label className="block text-sm text-slate-400 mb-1.5">Key name</label>
              <input
                autoFocus
                type="text"
                className="w-full bg-surface border border-white/10 rounded-lg px-3 py-2 text-white text-sm outline-none focus:border-indigo-500"
                placeholder="e.g. Home automation script"
                value={name}
                onChange={(e) => setName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
              />
            </>
          ) : (
            <>
              <p className="text-sm text-amber-400 mb-3">
                Copy this key now — it will not be shown again.
              </p>
              <div className="flex items-center gap-2">
                <code className="flex-1 bg-surface text-green-400 text-xs font-mono px-3 py-2 rounded-lg break-all select-all">
                  {generatedKey}
                </code>
                <button
                  onClick={handleCopy}
                  className="shrink-0 bg-indigo-500 hover:bg-indigo-600 text-white px-3 py-2 rounded-lg text-sm font-medium transition-colors"
                >
                  {copied ? 'Copied!' : 'Copy'}
                </button>
              </div>
              <p className="text-xs text-slate-500 mt-3">
                Send this key in the <code className="text-slate-400">x-api-key</code> header when calling the API.
              </p>
            </>
          )}
        </div>

        <div className="px-6 pb-5 flex justify-end gap-3">
          {!generatedKey ? (
            <>
              <button
                onClick={onClose}
                className="text-sm text-slate-400 hover:text-white transition-colors px-4 py-2"
              >
                Cancel
              </button>
              <button
                onClick={handleSubmit}
                disabled={!name.trim() || loading}
                className="bg-indigo-500 hover:bg-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
              >
                {loading ? 'Generating…' : 'Generate'}
              </button>
            </>
          ) : (
            <button
              onClick={onClose}
              className="bg-indigo-500 hover:bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
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
      <div className="bg-surface-high rounded-xl shadow-xl w-full max-w-sm">
        <div className="px-6 pt-6 pb-4">
          <h2 className="text-lg font-semibold mb-2">Revoke API Key</h2>
          <p className="text-sm text-slate-400">
            Revoke <span className="text-white font-medium">"{keyMeta.name}"</span>? Any scripts using this key will stop working immediately.
          </p>
        </div>
        <div className="px-6 pb-5 flex justify-end gap-3">
          <button
            onClick={onClose}
            className="text-sm text-slate-400 hover:text-white transition-colors px-4 py-2"
          >
            Cancel
          </button>
          <button
            onClick={handleConfirm}
            disabled={loading}
            className="bg-red-500 hover:bg-red-600 disabled:opacity-50 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
          >
            {loading ? 'Revoking…' : 'Revoke'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function SettingsPage() {
  const { user, signOut } = useAuth()
  const { keys, loading, generateKey, revokeKey } = useApiKeys(user!.uid)
  const [showGenerate, setShowGenerate] = useState(false)
  const [revokeTarget, setRevokeTarget] = useState<ApiKeyMeta | null>(null)

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
            <Link
              to="/reminders"
              className="text-sm text-slate-400 hover:text-white transition-colors px-3 py-1.5 rounded-lg hover:bg-surface-high"
            >
              Reminders
            </Link>
            <span className="text-sm text-white font-medium px-3 py-1.5 rounded-lg bg-surface-high">
              Settings
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

      <main className="max-w-3xl mx-auto px-6 py-10">
        {/* API Keys section */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold">API Keys</h1>
            <p className="text-slate-400 text-sm mt-1">
              Use an API key to create reminders programmatically via the REST API.
            </p>
          </div>
          <button
            onClick={() => setShowGenerate(true)}
            className="bg-indigo-500 hover:bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors shrink-0"
          >
            New API Key
          </button>
        </div>

        {loading ? (
          <p className="text-slate-500 text-sm">Loading…</p>
        ) : keys.length === 0 ? (
          <div className="border border-dashed border-white/10 rounded-xl px-6 py-10 text-center">
            <p className="text-slate-400 text-sm">No API keys yet.</p>
            <p className="text-slate-500 text-xs mt-1">Generate a key to start using the API.</p>
          </div>
        ) : (
          <div className="border border-white/10 rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/10 text-slate-400 text-xs uppercase tracking-wide">
                  <th className="text-left px-5 py-3 font-medium">Name</th>
                  <th className="text-left px-5 py-3 font-medium">Created</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody>
                {keys.map((key) => (
                  <tr key={key.id} className="border-b border-white/5 last:border-0 hover:bg-surface-high/50 transition-colors">
                    <td className="px-5 py-3.5 font-medium">{key.name}</td>
                    <td className="px-5 py-3.5 text-slate-400">{formatDate(key.createdAt)}</td>
                    <td className="px-5 py-3.5 text-right">
                      <button
                        onClick={() => setRevokeTarget(key)}
                        className="text-red-400 hover:text-red-300 transition-colors text-xs font-medium"
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
        <div className="mt-10 border border-white/10 rounded-xl p-6">
          <h2 className="text-base font-semibold mb-3">API Reference</h2>
          <p className="text-slate-400 text-sm mb-4">
            POST to the endpoint below with your API key in the <code className="text-slate-300">x-api-key</code> header.
          </p>
          <code className="block bg-surface text-slate-300 text-xs font-mono px-4 py-3 rounded-lg mb-5 select-all">
            https://us-central1-crabban.cloudfunctions.net/createReminder
          </code>

          <h3 className="text-sm font-medium text-slate-300 mb-2">One-off reminder</h3>
          <pre className="bg-surface text-slate-300 text-xs font-mono px-4 py-3 rounded-lg overflow-x-auto mb-5">{`{
  "type": "one_off",
  "title": "Take medication",
  "scheduledAt": 1714220400000,
  "reminderStyle": "NOTIFICATION"
}`}</pre>

          <h3 className="text-sm font-medium text-slate-300 mb-2">Recurring reminder</h3>
          <pre className="bg-surface text-slate-300 text-xs font-mono px-4 py-3 rounded-lg overflow-x-auto">{`{
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
      </main>

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
    </div>
  )
}
