import { useEffect, useState } from 'react'
import { DocumentData, Query, limit, onSnapshot, query } from 'firebase/firestore'

export const PAGE_SIZE = 50

/** A live, contiguous prefix. Growing one window avoids gaps/duplicates when
 * documents move across page boundaries while another device is editing. */
export function usePagedQuery<T>(source: Query<DocumentData>, enabled = true) {
  const [request, setRequest] = useState({ source, size: PAGE_SIZE, retry: 0 })
  const size = request.source === source ? request.size : PAGE_SIZE
  const [result, setResult] = useState<{
    source: Query<DocumentData>; size: number; items: T[]; next?: T; error?: string
  }>()

  useEffect(() => {
    if (!enabled) {
      setRequest({ source, size: PAGE_SIZE, retry: 0 })
      setResult(undefined)
      return
    }
    let active = true
    const unsubscribe = onSnapshot(query(source, limit(size + 1)), (snapshot) => {
      if (!active) return
      const rows = snapshot.docs.map((doc) => ({ ...doc.data(), id: doc.id } as T))
      setResult({ source, size, items: rows.slice(0, size), next: rows[size] })
    }, (error) => {
      if (!active) return
      setResult((previous) => ({
        source, size,
        items: previous?.source === source ? previous.items : [],
        next: previous?.source === source ? previous.next : undefined,
        error: error.message,
      }))
    })
    return () => { active = false; unsubscribe() }
  }, [source, enabled, size, request.retry])

  const current = enabled && result?.source === source ? result : undefined
  const loading = enabled && (!current || current.size !== size)
  return {
    items: current?.items ?? [],
    next: current?.next,
    hasMore: current?.next !== undefined,
    loading,
    error: current?.error,
    loadMore: () => {
      if (!loading && current?.next) setRequest({ source, size: size + PAGE_SIZE, retry: 0 })
    },
    retry: () => {
      setResult((previous) => previous?.source === source ? { ...previous, size: 0, error: undefined } : undefined)
      setRequest({ source, size, retry: request.retry + 1 })
    },
  }
}
