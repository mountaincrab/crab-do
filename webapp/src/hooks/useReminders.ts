import { useEffect, useMemo, useState } from 'react'
import {
  collection, doc, query, onSnapshot, where, orderBy, documentId,
  addDoc, updateDoc, serverTimestamp,
} from 'firebase/firestore'
import { db } from '../firebase'
import { usePagedQuery } from './usePagedQuery'
import { Reminder, RecurringReminder } from '../types'

export function useReminders(userId: string, history: { completed: boolean; deleted: boolean; deletedRecurring: boolean }) {
  const [reminders, setReminders] = useState<Reminder[]>([])
  const [recurringReminders, setRecurringReminders] = useState<RecurringReminder[]>([])
  const [loading, setLoading] = useState(true)

  const [error, setError] = useState<string>()
  const historyQueries = useMemo(() => ({
    completed: query(collection(db, 'users', userId, 'reminders'),
      where('isDeleted', '==', false), where('isCompleted', '==', true),
      orderBy('completedAt', 'desc'), orderBy(documentId(), 'desc')),
    deleted: query(collection(db, 'users', userId, 'reminders'),
      where('isDeleted', '==', true), orderBy('updatedAt', 'desc'), orderBy(documentId(), 'desc')),
    deletedRecurring: query(collection(db, 'users', userId, 'recurringReminders'),
      where('isDeleted', '==', true), orderBy('updatedAt', 'desc'), orderBy(documentId(), 'desc')),
  }), [userId])
  const completedPage = usePagedQuery<Reminder>(historyQueries.completed, history.completed)
  const deletedPage = usePagedQuery<Reminder>(historyQueries.deleted, history.deleted)
  const deletedRecurringPage = usePagedQuery<RecurringReminder>(historyQueries.deletedRecurring, history.deletedRecurring)

  useEffect(() => {
    setLoading(true)
    setError(undefined)
    setReminders([])
    setRecurringReminders([])
    let active = true
    let ready = 0
    const onReady = () => { if (++ready === 2) setLoading(false) }
    const onError = (err: Error) => { if (active) { setError(err.message); setLoading(false) } }
    let oneOffReady = false
    let recurringReady = false
    const unsub1 = onSnapshot(query(collection(db, 'users', userId, 'reminders'),
      where('isDeleted', '==', false), where('isCompleted', '==', false)), (snap) => {
      if (!active) return
      setReminders(snap.docs.map((d) => ({ ...d.data(), id: d.id } as Reminder))
        .sort((a, b) => a.scheduledAt - b.scheduledAt))
      if (!oneOffReady) { oneOffReady = true; onReady() }
    }, onError)
    const unsub2 = onSnapshot(query(collection(db, 'users', userId, 'recurringReminders'),
      where('isDeleted', '==', false)), (snap) => {
      if (!active) return
      setRecurringReminders(snap.docs.map((d) => ({ ...d.data(), id: d.id } as RecurringReminder))
        .sort((a, b) => a.nextFireAt - b.nextFireAt))
      if (!recurringReady) { recurringReady = true; onReady() }
    }, onError)
    return () => { active = false; unsub1(); unsub2() }
  }, [userId])

  const createReminder = async (
    title: string,
    scheduledAt: number,
    style: 'ALARM' | 'NOTIFICATION',
  ) => {
    await addDoc(collection(db, 'users', userId, 'reminders'), {
      userId,
      title,
      scheduledAt,
      reminderStyle: style,
      snoozedUntilMillis: null,
      isCompleted: false,
      completedAt: null,
      createdAt: Date.now(),
      updatedAt: serverTimestamp(),
      isDeleted: false,
    })
  }

  const updateReminder = async (
    reminderId: string,
    title: string,
    scheduledAt: number,
    style: 'ALARM' | 'NOTIFICATION',
  ) => {
    await updateDoc(doc(db, 'users', userId, 'reminders', reminderId), {
      title,
      scheduledAt,
      reminderStyle: style,
      updatedAt: serverTimestamp(),
    })
  }

  const deleteReminder = async (reminderId: string) => {
    await updateDoc(doc(db, 'users', userId, 'reminders', reminderId), {
      isDeleted: true,
      updatedAt: serverTimestamp(),
    })
  }

  const restoreReminder = async (reminderId: string) => {
    await updateDoc(doc(db, 'users', userId, 'reminders', reminderId), {
      isDeleted: false,
      updatedAt: serverTimestamp(),
    })
  }

  const deleteRecurringReminder = async (reminderId: string) => {
    await updateDoc(doc(db, 'users', userId, 'recurringReminders', reminderId), {
      isDeleted: true,
      updatedAt: serverTimestamp(),
    })
  }

  const restoreRecurringReminder = async (reminderId: string) => {
    await updateDoc(doc(db, 'users', userId, 'recurringReminders', reminderId), {
      isDeleted: false,
      updatedAt: serverTimestamp(),
    })
  }

  const toggleRecurringEnabled = async (reminder: RecurringReminder) => {
    await updateDoc(doc(db, 'users', userId, 'recurringReminders', reminder.id), {
      isEnabled: !reminder.isEnabled,
      updatedAt: serverTimestamp(),
    })
  }

  return {
    reminders,
    completedReminders: completedPage.items,
    completedPage,
    deletedReminders: deletedPage.items,
    deletedPage,
    recurringReminders,
    deletedRecurring: deletedRecurringPage.items,
    deletedRecurringPage,
    loading,
    error,
    createReminder,
    updateReminder,
    deleteReminder,
    restoreReminder,
    deleteRecurringReminder,
    restoreRecurringReminder,
    toggleRecurringEnabled,
  }
}
