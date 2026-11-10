import { useEffect, useState } from 'react'
import { collection, doc, onSnapshot, query, serverTimestamp, updateDoc, where } from 'firebase/firestore'
import { db } from '../firebase'
import { clearedTaskReminder } from '../lib/softDeletePages'
import { Column, Task } from '../types'
import { useBoards } from './useBoards'

export interface TaskReminderSummary {
  task: Task
  boardTitle: string
  columnTitle: string
}

export function useTaskReminders(userId: string) {
  const { boards, loading: boardsLoading } = useBoards(userId)
  const [byBoard, setByBoard] = useState<Record<string, TaskReminderSummary[]>>({})

  useEffect(() => {
    setByBoard({})
    const unsubs = boards.flatMap((board) => {
      let tasks: Task[] | null = null
      let columns: Column[] | null = null
      const publish = () => {
        if (!tasks || !columns) return
        const summaries = tasks.flatMap((task) => {
          const column = columns!.find((c) => c.id === task.columnId)
          return column && (task.snoozedUntilMillis != null || task.reminderTimeMillis != null)
            ? [{ task, boardTitle: board.title, columnTitle: column.title }]
            : []
        })
        setByBoard((prev) => ({ ...prev, [board.id]: summaries }))
      }
      return [
        onSnapshot(query(
          collection(db, 'users', userId, 'boards', board.id, 'tasks'),
          where('isDeleted', '==', false),
        ), (snap) => {
          tasks = snap.docs.map((d) => ({ ...d.data(), id: d.id, boardId: board.id } as Task))
          publish()
        }),
        onSnapshot(query(
          collection(db, 'users', userId, 'boards', board.id, 'columns'),
          where('isDeleted', '==', false),
        ), (snap) => {
          columns = snap.docs.map((d) => ({ ...d.data(), id: d.id } as Column))
          publish()
        }),
      ]
    })
    return () => unsubs.forEach((unsub) => unsub())
  }, [userId, boards])

  const taskReminders = boards.flatMap((board) => byBoard[board.id] ?? [])
    .sort((a, b) =>
      (a.task.snoozedUntilMillis ?? a.task.reminderTimeMillis!) -
      (b.task.snoozedUntilMillis ?? b.task.reminderTimeMillis!) || a.task.id.localeCompare(b.task.id),
    )

  const clearTaskReminder = async (task: Task) => {
    await updateDoc(doc(db, 'users', userId, 'boards', task.boardId, 'tasks', task.id), {
      ...clearedTaskReminder,
      updatedAt: serverTimestamp(),
    })
  }

  return {
    taskReminders,
    loading: boardsLoading || boards.some((board) => !(board.id in byBoard)),
    clearTaskReminder,
  }
}
