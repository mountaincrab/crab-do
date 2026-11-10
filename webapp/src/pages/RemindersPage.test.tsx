import { act, create, ReactTestRenderer } from 'react-test-renderer'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import type { TaskReminderSummary } from '../hooks/useTaskReminders'
import RemindersPage from './RemindersPage'

const fixtures = vi.hoisted(() => ({ tasks: [] as TaskReminderSummary[] }))
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => ({ user: { uid: 'user' } }) }))
vi.mock('../components/AppShell', () => ({ default: ({ children }: { children: ReactNode }) => children }))
vi.mock('../hooks/useTaskReminders', () => ({
  useTaskReminders: () => ({ taskReminders: fixtures.tasks, loading: false, clearTaskReminder: vi.fn() }),
}))
vi.mock('../hooks/useReminders', () => ({
  useReminders: () => ({
    reminders: [], completedReminders: [], deletedReminders: [], recurringReminders: [], deletedRecurring: [],
    loading: false, completedPage: {}, deletedPage: {}, deletedRecurringPage: {},
  }),
}))

let renderer: ReactTestRenderer
beforeEach(() => {
  vi.useFakeTimers()
  vi.setSystemTime(new Date(2026, 10, 10, 12))
})
afterEach(() => {
  act(() => renderer?.unmount())
  vi.useRealTimers()
})

function reminder(title: string, scheduled: Date | null, snoozed?: Date): TaskReminderSummary {
  return {
    task: {
      id: title, boardId: 'board', columnId: 'column', title, description: '', order: 1,
      reminderTimeMillis: scheduled?.getTime() ?? null, snoozedUntilMillis: snoozed?.getTime(),
      reminderStyle: 'NOTIFICATION', updatedAt: 0, isDeleted: false,
    },
    boardTitle: 'Work', columnTitle: 'To do',
  }
}

function visibleGroups() {
  act(() => { renderer = create(<MemoryRouter initialEntries={['/reminders?view=one-off']}><RemindersPage /></MemoryRouter>) })
  const section = renderer.root.findByProps({ 'aria-label': 'Task reminders' })
  const labelsAndTitles = new Set(['Today', 'Tomorrow', 'Next 7 Days', 'Future', 'Past', ...fixtures.tasks.map(({ task }) => task.title)])
  return section.findAllByType('p').map((node) => node.children.join('')).filter((text) => labelsAndTitles.has(text))
}

it('groups task reminders at the standalone time boundaries, using snooze times when present', () => {
  fixtures.tasks = [
    reminder('past', new Date(2026, 10, 9, 23, 59, 59, 999)),
    reminder('today-start', new Date(2026, 10, 10)),
    reminder('today-end', new Date(2026, 10, 10, 23, 59, 59, 999)),
    reminder('tomorrow-start', new Date(2026, 10, 11)),
    reminder('rescheduled-by-snooze', new Date(2026, 10, 30), new Date(2026, 10, 11, 5)),
    reminder('snooze-only', null, new Date(2026, 10, 11, 10)),
    reminder('tomorrow-end', new Date(2026, 10, 11, 23, 59, 59, 999)),
    reminder('next-seven-start', new Date(2026, 10, 12)),
    reminder('next-seven-end', new Date(2026, 10, 17, 23, 59, 59, 999)),
    reminder('future-start', new Date(2026, 10, 18)),
  ]
  expect(visibleGroups()).toEqual([
    'Today', 'today-start', 'today-end',
    'Tomorrow', 'tomorrow-start', 'rescheduled-by-snooze', 'snooze-only', 'tomorrow-end',
    'Next 7 Days', 'next-seven-start', 'next-seven-end',
    'Future', 'future-start', 'Past', 'past',
  ])
})

it('shows only populated time groups when there are task reminders but no standalone reminders', () => {
  fixtures.tasks = [reminder('snoozed task', null, new Date(2026, 10, 11, 10))]
  expect(visibleGroups()).toEqual(['Tomorrow', 'snoozed task'])
})
