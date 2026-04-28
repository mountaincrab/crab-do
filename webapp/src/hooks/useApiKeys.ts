import { useEffect, useState } from 'react'
import {
  collection, onSnapshot, writeBatch, doc,
} from 'firebase/firestore'
import { db } from '../firebase'
import { ApiKeyMeta } from '../types'

async function sha256Hex(data: Uint8Array): Promise<string> {
  const hashBuffer = await crypto.subtle.digest('SHA-256', data.buffer as ArrayBuffer)
  return Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

function randomHex(bytes: Uint8Array): string {
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

export function useApiKeys(userId: string) {
  const [keys, setKeys] = useState<ApiKeyMeta[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const q = collection(db, 'users', userId, 'apiKeyMeta')
    const unsub = onSnapshot(q, (snap) => {
      const list = snap.docs
        .map((d) => ({ id: d.id, ...d.data() } as ApiKeyMeta))
        .sort((a, b) => b.createdAt - a.createdAt)
      setKeys(list)
      setLoading(false)
    })
    return unsub
  }, [userId])

  const generateKey = async (name: string): Promise<string> => {
    const rawBytes = new Uint8Array(32)
    crypto.getRandomValues(rawBytes)
    const rawHex = randomHex(rawBytes)
    const keyHash = await sha256Hex(rawBytes)

    const metaId = crypto.randomUUID()
    const batch = writeBatch(db)
    batch.set(doc(db, 'apiKeys', keyHash), { userId })
    batch.set(doc(db, 'users', userId, 'apiKeyMeta', metaId), {
      name,
      createdAt: Date.now(),
      keyHash,
    })
    await batch.commit()

    return rawHex
  }

  const revokeKey = async (keyHash: string, metaDocId: string): Promise<void> => {
    const batch = writeBatch(db)
    batch.delete(doc(db, 'apiKeys', keyHash))
    batch.delete(doc(db, 'users', userId, 'apiKeyMeta', metaDocId))
    await batch.commit()
  }

  return { keys, loading, generateKey, revokeKey }
}
