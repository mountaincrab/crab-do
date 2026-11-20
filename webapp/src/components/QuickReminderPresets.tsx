export type QuickPreset = 'LATER_TODAY' | 'TOMORROW' | 'NEXT_WEEK'

/** Quick-set reminder times in the user's local timezone. */
export function quickPresetMillis(preset: QuickPreset, now: Date = new Date()): number {
  const d = new Date(now)
  if (preset === 'LATER_TODAY') {
    d.setHours(d.getHours() + 3, 0, 0, 0)
  } else if (preset === 'TOMORROW') {
    d.setDate(d.getDate() + 1)
    d.setHours(9, 0, 0, 0)
  } else {
    // The coming Monday at 09:00 (a full week ahead when today is Monday).
    d.setDate(d.getDate() + ((8 - d.getDay()) % 7 || 7))
    d.setHours(9, 0, 0, 0)
  }
  return d.getTime()
}

export default function QuickReminderPresets({ onSelect }: { onSelect: (millis: number) => void }) {
  return (
    <div className="flex gap-2">
      {([['LATER_TODAY', 'Later'], ['TOMORROW', 'Tomorrow'], ['NEXT_WEEK', 'Next week']] as const).map(([preset, label]) => (
        <button
          key={preset}
          onClick={() => onSelect(quickPresetMillis(preset))}
          className="px-3 py-1 rounded-full border border-DEFAULT text-xs font-semibold text-fg-muted hover:text-fg hover:border-strong transition-colors"
        >
          {label}
        </button>
      ))}
    </div>
  )
}
