import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ClipboardList, MoreHorizontal, Users } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useBoards } from '../hooks/useBoards'
import { Board } from '../types'
import AppShell from '../components/AppShell'
import ConfirmDialog from '../components/ConfirmDialog'

export default function BoardListPage() {
  const { user } = useAuth()
  const { boards, loading, createBoard, deleteBoard } = useBoards(user!.uid)
  const navigate = useNavigate()
  const [newTitle, setNewTitle] = useState('')
  const [creating, setCreating] = useState(false)
  const [showDialog, setShowDialog] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState<Board | null>(null)
  const [openMenuId, setOpenMenuId] = useState<string | null>(null)

  const handleCreate = async () => {
    if (!newTitle.trim()) return
    setCreating(true)
    await createBoard(newTitle.trim())
    setNewTitle('')
    setShowDialog(false)
    setCreating(false)
  }

  const handleDelete = async () => {
    if (!confirmDelete) return
    await deleteBoard(confirmDelete.id)
    setConfirmDelete(null)
  }

  return (
    <AppShell>
      <div className="flex-1 overflow-y-auto" onClick={() => setOpenMenuId(null)}>
      <main className="max-w-3xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-extrabold tracking-tightish">My Boards</h1>
          <button
            onClick={() => setShowDialog(true)}
            className="bg-accent hover:bg-accent-hover text-accent-fg px-4 py-2 rounded-xl text-sm font-semibold transition-colors"
          >
            + New board
          </button>
        </div>

        {loading ? (
          <div className="text-fg-faint text-center py-20">Loading…</div>
        ) : boards.length === 0 ? (
          <div className="text-fg-faint text-center py-20">
            <ClipboardList size={40} className="mx-auto mb-4 text-fg-faint" />
            <p>No boards yet. Create your first one.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {boards.map((board) => (
              <div key={board.id} className="relative">
                <button
                  onClick={() => navigate(`/board/${board.id}`)}
                  className="w-full bg-surface-raised hover:bg-surface-high border border-DEFAULT rounded-xl p-5 text-left transition-colors group"
                >
                  <div className="flex items-start justify-between gap-3 pr-6">
                    <h2 className="font-semibold text-fg group-hover:text-accent-text transition-colors">
                      {board.title}
                    </h2>
                    {board.isShared && (
                      <span className="flex items-center gap-1 text-xs text-fg-muted bg-surface-high px-2 py-0.5 rounded-full shrink-0">
                        <Users size={11} />
                        shared
                      </span>
                    )}
                  </div>
                </button>

                <div className="absolute top-3 right-3">
                  <button
                    onClick={(e) => { e.stopPropagation(); setOpenMenuId(openMenuId === board.id ? null : board.id) }}
                    className="text-fg-faint hover:text-fg p-1 rounded transition-colors"
                  >
                    <MoreHorizontal size={16} />
                  </button>
                  {openMenuId === board.id && (
                    <div className="absolute right-0 top-full mt-1 bg-surface-high border border-DEFAULT rounded-lg shadow-dialog z-20 min-w-32 py-1">
                      <button
                        onClick={(e) => { e.stopPropagation(); setConfirmDelete(board); setOpenMenuId(null) }}
                        className="w-full text-left px-4 py-2 text-sm text-danger-text hover:bg-white/5 transition-colors"
                      >
                        Delete board
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
      </div>

      {showDialog && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-surface-raised border border-DEFAULT rounded-2xl p-6 w-full max-w-sm shadow-dialog">
            <h2 className="text-lg font-bold mb-4 text-fg">New Board</h2>
            <input
              autoFocus
              type="text"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') handleCreate() }}
              placeholder="Board name"
              className="w-full bg-surface-raised border border-DEFAULT rounded-lg px-3 py-2.5 text-fg placeholder:text-fg-faint outline-none focus:border-accent transition-colors mb-4"
            />
            <div className="flex justify-end gap-2">
              <button
                onClick={() => { setShowDialog(false); setNewTitle('') }}
                className="px-4 py-2 rounded-xl text-fg-muted hover:text-fg transition-colors text-sm font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={handleCreate}
                disabled={creating || !newTitle.trim()}
                className="px-4 py-2 bg-accent hover:bg-accent-hover disabled:opacity-40 text-accent-fg rounded-xl text-sm font-semibold transition-colors"
              >
                Create
              </button>
            </div>
          </div>
        </div>
      )}

      {confirmDelete && (
        <ConfirmDialog
          title="Delete board?"
          message={`"${confirmDelete.title}" and all its tasks will be permanently removed.`}
          confirmLabel="Delete"
          onConfirm={handleDelete}
          onCancel={() => setConfirmDelete(null)}
        />
      )}
    </AppShell>
  )
}
