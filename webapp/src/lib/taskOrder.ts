/** The lookahead row keeps a drop at the end of a partial page ahead of the
 * first unloaded task. Callers exclude the dragged task from the visible rows. */
export function orderForGap(gap: number, tasks: { order: number }[], lookahead?: { order: number }): number {
  const prev = tasks[gap - 1]
  const next = tasks[gap] ?? lookahead
  if (!prev && !next) return 1
  if (!prev) return next.order - 1
  if (!next) return prev.order + 1
  return (prev.order + next.order) / 2
}
