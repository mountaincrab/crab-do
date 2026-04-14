import { useEffect, useState } from 'react'
import {
  collection, doc, query, where, onSnapshot,
  addDoc, updateDoc, serverTimestamp,
} from 'firebase/firestore'
import { db } from '../firebase'
import { Reminder } from '../types'

export function useReminders(userId: string) {
  const [reminders, setReminders] = useState<Reminder[]>([])
  const [completedReminders, setCompletedReminders] = useState<Reminder[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const q = query(
      collection(db, 'users', userId, 'reminders'),
      where('isDeleted', '==', false),
    )
    return onSnapshot(q, (snap) => {
      const all = snap.docs.map((d) => ({ id: d.id, ...d.data() } as Reminder))
      setReminders(
        all.filter((r) => !r.isCompleted).sort((a, b) => a.nextTriggerMillis - b.nextTriggerMillis),
      )
      setCompletedReminders(
        all.filter((r) => r.isCompleted).sort((a, b) => (b.completedAt ?? 0) - (a.completedAt ?? 0)),
      )
      setLoading(false)
    })
  }, [userId])

  const createReminder = async (
    title: string,
    nextTriggerMillis: number,
    style: 'ALARM' | 'NOTIFICATION',
  ) => {
    await addDoc(collection(db, 'users', userId, 'reminders'), {
      userId,
      title,
      nextTriggerMillis,
      reminderStyle: style,
      recurrenceRuleJson: null,
      isEnabled: true,
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
    nextTriggerMillis: number,
    style: 'ALARM' | 'NOTIFICATION',
  ) => {
    await updateDoc(doc(db, 'users', userId, 'reminders', reminderId), {
      title,
      nextTriggerMillis,
      reminderStyle: style,
      isEnabled: true,
      updatedAt: serverTimestamp(),
    })
  }

  const deleteReminder = async (reminderId: string) => {
    await updateDoc(doc(db, 'users', userId, 'reminders', reminderId), {
      isDeleted: true,
      updatedAt: serverTimestamp(),
    })
  }

  return { reminders, completedReminders, loading, createReminder, updateReminder, deleteReminder }
}
