import { useEffect, useState } from 'react'
import {
  collection, doc, query, onSnapshot,
  addDoc, updateDoc, serverTimestamp,
} from 'firebase/firestore'
import { db } from '../firebase'
import { Reminder, RecurringReminder } from '../types'

export function useReminders(userId: string) {
  const [reminders, setReminders] = useState<Reminder[]>([])
  const [completedReminders, setCompletedReminders] = useState<Reminder[]>([])
  const [deletedReminders, setDeletedReminders] = useState<Reminder[]>([])
  const [recurringReminders, setRecurringReminders] = useState<RecurringReminder[]>([])
  const [deletedRecurring, setDeletedRecurring] = useState<RecurringReminder[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let oneOffReady = false
    let recurringReady = false
    const checkReady = () => { if (oneOffReady && recurringReady) setLoading(false) }

    // Listen across all docs (including deleted) so we can show a Deleted section.
    const q1 = query(collection(db, 'users', userId, 'reminders'))
    const unsub1 = onSnapshot(q1, (snap) => {
      const all = snap.docs.map((d) => ({ id: d.id, ...d.data() } as Reminder))
      const live = all.filter((r) => !r.isDeleted)
      setReminders(
        live.filter((r) => !r.isCompleted).sort((a, b) => a.scheduledAt - b.scheduledAt),
      )
      setCompletedReminders(
        live.filter((r) => r.isCompleted).sort((a, b) => (b.completedAt ?? 0) - (a.completedAt ?? 0)),
      )
      setDeletedReminders(
        all.filter((r) => r.isDeleted).sort((a, b) => b.scheduledAt - a.scheduledAt),
      )
      oneOffReady = true
      checkReady()
    })

    const q2 = query(collection(db, 'users', userId, 'recurringReminders'))
    const unsub2 = onSnapshot(q2, (snap) => {
      const all = snap.docs.map((d) => ({ id: d.id, ...d.data() } as RecurringReminder))
      setRecurringReminders(
        all.filter((r) => !r.isDeleted).sort((a, b) => a.nextFireAt - b.nextFireAt),
      )
      setDeletedRecurring(
        all.filter((r) => r.isDeleted).sort((a, b) => b.nextFireAt - a.nextFireAt),
      )
      recurringReady = true
      checkReady()
    })

    return () => { unsub1(); unsub2() }
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
    completedReminders,
    deletedReminders,
    recurringReminders,
    deletedRecurring,
    loading,
    createReminder,
    updateReminder,
    deleteReminder,
    restoreReminder,
    deleteRecurringReminder,
    restoreRecurringReminder,
    toggleRecurringEnabled,
  }
}
