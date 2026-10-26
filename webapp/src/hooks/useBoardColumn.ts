import { useEffect, useMemo, useRef, useState } from 'react'
import { collection, documentId, onSnapshot, orderBy, query, where } from 'firebase/firestore'
import { db } from '../firebase'
import { Subtask, Task } from '../types'
import { SubtaskCount } from './useBoard'
import { usePagedQuery } from './usePagedQuery'

export function useBoardColumn(userId: string, boardId: string, columnId: string) {
  const source = useMemo(() => query(collection(db, 'users', userId, 'boards', boardId, 'tasks'),
    where('columnId', '==', columnId), where('isDeleted', '==', false),
    orderBy('order'), orderBy(documentId())), [userId, boardId, columnId])
  const page = usePagedQuery<Task>(source)
  const [subtasksByTask, setSubtasksByTask] = useState<Record<string, Subtask[]>>({})
  const listeners = useRef(new Map<string, () => void>())
  const idsKey = JSON.stringify(page.items.map((task) => task.id).sort())

  useEffect(() => {
    const registrations = listeners.current
    setSubtasksByTask({})
    return () => { registrations.forEach((stop) => stop()); registrations.clear() }
  }, [userId, boardId, columnId])

  useEffect(() => {
    const ids = new Set<string>(JSON.parse(idsKey))
    for (const [id, stop] of listeners.current) {
      if (!ids.has(id)) { stop(); listeners.current.delete(id) }
    }
    setSubtasksByTask((prev) => Object.fromEntries(Object.entries(prev).filter(([id]) => ids.has(id))))
    for (const id of ids) {
      if (listeners.current.has(id)) continue
      let active = true
      const stop = onSnapshot(query(collection(db, 'users', userId, 'boards', boardId, 'tasks', id, 'subtasks'),
        where('isDeleted', '==', false)), (snap) => {
        if (!active) return
        const subs = snap.docs.map((doc) => ({ ...doc.data(), id: doc.id } as Subtask))
          .sort((a, b) => a.order - b.order)
        setSubtasksByTask((prev) => ({ ...prev, [id]: subs }))
      })
      listeners.current.set(id, () => { active = false; stop() })
    }
  }, [userId, boardId, columnId, idsKey])

  const subtaskCounts: Record<string, SubtaskCount> = Object.fromEntries(
    Object.entries(subtasksByTask).map(([id, subs]) => [id, {
      total: subs.length, completed: subs.filter((sub) => sub.isCompleted).length,
    }]),
  )
  return { page, subtaskCounts, subtasksByTask }
}
