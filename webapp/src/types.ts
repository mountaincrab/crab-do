export interface Board {
  id: string
  userId: string
  title: string
  columnOrder: string  // JSON-encoded string[] of column IDs
  createdAt: number
  updatedAt: number
  isShared: boolean
  isDeleted: boolean
}

export interface Column {
  id: string
  boardId: string
  title: string
  order: number
  updatedAt: number
  isDeleted: boolean
}

export interface Task {
  id: string
  boardId: string
  columnId: string
  title: string
  description: string
  order: number
  updatedAt: number
  isDeleted: boolean
}

export interface Subtask {
  id: string
  taskId: string
  title: string
  isCompleted: boolean
  order: number
  updatedAt: number
  isDeleted: boolean
}

export interface Reminder {
  id: string
  userId: string
  title: string
  scheduledAt: number
  reminderStyle: 'ALARM' | 'NOTIFICATION'
  isEnabled: boolean
  snoozedUntilMillis: number | null
  isCompleted: boolean
  completedAt: number | null
  createdAt: number
  updatedAt: number
  isDeleted: boolean
}

export interface ApiKeyMeta {
  id: string        // Firestore doc id (UUID)
  name: string
  createdAt: number
  keyHash: string   // SHA-256 hex; FK to apiKeys/{keyHash}
}

export interface RecurringReminder {
  id: string
  userId: string
  title: string
  recurrenceRuleJson: string
  reminderTime: string  // "HH:mm"
  nextFireAt: number
  reminderStyle: 'ALARM' | 'NOTIFICATION'
  isEnabled: boolean
  snoozedUntilMillis: number | null
  createdAt: number
  updatedAt: number
  isDeleted: boolean
}
