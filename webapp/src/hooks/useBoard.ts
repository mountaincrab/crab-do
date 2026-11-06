import { useEffect, useState } from 'react'
import {
  collection, doc, query, where, onSnapshot,
  addDoc, updateDoc, serverTimestamp,
  getDocsFromServer, writeBatch, orderBy, limit,
} from 'firebase/firestore'
import { db } from '../firebase'
import { clearedTaskReminder, softDeletePages } from '../lib/softDeletePages'
import { Board, Column } from '../types'
import { NewTaskDraft } from '../components/NewTaskModal'

export interface SubtaskCount {
  total: number
  completed: number
}

export function useBoard(userId: string, boardId: string) {
  const [board, setBoard] = useState<Board | null>(null)
  const [columns, setColumns] = useState<Column[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>()

  const boardRef = doc(db, 'users', userId, 'boards', boardId)

  useEffect(() => {
    return onSnapshot(boardRef, (snap) => {
      if (snap.exists()) setBoard({ id: snap.id, ...snap.data() } as Board)
    })
  }, [userId, boardId])

  useEffect(() => {
    setLoading(true)
    setError(undefined)
    setColumns([])
    const q = query(
      collection(db, 'users', userId, 'boards', boardId, 'columns'),
      where('isDeleted', '==', false),
    )
    return onSnapshot(q, (snap) => {
      const cols = snap.docs
        .map((d) => ({ id: d.id, ...d.data() } as Column))
        .sort((a, b) => a.order - b.order)
      setColumns(cols)
      setLoading(false)
    }, (err) => { setError(err.message); setLoading(false) })
  }, [userId, boardId])

  const addColumn = async (title: string) => {
    const maxOrder = columns.length > 0 ? Math.max(...columns.map((c) => c.order)) : 0
    await addDoc(collection(db, 'users', userId, 'boards', boardId, 'columns'), {
      boardId,
      title,
      order: maxOrder + 1,
      updatedAt: serverTimestamp(),
      isDeleted: false,
    })
  }

  const renameColumn = async (columnId: string, title: string) => {
    await updateDoc(
      doc(db, 'users', userId, 'boards', boardId, 'columns', columnId),
      { title, updatedAt: serverTimestamp() },
    )
  }

  const deleteColumn = async (columnId: string) => {
    await softDeletePages(query(collection(db, 'users', userId, 'boards', boardId, 'tasks'),
      where('columnId', '==', columnId), where('isDeleted', '==', false)), clearedTaskReminder)
    await updateDoc(doc(db, 'users', userId, 'boards', boardId, 'columns', columnId),
      { isDeleted: true, updatedAt: serverTimestamp() })
  }

  /**
   * Creates a task and its checklist items in one batch, so a task only ever
   * reaches Firestore once the user has committed to it (see NewTaskModal).
   */
  const createTask = async (columnId: string, draft: NewTaskDraft) => {
    const first = await getDocsFromServer(query(collection(db, 'users', userId, 'boards', boardId, 'tasks'),
      where('columnId', '==', columnId), where('isDeleted', '==', false), orderBy('order'), limit(1)))
    const minOrder = first.docs[0]?.data().order
    const newOrder = minOrder == null ? 1 : minOrder - 1
    const taskRef = doc(collection(db, 'users', userId, 'boards', boardId, 'tasks'))
    const batch = writeBatch(db)
    batch.set(taskRef, {
      boardId,
      columnId,
      title: draft.title,
      description: draft.description,
      order: newOrder,
      reminderTimeMillis: draft.reminderTimeMillis,
      snoozedUntilMillis: null,
      reminderStyle: draft.reminderStyle,
      reminderTransitionColumnId: draft.reminderTimeMillis != null ? draft.reminderTransitionColumnId : null,
      updatedAt: serverTimestamp(),
      isDeleted: false,
    })
    draft.subtasks.forEach((s, i) => {
      batch.set(doc(collection(taskRef, 'subtasks')), {
        taskId: taskRef.id,
        title: s.title,
        isCompleted: s.isCompleted,
        order: i + 1,
        updatedAt: serverTimestamp(),
        isDeleted: false,
      })
    })
    await batch.commit()
    return taskRef.id
  }

  const moveTask = async (taskId: string, targetColumnId: string, newOrder?: number) => {
    if (newOrder === undefined) {
      const first = await getDocsFromServer(query(collection(db, 'users', userId, 'boards', boardId, 'tasks'),
        where('columnId', '==', targetColumnId), where('isDeleted', '==', false), orderBy('order'), limit(1)))
      newOrder = (first.docs[0]?.data().order ?? 1) - 1
    }
    await updateDoc(
      doc(db, 'users', userId, 'boards', boardId, 'tasks', taskId),
      { columnId: targetColumnId, order: newOrder, updatedAt: serverTimestamp() },
    )
  }

  const toggleSubtask = async (taskId: string, subtaskId: string, isCompleted: boolean) => {
    await updateDoc(
      doc(db, 'users', userId, 'boards', boardId, 'tasks', taskId, 'subtasks', subtaskId),
      { isCompleted, updatedAt: serverTimestamp() },
    )
  }

  const reorderColumns = async (orderedIds: string[]) => {
    const batch = writeBatch(db)
    orderedIds.forEach((id, i) => {
      batch.update(
        doc(db, 'users', userId, 'boards', boardId, 'columns', id),
        { order: i + 1, updatedAt: serverTimestamp() },
      )
    })
    await batch.commit()
  }

  const deleteTask = async (taskId: string) => {
    await softDeletePages(query(collection(db, 'users', userId, 'boards', boardId, 'tasks', taskId, 'subtasks'),
      where('isDeleted', '==', false)))
    await updateDoc(doc(db, 'users', userId, 'boards', boardId, 'tasks', taskId), {
      ...clearedTaskReminder, isDeleted: true, updatedAt: serverTimestamp(),
    })
  }

  return {
    board,
    columns,
    loading,
    error,
    addColumn,
    renameColumn,
    deleteColumn,
    reorderColumns,
    createTask,
    moveTask,
    deleteTask,
    toggleSubtask,
  }
}
