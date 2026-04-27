# Crab Do — Claude guidance

## Project structure

This repo contains **two separate apps** sharing a Firestore backend:

| Directory | What it is |
|-----------|-----------|
| `app/` | Android app (Kotlin Multiplatform + Jetpack Compose) |
| `webapp/` | Web app (React + TypeScript + Tailwind + Vite) |

When making changes that affect shared data (Firestore schema, subtask fields, ordering logic, etc.) always check and update **both** apps.

## Android app (`app/`)

**Stack:** Kotlin Multiplatform, Jetpack Compose, Room (local DB), Firestore (sync), Koin (DI)

**Build:**
```bash
./gradlew :app:assembleDebug
```

**Install on device:**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Key source directories:**
- `app/src/commonMain/` — shared data models, Room DAOs/entities
- `app/src/androidMain/kotlin/com/mountaincrab/crabdo/`
  - `ui/boards/` — Kanban board, task detail screen + ViewModel
  - `ui/boards/components/` — KanbanColumn, TaskCard, SubtaskItem, AddCardDialog
  - `ui/reminders/` — reminders screens
  - `ui/settings/` — settings + theme selection
  - `ui/theme/Theme.kt` — all colour themes (add new themes here)
  - `data/repository/` — TaskRepository, SubtaskRepository, etc.
  - `data/remote/SyncWorker.kt` — Firestore sync

**Themes** are defined in `ui/theme/Theme.kt`. Each theme needs an entry in: `AppTheme` enum, `buildScheme`/custom scheme, `paletteFor()`, `CrabbanTheme` when block, and the `ThemeSwatch` in `SettingsScreen.kt`.

## Web app (`webapp/`)

**Stack:** React 18, TypeScript, Tailwind CSS, Vite, Firebase SDK v10

**Dev server:**
```bash
cd webapp && npm run dev
```

**Type check:**
```bash
cd webapp && npx tsc --noEmit
```

**Build:**
```bash
cd webapp && npm run build
```

**Key source files:**
- `src/pages/KanbanBoardPage.tsx` — board view with drag-and-drop task cards
- `src/pages/TaskDetailPage.tsx` — task detail with subtask drag-and-drop + rename
- `src/hooks/useBoard.ts` — board/column/task Firestore hooks
- `src/hooks/useTask.ts` — task + subtask Firestore hooks
- `src/types.ts` — shared TypeScript types

## Keeping both apps in sync

Any change to **shared data** must be reflected in both apps. Examples:
- New Firestore field → update both `FirestoreMappers.kt` (read+write) **and** `webapp/src/types.ts` + relevant hook
- New display state (e.g. snooze, enable/disable) → update both `ReminderItem.kt` **and** `RemindersPage.tsx`
- New Firestore collection → update both `SyncWorker.kt` **and** the relevant webapp hook
- Field name divergence between apps → always write **both** field names and read with fallback

**What the webapp does NOT need to match:**
- Android alarm/notification mechanics (AlarmScheduler, BroadcastReceiver, etc.)
- Room DB schema (local-only)
- RecurrenceEngine computation (next-fire calculation is Android-only)
- Creating recurring reminders (Android-only; webapp is read/display only for recurring)

## Firestore data model

```
users/{userId}/
  boards/{boardId}/
    columns/{columnId}
    tasks/{taskId}/
      subtasks/{subtaskId}
  reminders/{reminderId}          ← one-off reminders
  recurringReminders/{reminderId} ← recurring reminders
```

**One-off reminder fields:** `userId`, `title`, `scheduledAt`, `nextTriggerMillis` (alias), `reminderStyle`, `isEnabled`, `snoozedUntilMillis`, `isCompleted`, `completedAt`, `createdAt`, `updatedAt`, `isDeleted`.

**Recurring reminder fields:** `userId`, `title`, `recurrenceRuleJson`, `startDate`, `reminderTime` ("HH:mm"), `nextFireAt`, `reminderStyle`, `isEnabled`, `snoozedUntilMillis`, `createdAt`, `updatedAt`, `isDeleted`.

**Subtask fields:** `id`, `taskId`, `title`, `isCompleted`, `order` (Double), `updatedAt`, `isDeleted`.

Ordering uses midpoint arithmetic: `newOrder = (orderBefore + orderAfter) / 2`. If `orderAfter <= orderBefore`, use `orderBefore + 1`.
