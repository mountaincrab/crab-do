import { describe, expect, it } from 'vitest'
import { quickPresetMillis } from './QuickReminderPresets'

describe('Next week quick-set', () => {
  it('picks the coming Monday at 09:00', () => {
    // Sat 14 Nov 2026 → Mon 16 Nov; Sun 15 Nov → Mon 16 Nov.
    expect(quickPresetMillis('NEXT_WEEK', new Date(2026, 10, 14, 18, 30))).toBe(new Date(2026, 10, 16, 9, 0).getTime())
    expect(quickPresetMillis('NEXT_WEEK', new Date(2026, 10, 15, 8, 0))).toBe(new Date(2026, 10, 16, 9, 0).getTime())
    // Tue 17 Nov → Mon 23 Nov.
    expect(quickPresetMillis('NEXT_WEEK', new Date(2026, 10, 17, 12, 0))).toBe(new Date(2026, 10, 23, 9, 0).getTime())
  })

  it('goes a full week ahead on a Monday, even before 09:00', () => {
    expect(quickPresetMillis('NEXT_WEEK', new Date(2026, 10, 16, 7, 0))).toBe(new Date(2026, 10, 23, 9, 0).getTime())
  })

  it('crosses month and year boundaries', () => {
    // Thu 31 Dec 2026 → Mon 4 Jan 2027.
    expect(quickPresetMillis('NEXT_WEEK', new Date(2026, 11, 31, 10, 0))).toBe(new Date(2027, 0, 4, 9, 0).getTime())
  })

  it('keeps 09:00 local time across daylight-saving changes', () => {
    expect(quickPresetMillis('NEXT_WEEK', new Date(2026, 2, 28, 18, 30))).toBe(new Date(2026, 2, 30, 9).getTime())
    expect(quickPresetMillis('NEXT_WEEK', new Date(2026, 9, 24, 18, 30))).toBe(new Date(2026, 9, 26, 9).getTime())
  })
})
