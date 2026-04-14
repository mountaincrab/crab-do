import { useEffect, useState } from 'react'
import {
  collection, query, where, onSnapshot,
  addDoc, serverTimestamp, orderBy,
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
      orderBy('createdAt', 'desc'),
    )
    return onSnapshot(q, (snap) => {
      setBoards(snap.docs.map((d) => ({ id: d.id, ...d.data() } as Board)))
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
