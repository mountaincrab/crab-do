# Crab Do — Claude guidance

## Project structure

This repo contains **three deployable surfaces** sharing a Firestore backend:

| Directory | What it is |
|-----------|-----------|
| `app/` | Android app (Kotlin Multiplatform + Jetpack Compose) |
| `webapp/` | Web app (React + TypeScript + Tailwind + Vite), deployed to Firebase Hosting |
| `functions/` | Firebase Cloud Functions (TypeScript) — public HTTP endpoints (e.g. `createReminder`) authenticated via API keys |

When making changes that affect shared data (Firestore schema, subtask fields, ordering logic, etc.) always check and update **all surfaces that touch the field** — that may include cloud functions too.

## Android app (`app/`)

**Stack:** Kotlin Multiplatform, Jetpack Compose, Room (local DB), Firestore (sync), Koin (DI), Glance (home-screen widgets), WorkManager (background sync).

**Build:**
```bash
./gradlew :app:assembleDebug
```

**Install on device:**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Compile-only check (faster than assembleDebug):**
```bash
./gradlew :app:compileDebugKotlinAndroid
```

**Key source directories:**
- `app/src/commonMain/` — shared data models, Room DAOs/entities, `AppDatabase.kt`
- `app/src/androidMain/kotlin/com/mountaincrab/crabdo/`
  - `ui/boards/` — Kanban board, task detail screen + ViewModel
  - `ui/boards/components/` — KanbanColumn, TaskCard, SubtaskItem, AddCardDialog
  - `ui/reminders/` — reminders screens + ReminderItem components
  - `ui/settings/` — settings + theme selection
  - `ui/theme/Theme.kt` — all colour themes (add new themes here)
  - `data/repository/` — TaskRepository, SubtaskRepository, ReminderRepository, BoardRepository, InvitationRepository
  - `data/remote/SyncWorker.kt` — periodic Firestore push/pull
  - `data/remote/FirestoreMappers.kt` — entity ↔ Firestore document mapping
  - `data/local/Migrations.kt` — Room migration registry (`ALL_MIGRATIONS`)
  - `alarm/` — AlarmScheduler, ReminderReceiver, AlarmRingerService, SnoozePickerActivity, BootReceiver
  - `widget/` — Glance home-screen widgets (OneOffRemindersWidget, RecurringRemindersWidget)
  - `domain/RecurrenceEngine.kt` — next-fire calculation for recurring reminders
  - `notification/` — notification channels, builders

**Themes** are defined in `ui/theme/Theme.kt`. Each theme needs an entry in: `AppTheme` enum, `buildScheme`/custom scheme, `paletteFor()`, `CrabbanTheme` when block, and the `ThemeSwatch` in `SettingsScreen.kt`.

### Sync architecture (non-obvious)

Two mechanisms run side-by-side:

1. **Real-time Firestore listener** in `ReminderRepository.startFirestoreListener` and `BoardRepository` — `addSnapshotListener` reflects remote changes into Room immediately.
2. **`SyncWorker`** (WorkManager, unique work name `"sync"`) — pushes locally-pending writes (`syncStatus = PENDING`) up to Firestore, then pulls deltas via `updatedAt > lastSyncTimestamp`.

**Critical invariant:** the Firestore listener checks `existing.syncStatus == SyncStatus.PENDING` before applying a remote document and **skips** if so. This prevents the listener from overwriting a local change before `SyncWorker` has had a chance to push it. When adding new entities/listeners, preserve this guard.

Mutation methods on repositories should:
1. Update Room with `syncStatus = SyncStatus.PENDING`.
2. Reschedule alarms (for reminders) — cancel + re-schedule based on the new state.
3. Call `enqueueSyncWork()` (replaces any in-flight sync).
4. Call `notifyWidgets()` so Glance updates.

### Alarm system (non-obvious)

`AlarmScheduler` uses `reminderId.hashCode() and 0x7FFFFFFF` as the `PendingIntent` request code — **this is shared across the original alarm AND any snooze alarm**. Snooze re-uses the slot via `FLAG_UPDATE_CURRENT`, which means scheduling a new alarm for the same reminder ID *replaces* the prior one (last-writer-wins). Don't try to track multiple alarms per reminder — schedule once with the correct fire time.

