import { useEffect, useState } from 'react'
import {
  collection, doc, query, where, onSnapshot,
  addDoc, updateDoc, serverTimestamp,
  getDocs, writeBatch,
} from 'firebase/firestore'
import { db } from '../firebase'
import { Board, Column, Task } from '../types'

export function useBoard(userId: string, boardId: string) {
  const [board, setBoard] = useState<Board | null>(null)
  const [columns, setColumns] = useState<Column[]>([])
  const [tasks, setTasks] = useState<Task[]>([])
  const [loading, setLoading] = useState(true)

  const boardRef = doc(db, 'users', userId, 'boards', boardId)

  useEffect(() => {
    return onSnapshot(boardRef, (snap) => {
      if (snap.exists()) setBoard({ id: snap.id, ...snap.data() } as Board)
    })
  }, [userId, boardId])

  useEffect(() => {
    const q = query(
      collection(db, 'users', userId, 'boards', boardId, 'columns'),
      where('isDeleted', '==', false),
    )
    return onSnapshot(q, (snap) => {
      const cols = snap.docs
        .map((d) => ({ id: d.id, ...d.data() } as Column))
        .sort((a, b) => a.order - b.order)
      setColumns(cols)
    })
  }, [userId, boardId])

  useEffect(() => {
    const q = query(
      collection(db, 'users', userId, 'boards', boardId, 'tasks'),
      where('isDeleted', '==', false),
    )
    return onSnapshot(q, (snap) => {
      const ts = snap.docs
        .map((d) => ({ id: d.id, ...d.data() } as Task))
        .sort((a, b) => a.order - b.order)
      setTasks(ts)
      setLoading(false)
    })
  }, [userId, boardId])

  const tasksByColumn = columns.reduce<Record<string, Task[]>>((acc, col) => {
    acc[col.id] = tasks.filter((t) => t.columnId === col.id)
    return acc
  }, {})

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
    // Soft-delete the column and all its tasks
    const batch = writeBatch(db)
    batch.update(
      doc(db, 'users', userId, 'boards', boardId, 'columns', columnId),
      { isDeleted: true, updatedAt: serverTimestamp() },
    )
    tasks
      .filter((t) => t.columnId === columnId)
      .forEach((t) => {
        batch.update(
          doc(db, 'users', userId, 'boards', boardId, 'tasks', t.id),
          { isDeleted: true, updatedAt: serverTimestamp() },
        )
      })
    await batch.commit()
  }

  const addTask = async (columnId: string, title: string, description = '') => {
    const colTasks = tasks.filter((t) => t.columnId === columnId)
    const maxOrder = colTasks.length > 0 ? Math.max(...colTasks.map((t) => t.order)) : 0
    await addDoc(collection(db, 'users', userId, 'boards', boardId, 'tasks'), {
      boardId,
      columnId,
      title,
      description,
      order: maxOrder + 1,
      updatedAt: serverTimestamp(),
      isDeleted: false,
    })
  }

  const moveTask = async (taskId: string, targetColumnId: string, newOrder: number) => {
    await updateDoc(
      doc(db, 'users', userId, 'boards', boardId, 'tasks', taskId),
      { columnId: targetColumnId, order: newOrder, updatedAt: serverTimestamp() },
    )
  }

  const deleteTask = async (taskId: string) => {
    // Soft-delete task and its subtasks
    const batch = writeBatch(db)
    batch.update(
      doc(db, 'users', userId, 'boards', boardId, 'tasks', taskId),
      { isDeleted: true, updatedAt: serverTimestamp() },
    )
    const subtasksSnap = await getDocs(
      collection(db, 'users', userId, 'boards', boardId, 'tasks', taskId, 'subtasks'),
    )
    subtasksSnap.docs.forEach((d) => {
      batch.update(d.ref, { isDeleted: true, updatedAt: serverTimestamp() })
    })
    await batch.commit()
  }

  return {
    board,
    columns,
    tasks,
    tasksByColumn,
    loading,
    addColumn,
    renameColumn,
    deleteColumn,
    addTask,
    moveTask,
    deleteTask,
  }
}
