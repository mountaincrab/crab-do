import { useEffect, useState } from 'react'
import {
  collection, doc, query, where, onSnapshot,
  addDoc, serverTimestamp, getDocs, writeBatch,
} from 'firebase/firestore'
import { db } from '../firebase'
import { Board } from '../types'

export function useBoards(userId: string) {
  const [boards, setBoards] = useState<Board[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const q = query(
      collection(db, 'users', userId, 'boards'),
      where('isDeleted', '==', false),
    )
    return onSnapshot(q, (snap) => {
      const sorted = snap.docs
        .map((d) => ({ id: d.id, ...d.data() } as Board))
        .sort((a, b) => b.createdAt - a.createdAt)
      setBoards(sorted)
      setLoading(false)
    })
  }, [userId])

  const createBoard = async (title: string) => {
    await addDoc(collection(db, 'users', userId, 'boards'), {
      userId,
      title,
      columnOrder: '[]',
      createdAt: Date.now(),
      updatedAt: serverTimestamp(),
      isShared: false,
      isDeleted: false,
    })
  }

  const deleteBoard = async (boardId: string) => {
    const batch = writeBatch(db)
    batch.update(doc(db, 'users', userId, 'boards', boardId), {
      isDeleted: true,
      updatedAt: serverTimestamp(),
    })
    const colsSnap = await getDocs(
      query(collection(db, 'users', userId, 'boards', boardId, 'columns'), where('isDeleted', '==', false)),
    )
    colsSnap.docs.forEach((d) => batch.update(d.ref, { isDeleted: true, updatedAt: serverTimestamp() }))
    const tasksSnap = await getDocs(
      query(collection(db, 'users', userId, 'boards', boardId, 'tasks'), where('isDeleted', '==', false)),
    )
    tasksSnap.docs.forEach((d) => batch.update(d.ref, { isDeleted: true, updatedAt: serverTimestamp() }))
    await batch.commit()
  }

  return { boards, loading, createBoard, deleteBoard }
}
