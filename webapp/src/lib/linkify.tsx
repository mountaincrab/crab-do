import { Fragment, ReactNode } from 'react'

// Links are stored inline in plain-string fields (task title/description,
// subtask title) as Markdown — `[label](url)` — plus bare `https://…` URLs are
// autolinked. There is no schema change: the field stays a string on every
// surface (Firestore, Room, the cloud function), so only the display + the
// authoring affordances differ. Keep this renderer link-only (no other Markdown)
// so a stray `#`, `*` or `_` in normal text never renders as formatting.

// A Markdown link `[label](url)` OR a bare http(s) URL.
const TOKEN = /\[([^\]\n]+)\]\((https?:\/\/[^\s)]+)\)|(https?:\/\/[^\s<]+)/g

// Trailing punctuation that is almost never part of a URL when it sits at the
// end of a bare link in prose (e.g. "see https://x.com." → drop the period).
const TRAILING = /[.,;:!?)]+$/

export function isUrl(text: string): boolean {
  return /^https?:\/\/\S+$/.test(text.trim())
}

/** Wrap `label` (defaulting to the URL) in Markdown link syntax. */
export function markdownLink(label: string, url: string): string {
  const trimmed = url.trim()
  const text = label.trim() || trimmed
  return `[${text}](${trimmed})`
}

/**
 * Render a plain string, turning Markdown links and bare URLs into clickable
 * anchors. Everything else is emitted as literal text. Anchors stop click
 * propagation so following a link inside a clickable card/row doesn't also
 * trigger the card/row handler.
 */
export function Linkified({ text }: { text: string }): JSX.Element {
  const nodes: ReactNode[] = []
  let last = 0
  let key = 0
  TOKEN.lastIndex = 0
  let m: RegExpExecArray | null
  while ((m = TOKEN.exec(text)) !== null) {
    if (m.index > last) nodes.push(text.slice(last, m.index))

    let label: string
    let url: string
    let trailing = ''
    if (m[3] != null) {
      // Bare URL — peel trailing prose punctuation back into the text run.
      const raw = m[3]
      const stripped = raw.replace(TRAILING, '')
      trailing = raw.slice(stripped.length)
      url = stripped
      label = stripped
    } else {
      label = m[1]
      url = m[2]
    }

    nodes.push(
      <a
        key={key++}
        href={url}
        target="_blank"
        rel="noopener noreferrer"
        onClick={(e) => e.stopPropagation()}
        onMouseDown={(e) => e.stopPropagation()}
        className="text-accent underline underline-offset-2 hover:opacity-80 break-words"
      >
        {label}
      </a>,
    )
    if (trailing) nodes.push(trailing)
    last = m.index + m[0].length
  }
  if (last < text.length) nodes.push(text.slice(last))
  return <Fragment>{nodes}</Fragment>
}

type Field = HTMLTextAreaElement | HTMLInputElement

function spliceLink(el: Field, value: string, onChange: (v: string) => void, url: string, label: string) {
  const start = el.selectionStart ?? value.length
  const end = el.selectionEnd ?? value.length
  const md = markdownLink(label, url)
  const next = value.slice(0, start) + md + value.slice(end)
  onChange(next)
  const caret = start + md.length
  requestAnimationFrame(() => {
    el.focus()
    el.setSelectionRange(caret, caret)
  })
}

/**
 * Paste-to-link: if the pasted clipboard text is a URL and the user has text
 * selected, wrap the selection in a Markdown link instead of replacing it.
 * Falls through to the browser's default paste otherwise.
 */
export function makeLinkPasteHandler(value: string, onChange: (v: string) => void) {
  return (e: React.ClipboardEvent<Field>) => {
    const pasted = e.clipboardData.getData('text')
    if (!isUrl(pasted)) return
    const el = e.currentTarget
    const start = el.selectionStart ?? 0
    const end = el.selectionEnd ?? 0
    if (end <= start) return // nothing selected — let it paste normally
    e.preventDefault()
    spliceLink(el, value, onChange, pasted, value.slice(start, end))
  }
}

/**
 * ⌘K / Ctrl-K to add a link: uses the current selection as the label (prompting
 * for one when nothing is selected) and prompts for the URL.
 */
export function makeLinkKeyHandler(value: string, onChange: (v: string) => void) {
  return (e: React.KeyboardEvent<Field>) => {
    if (!(e.metaKey || e.ctrlKey) || e.key.toLowerCase() !== 'k') return
    e.preventDefault()
    const el = e.currentTarget
    const start = el.selectionStart ?? 0
    const end = el.selectionEnd ?? 0
    const selected = value.slice(start, end)
    const url = window.prompt('Link URL')?.trim()
    if (!url) return
    const label = selected || window.prompt('Link text', url)?.trim() || url
    spliceLink(el, value, onChange, url, label)
  }
}
