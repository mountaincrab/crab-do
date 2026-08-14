# Crab Do — Claude guidance

## Project structure

This repo contains **three deployable surfaces** sharing a Firestore backend:

| Directory | What it is |
|-----------|-----------|
| `app/` | Android app (Kotlin Multiplatform + Jetpack Compose) |
| `webapp/` | Web app (React + TypeScript + Tailwind + Vite), deployed to Firebase Hosting |
| `functions/` | Firebase Cloud Functions (TypeScript) — public HTTP endpoints (e.g. `createReminder`) authenticated via API keys |

When making changes that affect shared data (Firestore schema, subtask fields, ordering logic, etc.) always check and update **all surfaces that touch the field** — that may include cloud functions too.

## Commit messages (required: Conventional Commits)

Every commit/PR **must** use [Conventional Commits](https://www.conventionalcommits.org/). This is not just style — the `Release` workflow (`.github/workflows/release.yml`) reads the commit history to **bump the per-surface semver tag** and **generate the GitHub Release notes from the commit subjects**. A commit without a recognised prefix is **silently dropped from the changelog**, so an unprefixed commit produces a release with empty notes.

**Three independent, continuous releases (path-detected).** The repo has three separately-versioned surfaces. Every push to `main` auto-releases and deploys **only the surfaces whose files changed** in that push — there is no release-PR gate. `release.yml` uses `dorny/paths-filter` to detect the changed surfaces, then `mathieudutour/github-tag-action` (once per surface, with its own `tag_prefix`) to bump that surface's tag from its Conventional Commits, then deploys: `app/**` → `android-vX.Y.Z` (build APK → GitHub Release, [`release-android.yml`](.github/workflows/release-android.yml)), `webapp/**` → `webapp-vX.Y.Z` (Firebase Hosting, [`deploy-web.yml`](.github/workflows/deploy-web.yml)), `functions/**` → `functions-vX.Y.Z` (functions + Firestore rules/indexes, [`deploy-functions.yml`](.github/workflows/deploy-functions.yml)). Tagging and deploying run in the **same workflow run**, so only `GITHUB_TOKEN` is needed (no PAT). The **path** of changed files decides which surface bumps; the commit **type** decides the bump size. Android's `versionName` is derived by Gradle from the `android-v*` tag (see `app/build.gradle.kts`) — if you change the tag prefix, update that `--match` pattern too. Root/shared files: `firestore.rules` + `firestore.indexes.json` map to the **functions** surface and `firebase.json` maps to **both** web and functions (see the `filters:` in `release.yml`).

Prefix → bump:

| Prefix | Bump | Example |
|--------|------|---------|
| `feat:` | minor | `feat: add dark mode toggle to settings` |
| `fix:` | patch | `fix: stop reminders firing twice after snooze` |
| `feat!:` or a `BREAKING CHANGE:` footer | major | `feat!: drop the legacy sync format` |
| `ci:` `chore:` `docs:` `refactor:` `perf:` `test:` `style:` | patch | `ci: cache Gradle packages between runs` |

Example:

```
feat: autosave task edits on close

Removes the explicit Save button; title/description persist on a
debounced timer and when the editor closes.
```

**Squash-merge caveat:** when a PR is squash-merged the **PR title** becomes the `main` commit subject, so the PR title is what needs the Conventional Commits prefix — a well-formed prefix only on the branch commits won't help if the PR title is plain prose.

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

### Task editor dialog (non-obvious)

`EditCardDialog` in `ui/boards/components/AddCardDialog.kt` is rendered as a **full-screen overlay in the activity window** (`BackHandler` + `Surface(Modifier.fillMaxSize())`), **not** a Compose `Dialog`. Do not turn it back into a `Dialog`.

A Compose `Dialog(usePlatformDefaultWidth = false)` gets its own sub-window which, on Android 15/16, is positioned below the status bar yet sized to the full display height — so its bottom lands ~one status-bar-height below the screen and the bottom-pinned "Add a subtask" composer falls off the bottom edge. No window hacking fixes it (`setLayout(MATCH_PARENT)`, `setDecorFitsSystemWindows(false)`, explicit display-height bound, `setGravity(TOP)` all fail; the WindowManager keeps the sub-window inset and Compose double-counts the status-bar inset). In the activity window the height is bounded and system-bar/IME insets dispatch correctly, so `weight(1f)` + the pinned composer (`imePadding()` + `navigationBarsPadding()`) work. (This is how the original `TaskDetailScreen` route behaved before it was moved into a Dialog.)

`AddCardDialog` (New Task) is still a `Dialog` — only safe because it has no pinned bottom element. If you add a pinned composer/bar there, convert it to an overlay too.

**Autosave must stay field-scoped.** `EditCardDialog` has no Save button — it persists on close (cross icon or back). It diffs the current field values against `opened` (a `remember`ed snapshot of the task as the editor opened, deliberately *not* the live `task` parameter, which recomposes when a remote change lands) and emits a `TaskEdits` carrying only changed fields; `KanbanBoardViewModel.saveTaskChanges` applies those onto the task's current row re-read from the DB. Do not go back to saving all fields unconditionally: since `SyncWorker` pushes the whole document, opening and closing the editor on a task whose description was edited elsewhere would otherwise push the editor's stale copy over it and destroy the remote edit.

### Sync architecture (non-obvious)

Three mechanisms run side-by-side:

1. **Real-time Firestore listeners** — `addSnapshotListener` reflects remote changes into Room immediately. `ReminderRepository.startFirestoreListener` covers `reminders/` + `recurringReminders/` (started by `BoardListViewModel`); `BoardRepository.startBoardListener` covers the board doc, its `columns/`, `tasks/` and each task's `subtasks/` (started by `KanbanBoardViewModel`, stopped in `onCleared`). Subtasks are a per-task subcollection, so the tasks listener attaches/detaches one subtask listener per task as tasks appear and disappear.
2. **`SyncWorker`** (WorkManager, unique work name `"sync"`) — pushes locally-pending writes (`syncStatus = PENDING`) up to Firestore, then pulls deltas via `updatedAt > lastSyncTimestamp`.
3. **`ForegroundSyncObserver`** (`data/remote/`) — a `ProcessLifecycleOwner` observer registered in `KanbanApplication`; enqueues a sync on every foreground entry, throttled to once per 30s. This is the backstop for what listeners can't cover (the window before a listener attaches, and changes that landed while the process was dead). It enqueues with `ExistingWorkPolicy.KEEP`, **not** `REPLACE`, so returning to the app can't cancel an in-flight push of pending local writes.

**Critical invariant:** the Firestore listener checks `existing.syncStatus == SyncStatus.PENDING` before applying a remote document and **skips** if so. This prevents the listener from overwriting a local change before `SyncWorker` has had a chance to push it. When adding new entities/listeners, preserve this guard.

**Critical invariant:** `SyncWorker` pushes a whole entity document (`set(map, merge)`), so any Room row it pushes overwrites the remote copy of *every* field in that map. A UI write must therefore never persist a field the user did not change — see the editor autosave below.

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
5. **Always add a migration test** (see below). A new migration without a test is not done.

If you bump the version without adding a migration, the app **will crash** on upgrade — this is the intended safety net (we deliberately replaced `fallbackToDestructiveMigration` with `fallbackToDestructiveMigrationOnDowngrade`, which only wipes on downgrade). Don't reintroduce destructive fallback for upgrades — the user's local data is the only copy of `syncStatus = PENDING` writes.

#### Migration tests (required for every migration)

Migration tests live in `app/src/androidInstrumentedTest/kotlin/com/mountaincrab/crabdo/data/local/MigrationTest.kt`. Each `@Database` version bump **must** add a `migrate{old}To{new}_…` method that:
1. `helper.createDatabase(name, oldVersion)` — re-creates the DB from the exported `{old}.json` schema; seed any rows whose preservation you want to assert.
2. `helper.runMigrationsAndValidate(name, newVersion, true, *ALL_MIGRATIONS)` — runs the migration and **validates the resulting schema against `{new}.json`** (this is the real safety net: it catches a missing/wrong migration before it ships).
3. Assert the seeded data survived and any new column behaves as expected.

These are **instrumented** tests (Room's `MigrationTestHelper` requires `Instrumentation`, and `BundledSQLiteDriver` ships only Android native libs), so they need a connected device/emulator. The exported schema JSONs are shipped into the test APK assets (`android.sourceSets["androidTest"].assets` → `$projectDir/schemas`).

Run:
```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mountaincrab.crabdo.data.local.MigrationTest
```

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

**Version display:** `vite.config.ts` derives the version from the latest `webapp-v*` tag
(the same series `release.yml` bumps) and bakes it in as the `__APP_VERSION__` global
(declared in `src/vite-env.d.ts`); Settings → About shows it, mirroring the Android
`BuildConfig.VERSION_NAME` row. Clean `X.Y.Z` on `main`/tagged builds,
`X.Y.Z-<branch>.<sha>` elsewhere, `0.0.0` when no tag is reachable — which is why
`deploy-web.yml` checks out with `fetch-depth: 0`. `VITE_APP_VERSION` overrides it.

**Build:**
```bash
cd webapp && npm run build
```

**Layout:** every authenticated page renders inside `src/components/AppShell.tsx`, which puts the persistent Todoist-style `src/components/Sidebar.tsx` on the left (Boards section listing each board, Reminders section with One-off + Recurring children, Settings, sign-out) and the page content in a scrollable column on the right. There is no top nav bar (the old `AppHeader` was removed). Pages must render their own scroll region (`flex-1 overflow-y-auto`) — `AppShell` itself is a non-scrolling `h-screen` flex column.

**Key source files:**
- `src/components/AppShell.tsx` — sidebar + content layout wrapper used by all authed pages
- `src/components/Sidebar.tsx` — persistent left nav; lists boards + reminder views
- `src/pages/KanbanBoardPage.tsx` — board view; columns are `flex-1` (divide the width evenly), cards show a checklist progress badge, add-column is a header dialog
- `src/components/TaskEditor.tsx` — presentational task editor dialog (title/description, reminder, checklist with drag-and-drop). Owns no persistence; the two modals below supply the handlers
- `src/components/TaskModal.tsx` — editing an **existing** task: title/description **autosave** (debounced, no Save button), everything else writes immediately
- `src/components/NewTaskModal.tsx` — creating a task: the whole draft (title, description, reminder, checklist items) is held in local state and written in **one batch** when "Add task" is pressed, via `useBoard.createTask`. Opening "+ Add task" must never write a placeholder task to Firestore
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
