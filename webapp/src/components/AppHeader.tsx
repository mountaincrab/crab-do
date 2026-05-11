import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import CrabMark from './CrabMark'

type NavKey = 'boards' | 'reminders' | 'settings'

const NAV: { key: NavKey; label: string; to: string }[] = [
  { key: 'boards', label: 'Boards', to: '/' },
  { key: 'reminders', label: 'Reminders', to: '/reminders' },
  { key: 'settings', label: 'Settings', to: '/settings' },
]

export default function AppHeader({ active }: { active: NavKey }) {
  const { user, signOut } = useAuth()
  return (
    <header className="border-b border-DEFAULT bg-surface px-6 py-3 flex items-center justify-between">
      <div className="flex items-center gap-4">
        <Link to="/" className="flex items-center gap-2.5">
          <CrabMark size={28} />
          <span className="font-bold text-fg text-base tracking-tightish">Crab Do</span>
        </Link>
        <nav className="flex items-center gap-1 ml-2">
          {NAV.map((n) =>
            n.key === active ? (
              <span
                key={n.key}
                className="text-sm text-fg font-semibold px-3 py-1.5 rounded-lg bg-surface-high"
              >
                {n.label}
              </span>
            ) : (
              <Link
                key={n.key}
                to={n.to}
                className="text-sm text-fg-muted hover:text-fg transition-colors px-3 py-1.5 rounded-lg hover:bg-surface-high"
              >
                {n.label}
              </Link>
            )
          )}
        </nav>
      </div>
      <div className="flex items-center gap-4">
        <span className="text-fg-muted text-sm">{user?.email}</span>
        <button
          onClick={signOut}
          className="text-sm text-fg-muted hover:text-fg transition-colors"
        >
          Sign out
        </button>
      </div>
    </header>
  )
}
