import { describe, expect, it } from 'vitest'
import { orderForGap } from './taskOrder'

describe('task placement with partial columns', () => {
  it('places a card at the loaded boundary before the first unloaded card', () => {
    expect(orderForGap(2, [{ order: 1 }, { order: 2 }], { order: 2.5 })).toBe(2.25)
  })
  it('handles empty columns, the start, interior gaps, and the real end', () => {
    expect(orderForGap(0, [])).toBe(1)
    expect(orderForGap(0, [{ order: 5 }])).toBe(4)
    expect(orderForGap(1, [{ order: 5 }, { order: 7 }])).toBe(6)
    expect(orderForGap(1, [{ order: 5 }])).toBe(6)
  })
})
