import { useEffect, useRef } from 'react'

interface ConfirmDialogProps {
  title: string
  message?: string
  confirmLabel?: string
  cancelLabel?: string
  /** `danger` for destructive actions, `accent` for neutral ones. */
  tone?: 'danger' | 'accent'
  onConfirm: () => void
  onCancel: () => void
}

/**
 * In-app replacement for `window.confirm`, styled like the rest of the app.
 * Sits above the other modals (z-60) so it can confirm actions taken inside one.
 */
export default function ConfirmDialog({
  title, message, confirmLabel = 'Confirm', cancelLabel = 'Cancel',
  tone = 'danger', onConfirm, onCancel,
}: ConfirmDialogProps) {
  const confirmRef = useRef<HTMLButtonElement>(null)

  useEffect(() => { confirmRef.current?.focus() }, [])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') { e.stopPropagation(); onCancel() }
      if (e.key === 'Enter') { e.stopPropagation(); onConfirm() }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onCancel, onConfirm])

  return (
    <div
      className="fixed inset-0 bg-black/60 flex items-center justify-center z-[60] p-4"
      onClick={onCancel}
    >
      <div
        role="dialog"
        aria-modal="true"
        className="bg-surface-raised border border-DEFAULT rounded-2xl p-6 w-full max-w-sm shadow-dialog"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-bold text-fg">{title}</h2>
        {message && <p className="text-sm text-fg-muted mt-2">{message}</p>}
        <div className="flex justify-end gap-2 mt-6">
          <button
            onClick={onCancel}
            className="px-4 py-2 rounded-xl text-fg-muted hover:text-fg transition-colors text-sm font-semibold"
          >
            {cancelLabel}
          </button>
          <button
            ref={confirmRef}
            onClick={onConfirm}
            className={`px-4 py-2 rounded-xl text-sm font-semibold transition-colors ${
              tone === 'danger'
                ? 'bg-danger hover:opacity-80 text-white'
                : 'bg-accent hover:bg-accent-hover text-accent-fg'
            }`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
