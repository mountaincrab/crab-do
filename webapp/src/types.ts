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
  nextTriggerMillis: number
  reminderStyle: 'ALARM' | 'NOTIFICATION'
  recurrenceRuleJson: string | null
  isEnabled: boolean
  isCompleted: boolean
  completedAt: number | null
  createdAt: number
  updatedAt: number
  isDeleted: boolean
}