The Firestore listener also reschedules: if a remote update brings `snoozedUntilMillis` or a new `nextFireAt`, the listener computes `fireAt = snooze ?: nextFireAt/scheduledAt` and calls `scheduleReminder` (or `cancelReminder` if disabled/past). `rescheduleAllReminders` does the same on app start, plus clears stale-past `snoozedUntilMillis` values to fix `COALESCE(snoozedUntilMillis, nextFireAt)` ordering.

### Room migrations

The database is configured with `exportSchema = true` and KSP `room.schemaLocation = $projectDir/schemas`. Each `@Database(version = N)` build emits `app/schemas/com.mountaincrab.crabdo.data.local.AppDatabase/N.json`. **Commit these JSONs** — they are the source of truth for diffing schema changes.

When bumping `version`:
1. Make the entity change, increment `version`.
2. `./gradlew :app:compileDebugKotlinAndroid` — emits new schema JSON.
3. Diff old vs new JSON to derive SQL.
4. Add a `Migration(oldVer, newVer) { db -> db.execSQL("...") }` to `ALL_MIGRATIONS` in `data/local/Migrations.kt`.

If you bump the version without adding a migration, the app **will crash** on upgrade — this is the intended safety net (we deliberately replaced `fallbackToDestructiveMigration` with `fallbackToDestructiveMigrationOnDowngrade`, which only wipes on downgrade). Don't reintroduce destructive fallback for upgrades — the user's local data is the only copy of `syncStatus = PENDING` writes.

## Web app (`webapp/`)

**Stack:** React 18, TypeScript, Tailwind CSS, Vite, Firebase SDK v10. Deploys to Firebase Hosting (`firebase deploy --only hosting`); the `dist/` build output is the hosting source.

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

**Layout:** every authenticated page renders inside `src/components/AppShell.tsx`, which puts the persistent Todoist-style `src/components/Sidebar.tsx` on the left (Boards section listing each board, Reminders section with One-off + Recurring children, Settings, sign-out) and the page content in a scrollable column on the right. There is no top nav bar (the old `AppHeader` was removed). Pages must render their own scroll region (`flex-1 overflow-y-auto`) — `AppShell` itself is a non-scrolling `h-screen` flex column.

**Key source files:**
- `src/components/AppShell.tsx` — sidebar + content layout wrapper used by all authed pages
- `src/components/Sidebar.tsx` — persistent left nav; lists boards + reminder views
- `src/pages/KanbanBoardPage.tsx` — board view; columns are `flex-1` (divide the width evenly), cards show a checklist progress badge, add-column is a header dialog
- `src/pages/TaskDetailPage.tsx` — task detail with subtask drag-and-drop + rename; title/description **autosave** (debounced, no Save button)
- `src/pages/RemindersPage.tsx` — one-off and recurring reminders; reads `?view=one-off|recurring` to filter to one section (matches the sidebar children)
- `src/pages/SettingsPage.tsx` — settings including API key management
- `src/hooks/useBoard.ts` — board/column/task Firestore hooks; also attaches one subtask listener per task to expose `subtaskCounts` for board card badges
- `src/hooks/useTask.ts` — task + subtask Firestore hooks
- `src/hooks/useReminders.ts` — reminders Firestore hooks (one-off + recurring)
- `src/hooks/useApiKeys.ts` — generate/revoke API keys (SHA-256, dual-write to `apiKeys/{hash}` and `apiKeyMeta`)
- `src/types.ts` — shared TypeScript types

## Cloud Functions (`functions/`)

**Stack:** TypeScript on Node 22, `firebase-functions` v2, `firebase-admin` v12.

**Build / deploy:**
```bash
cd functions && npm run build           # tsc → lib/
cd functions && npm run serve           # local emulator (functions only)
cd functions && npm run deploy          # firebase deploy --only functions
```

**Endpoints:**
- `createReminder` (HTTPS) — authenticated by `x-api-key` header. Hashes the key with SHA-256, looks up `apiKeys/{hash}.userId`, writes to that user's `reminders/` or `recurringReminders/` collection. Used by external integrations (e.g. Shortcuts, scripts) to create reminders without going through the Android app.

The function uses Firebase Admin SDK, which **bypasses Firestore security rules** — validation must happen in code. Don't add functions that take a `userId` from the request body without re-deriving it from the authenticated key.

## Keeping surfaces in sync

Any change to **shared data** must be reflected everywhere that touches the field. Examples:

- New Firestore field on an existing entity → update **all** of:
  - `app/src/.../FirestoreMappers.kt` (read+write)
  - `webapp/src/types.ts` + relevant hook
  - `functions/src/createReminder.ts` if the function writes that entity
  - Room entity + a Room migration (see Room migrations above)
