import { useEffect, useState } from 'react'
import {
  doc, collection, query, where, onSnapshot,
  updateDoc, addDoc, serverTimestamp,
} from 'firebase/firestore'
import { db } from '../firebase'
import { Task, Subtask } from '../types'

export function useTask(userId: string, boardId: string, taskId: string) {
  const [task, setTask] = useState<Task | null>(null)
  const [subtasks, setSubtasks] = useState<Subtask[]>([])

  useEffect(() => {
    return onSnapshot(
      doc(db, 'users', userId, 'boards', boardId, 'tasks', taskId),
      (snap) => { if (snap.exists()) setTask({ id: snap.id, ...snap.data() } as Task) },
    )
  }, [userId, boardId, taskId])

  useEffect(() => {
    const q = query(
      collection(db, 'users', userId, 'boards', boardId, 'tasks', taskId, 'subtasks'),
      where('isDeleted', '==', false),
    )
    return onSnapshot(q, (snap) => {
      setSubtasks(
        snap.docs
          .map((d) => ({ id: d.id, ...d.data() } as Subtask))
          .sort((a, b) => a.order - b.order),
      )
    })
  }, [userId, boardId, taskId])

  const updateTask = async (fields: Partial<Pick<Task, 'title' | 'description'>>) => {
    await updateDoc(
      doc(db, 'users', userId, 'boards', boardId, 'tasks', taskId),
      { ...fields, updatedAt: serverTimestamp() },
    )
  }

  const addSubtask = async (title: string) => {
    const maxOrder = subtasks.length > 0 ? Math.max(...subtasks.map((s) => s.order)) : 0
    await addDoc(
      collection(db, 'users', userId, 'boards', boardId, 'tasks', taskId, 'subtasks'),
      {
        taskId,
        title,
        isCompleted: false,
        order: maxOrder + 1,
        updatedAt: serverTimestamp(),
        isDeleted: false,
      },
    )
  }

  const toggleSubtask = async (subtaskId: string, isCompleted: boolean) => {
    await updateDoc(
      doc(db, 'users', userId, 'boards', boardId, 'tasks', taskId, 'subtasks', subtaskId),
      { isCompleted, updatedAt: serverTimestamp() },
    )
  }

  const deleteSubtask = async (subtaskId: string) => {
    await updateDoc(
      doc(db, 'users', userId, 'boards', boardId, 'tasks', taskId, 'subtasks', subtaskId),
      { isDeleted: true, updatedAt: serverTimestamp() },
    )
  }

  return { task, subtasks, updateTask, addSubtask, toggleSubtask, deleteSubtask }
}
