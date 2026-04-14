import { useEffect, useState } from 'react'
import {
  collection, query, where, onSnapshot,
  addDoc, serverTimestamp,
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

  return { boards, loading, createBoard }
}
