import { act, create, ReactTestRenderer } from 'react-test-renderer'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { DocumentData, Query } from 'firebase/firestore'
import { PAGE_SIZE, usePagedQuery } from './usePagedQuery'

const { subscriptions } = vi.hoisted(() => ({ subscriptions: [] as {
  size: number
  emit: (snapshot: unknown) => void
  fail: (error: Error) => void
  stop: ReturnType<typeof vi.fn>
}[] }))

vi.mock('firebase/firestore', () => ({
  limit: (size: number) => size,
  query: (_source: unknown, size: number) => size,
  onSnapshot: (size: number, emit: (snapshot: unknown) => void, fail: (error: Error) => void) => {
    const stop = vi.fn()
    subscriptions.push({ size, emit, fail, stop })
    return stop
  },
}))

const source = {} as Query<DocumentData>
let page: ReturnType<typeof usePagedQuery<{ id: string }>>
let renderer: ReactTestRenderer | undefined
function Harness({ enabled = true, query = source }) {
  page = usePagedQuery<{ id: string }>(query, enabled)
  return null
}
function emit(ids: string[], index = subscriptions.length - 1) {
  act(() => subscriptions[index].emit({ docs: ids.map((id) => ({ id, data: () => ({}) })) }))
}
const ids = (count: number) => Array.from({ length: count }, (_, i) => `task-${i}`)

afterEach(() => {
  act(() => renderer?.unmount())
  renderer = undefined
  subscriptions.length = 0
})

describe('live paging', () => {
  it('loads one page plus lookahead, grows only on request, and detects the end', () => {
    act(() => { renderer = create(<Harness />) })
    expect(subscriptions[0].size).toBe(PAGE_SIZE + 1)
    expect(page.loading).toBe(true)
    emit(ids(51))
    expect(page.items).toHaveLength(50)
    expect(page.next?.id).toBe('task-50')
    act(() => page.loadMore())
    expect(subscriptions[0].stop).toHaveBeenCalledOnce()
    expect(subscriptions[1].size).toBe(101)
    expect(page.items).toHaveLength(50)
    expect(page.loading).toBe(true)
    emit(ids(73))
    expect(page.items).toHaveLength(73)
    expect(page.hasMore).toBe(false)
    act(() => page.loadMore())
    expect(subscriptions).toHaveLength(2)
  })

  it('replaces the contiguous window after moves, inserts, deletes, and restores', () => {
    act(() => { renderer = create(<Harness />) })
    emit(ids(51))
    act(() => page.loadMore())
    emit(ids(80))
    emit(['inserted', ...ids(80).filter((id) => id !== 'task-49')])
    expect(page.items).toHaveLength(80)
    expect(new Set(page.items.map((item) => item.id)).size).toBe(80)
    expect(page.items[0].id).toBe('inserted')
    expect(page.items.some((item) => item.id === 'task-49')).toBe(false)
    emit(ids(81))
    expect(page.items[49].id).toBe('task-49')
  })

  it('does not subscribe to collapsed history and resets it on close', () => {
    act(() => { renderer = create(<Harness enabled={false} />) })
    expect(subscriptions).toHaveLength(0)
    act(() => renderer!.update(<Harness />))
    emit(ids(51))
    act(() => page.loadMore())
    act(() => renderer!.update(<Harness enabled={false} />))
    expect(page.items).toEqual([])
    expect(subscriptions[1].stop).toHaveBeenCalledOnce()
    act(() => renderer!.update(<Harness />))
    expect(subscriptions[subscriptions.length - 1]?.size).toBe(51)
  })

  it('isolates a new account/query and ignores callbacks from an unsubscribed window', () => {
    act(() => { renderer = create(<Harness />) })
    emit(ids(51))
    act(() => page.loadMore())
    act(() => renderer!.update(<Harness query={{} as Query<DocumentData>} />))
    expect(page.items).toEqual([])
    expect(subscriptions[subscriptions.length - 1]?.size).toBe(51)
    emit(['stale'], 0)
    expect(page.items).toEqual([])
    emit(['current'])
    expect(page.items).toEqual([{ id: 'current' }])
  })

  it('keeps loaded rows on failure and retries the requested page', () => {
    act(() => { renderer = create(<Harness />) })
    emit(ids(51))
    act(() => page.loadMore())
    act(() => subscriptions[1].fail(new Error('unavailable')))
    expect(page.error).toBe('unavailable')
    expect(page.items).toHaveLength(50)
    expect(page.loading).toBe(false)
    act(() => page.retry())
    expect(subscriptions[subscriptions.length - 1]?.size).toBe(101)
    emit(ids(70))
    expect(page.error).toBeUndefined()
    expect(page.items).toHaveLength(70)
  })
})
