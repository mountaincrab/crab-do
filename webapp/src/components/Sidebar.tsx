import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
  ChevronDown, ChevronRight, Bell, Repeat, Settings, LogOut, ClipboardList,
} from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useBoards } from '../hooks/useBoards'
import CrabMark from './CrabMark'

function SectionHeader({
  label, expanded, onToggle, to, active,
}: {
  label: string
  expanded: boolean
  onToggle: () => void
  to: string
  active: boolean
}) {
  return (
    <div className="flex items-center group">
      <button
        onClick={onToggle}
        className="p-1 rounded text-fg-faint hover:text-fg hover:bg-surface-high transition-colors"
        aria-label={expanded ? `Collapse ${label}` : `Expand ${label}`}
      >
        {expanded ? <ChevronDown size={15} /> : <ChevronRight size={15} />}
      </button>
      <Link
        to={to}
        className={`flex-1 text-xs font-bold uppercase tracking-wider px-1.5 py-1.5 rounded-lg transition-colors ${
          active ? 'text-fg' : 'text-fg-muted hover:text-fg'
        }`}
      >
        {label}
      </Link>
    </div>
  )
}

function NavChild({
  to, label, icon, active,
}: {
  to: string
  label: string
  icon: React.ReactNode
  active: boolean
}) {
  return (
    <Link
      to={to}
      className={`flex items-center gap-2.5 pl-8 pr-3 py-1.5 rounded-lg text-sm transition-colors truncate ${
        active
          ? 'bg-accent-soft text-accent-text font-semibold'
          : 'text-fg-muted hover:text-fg hover:bg-surface-high'
      }`}
    >
      <span className="shrink-0 text-fg-faint">{icon}</span>
      <span className="truncate">{label}</span>
    </Link>
  )
}

export default function Sidebar() {
  const { user, signOut } = useAuth()
  const { boards } = useBoards(user!.uid)
  const location = useLocation()
  const [boardsOpen, setBoardsOpen] = useState(true)
  const [remindersOpen, setRemindersOpen] = useState(true)

  const path = location.pathname
  const view = new URLSearchParams(location.search).get('view')
  const activeBoardId = path.startsWith('/board/') ? path.split('/')[2] : null
  const onReminders = path === '/reminders'

  return (
    <aside className="w-60 shrink-0 h-screen bg-surface border-r border-DEFAULT flex flex-col">
      <Link to="/" className="flex items-center gap-2.5 px-4 h-14 shrink-0 border-b border-DEFAULT">
        <CrabMark size={26} />
        <span className="font-bold text-fg text-base tracking-tightish">Crab Do</span>
      </Link>

      <nav className="flex-1 overflow-y-auto px-2 py-3 flex flex-col gap-0.5">
        {/* Boards */}
        <SectionHeader
          label="Boards"
          expanded={boardsOpen}
          onToggle={() => setBoardsOpen((v) => !v)}
          to="/"
          active={path === '/'}
        />
        {boardsOpen && (
          <div className="flex flex-col gap-0.5 mb-1">
            {boards.length === 0 ? (
              <span className="pl-8 pr-3 py-1.5 text-sm text-fg-faint">No boards yet</span>
            ) : (
              boards.map((b) => (
                <NavChild
                  key={b.id}
                  to={`/board/${b.id}`}
                  label={b.title}
                  icon={<ClipboardList size={15} />}
                  active={activeBoardId === b.id}
                />
              ))
            )}
          </div>
        )}

        {/* Reminders */}
        <SectionHeader
          label="Reminders"
          expanded={remindersOpen}
          onToggle={() => setRemindersOpen((v) => !v)}
          to="/reminders"
          active={onReminders && !view}
        />
        {remindersOpen && (
          <div className="flex flex-col gap-0.5 mb-1">
            <NavChild
              to="/reminders?view=one-off"
              label="One-off"
              icon={<Bell size={15} />}
              active={onReminders && view === 'one-off'}
            />
            <NavChild
              to="/reminders?view=recurring"
              label="Recurring"
              icon={<Repeat size={15} />}
              active={onReminders && view === 'recurring'}
            />
          </div>
        )}
      </nav>

      <div className="border-t border-DEFAULT px-2 py-2 flex flex-col gap-0.5 shrink-0">
        <Link
          to="/settings"
          className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm transition-colors ${
            path === '/settings'
              ? 'bg-surface-high text-fg font-semibold'
              : 'text-fg-muted hover:text-fg hover:bg-surface-high'
          }`}
        >
          <Settings size={16} /> Settings
        </Link>
        <div className="flex items-center gap-2 px-3 pt-1.5">
          <span className="flex-1 text-xs text-fg-faint truncate" title={user?.email ?? ''}>
            {user?.email}
          </span>
          <button
            onClick={signOut}
            className="p-1.5 rounded-lg text-fg-faint hover:text-fg hover:bg-surface-high transition-colors"
            title="Sign out"
            aria-label="Sign out"
          >
            <LogOut size={15} />
          </button>
        </div>
      </div>
    </aside>
  )
}
