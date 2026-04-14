import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useBoards } from '../hooks/useBoards'

export default function BoardListPage() {
  const { user, signOut } = useAuth()
  const { boards, loading, createBoard } = useBoards(user!.uid)
  const navigate = useNavigate()
  const [newTitle, setNewTitle] = useState('')
  const [creating, setCreating] = useState(false)
  const [showDialog, setShowDialog] = useState(false)

  const handleCreate = async () => {
    if (!newTitle.trim()) return
    setCreating(true)
    await createBoard(newTitle.trim())
    setNewTitle('')
    setShowDialog(false)
    setCreating(false)
  }

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
            <span className="text-sm text-white font-medium px-3 py-1.5 rounded-lg bg-surface-high">
              Boards
            </span>
            <Link
              to="/reminders"
              className="text-sm text-slate-400 hover:text-white transition-colors px-3 py-1.5 rounded-lg hover:bg-surface-high"
            >
              Reminders
            </Link>
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
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-2xl font-bold">My Boards</h1>
          <button
            onClick={() => setShowDialog(true)}
            className="bg-indigo-500 hover:bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
          >
            + New board
          </button>
        </div>

        {loading ? (
          <div className="text-slate-500 text-center py-20">Loading…</div>
        ) : boards.length === 0 ? (
          <div className="text-slate-500 text-center py-20">
            <p className="text-4xl mb-4">📋</p>
            <p>No boards yet. Create your first one.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {boards.map((board) => (
              <button
                key={board.id}
                onClick={() => navigate(`/board/${board.id}`)}
                className="bg-surface-raised hover:bg-surface-high rounded-xl p-5 text-left transition-colors group"
              >
                <div className="flex items-start justify-between">
                  <h2 className="font-semibold text-white group-hover:text-indigo-300 transition-colors">
                    {board.title}
                  </h2>
                  {board.isShared && (
                    <span className="text-xs text-slate-500 bg-surface-high px-2 py-0.5 rounded-full">
                      shared
                    </span>
                  )}
                </div>
              </button>
            ))}
          </div>
        )}
      </main>

      {/* New board dialog */}
      {showDialog && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-surface-raised rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <h2 className="text-lg font-semibold mb-4">New Board</h2>
            <input
              autoFocus
              type="text"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') handleCreate() }}
              placeholder="Board name"
              className="w-full bg-surface-high border border-white/10 rounded-lg px-3 py-2.5 text-white placeholder-slate-500 outline-none focus:border-indigo-500 mb-4"
            />
            <div className="flex justify-end gap-2">
              <button
                onClick={() => { setShowDialog(false); setNewTitle('') }}
                className="px-4 py-2 rounded-lg text-slate-400 hover:text-white transition-colors text-sm"
              >
                Cancel
              </button>
              <button
                onClick={handleCreate}
                disabled={creating || !newTitle.trim()}
                className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 disabled:opacity-40 text-white rounded-lg text-sm font-medium transition-colors"
              >
                Create
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
