import { onRequest } from 'firebase-functions/v2/https'
import { getFirestore, FieldValue } from 'firebase-admin/firestore'
import { hashKey, resolveUserId } from './utils/auth'
import { computeNextFireAt } from './utils/recurrence'

type ReminderStyle = 'ALARM' | 'NOTIFICATION'
type RecurrenceType = 'DAILY' | 'WEEKLY' | 'EVERY_N_DAYS' | 'MONTHLY'

interface RecurrenceRule {
  type: RecurrenceType
  interval: number
  daysOfWeek: number[]
  dayOfMonth: number
}

interface OneOffBody {
  type: 'one_off'
  title: string
  scheduledAt: number
  reminderStyle: ReminderStyle
  isEnabled?: boolean
}

interface RecurringBody {
  type: 'recurring'
  title: string
  recurrenceRule: RecurrenceRule
  startDate: number
  reminderTime: string
  reminderStyle: ReminderStyle
  isEnabled?: boolean
}

type RequestBody = OneOffBody | RecurringBody

function validateOneOff(b: OneOffBody): string | null {
  if (typeof b.title !== 'string' || !b.title.trim()) return 'title is required'
  if (typeof b.scheduledAt !== 'number') return 'scheduledAt must be a number (millis)'
  if (b.reminderStyle !== 'ALARM' && b.reminderStyle !== 'NOTIFICATION') {
    return 'reminderStyle must be ALARM or NOTIFICATION'
  }
  return null
}

function validateRecurring(b: RecurringBody): string | null {
  if (typeof b.title !== 'string' || !b.title.trim()) return 'title is required'
  if (typeof b.startDate !== 'number') return 'startDate must be a number (millis)'
  if (typeof b.reminderTime !== 'string' || !/^\d{2}:\d{2}$/.test(b.reminderTime)) {
    return 'reminderTime must be HH:mm'
  }
  if (b.reminderStyle !== 'ALARM' && b.reminderStyle !== 'NOTIFICATION') {
    return 'reminderStyle must be ALARM or NOTIFICATION'
  }
  const rule = b.recurrenceRule
  if (!rule || typeof rule !== 'object') return 'recurrenceRule is required'
  const validTypes: RecurrenceType[] = ['DAILY', 'WEEKLY', 'EVERY_N_DAYS', 'MONTHLY']
  if (!validTypes.includes(rule.type)) return `recurrenceRule.type must be one of ${validTypes.join(', ')}`
  if (typeof rule.interval !== 'number' || rule.interval < 1) return 'recurrenceRule.interval must be >= 1'
  return null
}

export const createReminder = onRequest({ cors: true }, async (req, res) => {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method Not Allowed' })
    return
  }

  const apiKey = req.headers['x-api-key']
  if (!apiKey || typeof apiKey !== 'string') {
    res.status(401).json({ error: 'Missing x-api-key header' })
    return
  }

  const keyHash = hashKey(apiKey)
  const userId = await resolveUserId(keyHash)
  if (!userId) {
    res.status(401).json({ error: 'Invalid API key' })
    return
  }

  const body = req.body as RequestBody
  if (!body || !body.type) {
    res.status(400).json({ error: 'Request body must include a type field' })
    return
  }

  const db = getFirestore()

  if (body.type === 'one_off') {
    const err = validateOneOff(body)
    if (err) { res.status(400).json({ error: err }); return }

    const docRef = await db.collection('users').doc(userId).collection('reminders').add({
      userId,
      title: body.title.trim(),
      scheduledAt: body.scheduledAt,
      reminderStyle: body.reminderStyle,
      isEnabled: body.isEnabled ?? true,
      snoozedUntilMillis: null,
      isCompleted: false,
      completedAt: null,
      createdAt: Date.now(),
      updatedAt: FieldValue.serverTimestamp(),
      isDeleted: false,
    })
    res.status(201).json({ id: docRef.id })
    return
  }

  if (body.type === 'recurring') {
    const err = validateRecurring(body)
    if (err) { res.status(400).json({ error: err }); return }

    const nextFireAt = computeNextFireAt(body.startDate, body.reminderTime)
    const docRef = await db.collection('users').doc(userId).collection('recurringReminders').add({
      userId,
      title: body.title.trim(),
      recurrenceRuleJson: JSON.stringify(body.recurrenceRule),
      startDate: body.startDate,
      reminderTime: body.reminderTime,
      nextFireAt,
      reminderStyle: body.reminderStyle,
      isEnabled: body.isEnabled ?? true,
      snoozedUntilMillis: null,
      createdAt: Date.now(),
      updatedAt: FieldValue.serverTimestamp(),
      isDeleted: false,
    })
    res.status(201).json({ id: docRef.id })
    return
  }

  res.status(400).json({ error: 'type must be one_off or recurring' })
})