- New display state (e.g. snooze, enable/disable) → update both `ReminderItem.kt` **and** `RemindersPage.tsx`
- New Firestore collection → update both `SyncWorker.kt` **and** the relevant webapp hook (and `firestore.rules`!)
- Field name divergence between apps → always write **both** field names and read with fallback (or align the names — recent example: webapp was changed from `nextTriggerMillis` to Android's `scheduledAt`)

**What the webapp does NOT need to match:**
- Alarm/notification mechanics (Android-only: AlarmScheduler, BroadcastReceiver, AlarmRingerService)
- Room DB schema (Android-local only)
- RecurrenceEngine computation (the cloud function has its own copy in `functions/src/utils/recurrence.ts` — keep these two in sync if recurrence rules change)
- Creating recurring reminders from the UI (webapp is read/display only for recurring; reminders are created by the Android app or the `createReminder` cloud function)
- Glance home-screen widgets (Android-only)

## Firestore data model

```
apiKeys/{sha256OfKey}                    ← top-level; { userId } — looked up by cloud functions
invitations/{boardId_inviteeEmail}       ← top-level; board-share invites
users/{userId}/
  boards/{boardId}/                      ← collaborators map for sharing; isDeleted soft-delete
    columns/{columnId}
    tasks/{taskId}/
      subtasks/{subtaskId}
  reminders/{reminderId}                 ← one-off reminders
  recurringReminders/{reminderId}        ← recurring reminders
  apiKeyMeta/{metaId}                    ← user-visible metadata (name, createdAt, keyHash) for keys
```

**Board fields:** `userId`, `title`, `columnOrder` (JSON-encoded `string[]`), `collaborators` (map keyed by collaborator uid; populated when an invitee accepts), `createdAt`, `updatedAt`, `isDeleted`.

**One-off reminder fields:** `userId`, `title`, `scheduledAt`, `reminderStyle`, `isEnabled`, `snoozedUntilMillis`, `isCompleted`, `completedAt`, `createdAt`, `updatedAt`, `isDeleted`.

**Recurring reminder fields:** `userId`, `title`, `recurrenceRuleJson`, `startDate`, `reminderTime` ("HH:mm"), `nextFireAt`, `reminderStyle`, `isEnabled`, `snoozedUntilMillis`, `createdAt`, `updatedAt`, `isDeleted`.

**Subtask fields:** `id`, `taskId`, `title`, `isCompleted`, `order` (Double), `updatedAt`, `isDeleted`.

**API key fields:** `apiKeys/{hash}` = `{ userId }` (server-only read; client may create); `apiKeyMeta/{metaId}` = `{ name, createdAt, keyHash }` (user-visible metadata).

Ordering uses midpoint arithmetic: `newOrder = (orderBefore + orderAfter) / 2`. If `orderAfter <= orderBefore`, use `orderBefore + 1`.

## Sharing / collaboration

A board is "shared" when its `collaborators` map is non-empty. Owner workflow:
1. Owner creates an `invitations/{boardId_inviteeEmail}` doc with `status = "pending"`, `ownerUid`, `inviteeEmail`.
2. Invitee (logged in with the matching email) reads the invitation, accepts → updates invitation to `status = "accepted"` AND adds themselves to the board's `collaborators` map (rules allow this specific update via the `isAcceptedInvitee` helper).
3. Now the invitee can read/write columns/tasks/subtasks under the shared board, but **cannot** rename the board (rule blocks `titleChanged()` for non-owners).

Shared-board reads on Android go through `BoardAccessDao` / `pullSharedBoard` — the local user has a `BoardAccessEntity` row pointing at the owner's UID, and the sync layer pulls the board from `users/{ownerUid}/boards/{boardId}` rather than the user's own subtree. Reminders are **not** shareable — `reminders/` and `recurringReminders/` are owner-only per the rules.

## Firebase config

- `firebase.json` — wires functions, Firestore (rules + indexes), and hosting (`webapp/dist`).
- `firestore.rules` — owner/collaborator/invitee rules; updates here must keep the helper functions (`isOwner`, `isCollaborator`, `isAcceptedInvitee`, `titleChanged`) consistent.
- `firestore.indexes.json` — composite indexes; add when introducing new compound queries.
- `google-services.json` — gitignored; required for the Android build.
