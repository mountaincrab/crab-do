import { act, create, ReactTestRenderer } from 'react-test-renderer'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useBoard } from './useBoard'

const mocks = vi.hoisted(() => ({
  getDocs: vi.fn(), updateDoc: vi.fn(), commits: [] as ReturnType<typeof vi.fn>[],
  writes: [] as { kind: string; ref: unknown; data: Record<string, unknown> }[],
}))
vi.mock('../firebase', () => ({ db: {} }))
vi.mock('firebase/firestore', () => ({
  collection: (_db: unknown, ...path: string[]) => path.join('/'),
  doc: (base: unknown, ...path: string[]) => path.length ? path.join('/') : `${base}/new-task`,
  query: (base: unknown, ...constraints: unknown[]) => ({ base, constraints }),
  where: (...args: unknown[]) => args,
  orderBy: (...args: unknown[]) => ['orderBy', ...args],
  limit: (count: number) => ['limit', count],
  onSnapshot: () => vi.fn(),
  addDoc: vi.fn(),
  serverTimestamp: () => 'timestamp',
  getDocsFromServer: mocks.getDocs,
  updateDoc: mocks.updateDoc,
  writeBatch: () => {
    const commit = vi.fn().mockResolvedValue(undefined)
    mocks.commits.push(commit)
    return {
      update: (ref: unknown, data: Record<string, unknown>) => mocks.writes.push({ kind: 'update', ref, data }),
      set: (ref: unknown, data: Record<string, unknown>) => mocks.writes.push({ kind: 'set', ref, data }),
      commit,
    }
  },
}))

let board: ReturnType<typeof useBoard>
let renderer: ReactTestRenderer | undefined
function Harness() { board = useBoard('user', 'board'); return null }
afterEach(() => {
  act(() => renderer?.unmount())
  vi.clearAllMocks()
  mocks.commits.length = 0
  mocks.writes.length = 0
})

describe('mutations on partially loaded boards', () => {
  it('deletes all 900 tasks in bounded batches before hiding the column', async () => {
    act(() => { renderer = create(<Harness />) })
    let offset = 0
    for (const size of [400, 400, 100, 0]) {
      mocks.getDocs.mockResolvedValueOnce({ empty: size === 0,
        docs: Array.from({ length: size }, () => ({ ref: `task-${offset++}` })),
      })
    }
    await board.deleteColumn('column')
    expect(mocks.writes).toHaveLength(900)
    expect(mocks.commits).toHaveLength(3)
    expect(mocks.commits.every((commit) => commit.mock.calls.length === 1)).toBe(true)
    expect(mocks.getDocs.mock.calls.every(([q]) => q.constraints.some((c: unknown[]) => c[0] === 'limit' && c[1] === 400))).toBe(true)
    expect(mocks.updateDoc).toHaveBeenCalledWith('users/user/boards/board/columns/column', {
      isDeleted: true, updatedAt: 'timestamp',
    })
    expect(mocks.writes.every((write) => write.data.reminderTimeMillis === null)).toBe(true)
  })

  it.each([{ firstOrder: -20, expected: -21 }, { firstOrder: null, expected: 1 }])('creates at the real top of a column ($firstOrder), regardless of loaded rows', async ({ firstOrder, expected }) => {
    act(() => { renderer = create(<Harness />) })
    mocks.getDocs.mockResolvedValueOnce({ docs: firstOrder === null ? [] : [{ data: () => ({ order: firstOrder }) }] })
    await board.createTask('column', {
      title: 'New', description: '', reminderTimeMillis: null, reminderStyle: 'ALARM',
      reminderTransitionColumnId: null, subtasks: [],
    })
    expect(mocks.writes[0].data.order).toBe(expected)
    expect(mocks.getDocs.mock.calls[0][0].constraints).toContainEqual(['orderBy', 'order'])
    expect(mocks.getDocs.mock.calls[0][0].constraints).toContainEqual(['limit', 1])
  })

  it('resolves the destination first task for menu moves to an unloaded column', async () => {
    act(() => { renderer = create(<Harness />) })
    mocks.getDocs.mockResolvedValueOnce({ docs: [{ data: () => ({ order: -20 }) }] })
    await board.moveTask('task', 'destination')
    expect(mocks.updateDoc).toHaveBeenCalledWith('users/user/boards/board/tasks/task', {
      columnId: 'destination', order: -21, updatedAt: 'timestamp',
    })
  })
})
