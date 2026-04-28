export function computeNextFireAt(startDate: number, reminderTime: string): number {
  const [hourStr, minStr] = reminderTime.split(':')
  const hour = parseInt(hourStr, 10)
  const minute = parseInt(minStr, 10)

  const base = new Date(startDate)
  const candidate = new Date(base.getFullYear(), base.getMonth(), base.getDate(), hour, minute, 0, 0)

  const now = Date.now()
  while (candidate.getTime() <= now) {
    candidate.setDate(candidate.getDate() + 1)
  }

  return candidate.getTime()
}
