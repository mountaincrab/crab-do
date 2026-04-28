import * as crypto from 'crypto'
import { getFirestore } from 'firebase-admin/firestore'

export function hashKey(key: string): string {
  // key is a hex-encoded raw byte string; decode to bytes before hashing
  // to match the client-side crypto.subtle.digest('SHA-256', rawBytes)
  return crypto.createHash('sha256').update(Buffer.from(key, 'hex')).digest('hex')
}

export async function resolveUserId(keyHash: string): Promise<string | null> {
  const doc = await getFirestore().collection('apiKeys').doc(keyHash).get()
  if (!doc.exists) return null
  const data = doc.data()
  return (data?.userId as string) ?? null
}
