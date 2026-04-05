# Crabban — Development Progress

## Project
Android Kanban + Reminders app. Offline-first, Room as local source of truth, Firestore sync in background.

- **Package**: `com.mountaincrab.crabdo`
- **Branch**: `feat/initial-implementation`
- **Min SDK**: API 26 (Android 8.0)
- **Target device**: Pixel 6

---

## What's been implemented

### Build & project setup
- Gradle project created from scratch (no Android Studio wizard): `settings.gradle.kts`, root and app `build.gradle.kts`, `gradle/libs.versions.toml`
- All dependencies pinned: Compose BOM 2025.04.00, Room 2.7, Hilt 2.52, Firebase BOM 33.7, WorkManager 2.10, Reorderable 2.4
- `gradle.properties` with 4g JVM heap (needed to avoid OOM during build)
- Placeholder `app/google-services.json` — **must be replaced** with real one from Firebase console before Firebase features work (see `~/claude/FIREBASE_SETUP_CRABBAN.md`)
- `.gitignore` updated to exclude `**/build/` (was previously only excluding root `/build`)

### Data layer (Room)
- 5 entities: `BoardEntity`, `ColumnEntity`, `TaskEntity`, `SubtaskEntity`, `ReminderEntity`
- All entities use UUID string primary keys and soft-delete (`isDeleted` flag) for sync compatibility
- Fractional indexing (`order: Double`) on columns, tasks, and subtasks for drag-and-drop reordering without renumbering all rows
- `SyncStatus` enum (`PENDING` / `SYNCED` / `SYNCING` / `FAILED`) on every entity
- 5 DAOs with upsert, soft-delete, and `getUnsynced*()` queries for the sync worker
- `AppDatabase` with type converters for enums

### Domain
- `RecurrenceRule` — data class serialised to/from JSON (via Gson) for storage in Room. Supports: Daily, Every-N-days, Weekly (multi-day), Monthly.
- `RecurrenceEngine` — pure Kotlin object that computes the next trigger time after a given instant for any `RecurrenceRule`. Also produces human-readable descriptions ("Every Monday at 09:00").

### Repositories
- `BoardRepository` — CRUD for boards and columns, fractional reorder
- `TaskRepository` — CRUD for tasks, fractional move between columns, alarm scheduling on save
- `SubtaskRepository` — CRUD for checklist items
- `ReminderRepository` — CRUD for standalone reminders, alarm scheduling, `onReminderFired()` to advance recurring reminders, `rescheduleAllReminders()` for boot

All repositories write to Room first, then enqueue a `SyncWorker` via WorkManager (requires network).

### Firebase / sync
- `AuthRepository` — anonymous sign-in on first launch; upgrade path to Google account via `linkWithGoogle()`
- `SyncWorker` (Hilt-injected `CoroutineWorker`) — pushes all `PENDING` entities to Firestore, pulls changes since last sync timestamp
- `FirestoreMappers.kt` — `toFirestoreMap()` and `DocumentSnapshot.toXxxEntity()` for all 5 entity types

### Alarms & notifications
- `AlarmScheduler` — wraps `AlarmManager`; uses `setExactAndAllowWhileIdle` on API 31+ when permission granted, falls back to `setWindow` otherwise
- `ReminderReceiver` — `BroadcastReceiver` handling fire / dismiss / snooze actions; advances recurrence on fire via Hilt `EntryPoint`
- `BootReceiver` — reschedules all alarms on boot, timezone change, app update
- `AlarmAlertActivity` — full-screen Compose UI shown over lock screen on Android < 14
- `NotificationHelper` — two channels: `ALARM` (ongoing, max importance, alarm sound) and `NOTIFICATION` (standard high-importance)

### Dependency injection (Hilt)
- `DatabaseModule` — provides `AppDatabase` and all DAOs
- `FirebaseModule` — provides `FirebaseAuth` and `FirebaseFirestore` singletons
- `WorkerModule` — provides `WorkManager`
- `RepositoryModule` — provides all repositories
- `KanbanApplication` implements `Configuration.Provider` to supply Hilt's `HiltWorkerFactory` to WorkManager (required for Hilt-injected workers)

### Navigation & UI
3-tab bottom nav: **Board** (pinned) · **Boards** (list) · **Reminders**

| Screen | Notes |
|--------|-------|
| `BoardListScreen` | List of boards; pin/unpin ⭐ (amber colour when pinned); rename/delete via dropdown; FAB to create |
| `KanbanBoardScreen` | Horizontal `LazyRow` of columns; drag-and-drop between columns (`dragAndDropSource`/`dragAndDropTarget`); within-column reorder (Reorderable library) |
| `ColumnConfigSheet` | Bottom sheet: rename, delete, drag-to-reorder columns |
| `TaskDetailScreen` | Edit title/description; checklist subtasks; set/clear reminder with separate date + time pickers |
| `PinnedBoardScreen` | Renders pinned board inline; shows prompt if none pinned |
| `RemindersScreen` | Sorted list; swipe-to-delete; enable/disable toggle |
| `AddEditReminderScreen` | Title, separate date and time fields (each opens its own picker), style toggle, recurrence toggle + `RecurrencePicker` |
| `RecurrencePicker` | Interval + period dropdown; day-of-week chips (weeks); day-of-month input (months); time input |
| `SettingsScreen` | Account section (anonymous / Google link); pinned board picker; exact-alarm permission banner |

### User preferences (DataStore)
- `UserPreferencesRepository` — persists `pinnedBoardId` and `lastSyncTimestamp`

---

## What still needs doing

### Before Firebase works
- [ ] Complete Firebase project setup (see `~/claude/FIREBASE_SETUP_CRABBAN.md`)
- [ ] Replace `app/google-services.json` placeholder with real file
- [ ] Add SHA-1 fingerprint to Firebase console to enable Google Sign-In
- [ ] Deploy Firestore security rules
- [ ] Revert the offline auth fallback commit (`git revert 2ba33c7`) once Firebase is configured

### Features not yet implemented
- [ ] Google Sign-In flow in `SettingsScreen` (stub button present, `CredentialManager` call not wired up)
- [ ] `POST_NOTIFICATIONS` runtime permission request (should be triggered when user first creates a reminder on API 33+)
- [ ] Column pull-sync in `SyncWorker` (boards and reminders pull; columns/tasks/subtasks push only for now)
- [ ] Firestore composite indexes (will be prompted in logcat on first run with real Firebase)

### Polish / known rough edges
- Launcher icon is a placeholder "C" on orange — replace with real artwork
- `AlarmAlertActivity` uses `MaterialTheme` directly without the app theme wrapper
- `TaskCard` subtask counts are hardcoded to 0 (the `KanbanBoardViewModel` doesn't yet join subtask counts into the task list — needs a separate query or a combined DAO result)
- No empty-state handling on the Kanban board itself if a board has zero columns
- `fallbackToDestructiveMigration()` is set in `DatabaseModule` — fine for development, must be replaced with proper migrations before any public release

---

## Build issues resolved

| Error | Fix |
|-------|-----|
| Gradle daemon OOM (512 MiB heap) | Added `gradle.properties` with `-Xmx4g` |
| `windowShowWhenLocked` / `windowTurnScreenOn` not found in XML | Removed from `themes.xml` — `AlarmAlertActivity` sets these in code |
| `mipmap/ic_launcher` not found | Added adaptive icon XML in `mipmap-anydpi-v26/` |
| App hangs on spinner without Firebase | Added offline fallback in `AuthRepository` (separate revertable commit `2ba33c7`) |
| `attr/colorControlNormal` not found in notification drawables | Removed `android:tint` attribute; `fillColor` is already white |
| Reorderable 2.x API changes | `rememberReorderableLazyListState` requires explicit `LazyListState`; `longPressDraggable` → `longPressDraggableHandle()` (scope-level) |
| `dragAndDropSource`/`dragAndDropTarget` unresolved | Moved to `androidx.compose.foundation.draganddrop` in newer Compose; added `@OptIn(ExperimentalFoundationApi::class)` |
| `ClipEntry` not found | `DragAndDropTransferData` takes `clipData: ClipData` directly, not `clipEntry` |
| `ExposedDropdownMenuBox` experimental error | Added `@OptIn(ExperimentalMaterial3Api::class)` to `RecurrencePicker` |
| Missing `import android.content.Intent` in `AlarmAlertActivity` | Added import |
| `WorkManagerInitializer` ClassNotFoundException on startup | Manifest entry was enabling auto-init instead of disabling it; fixed with `tools:node="remove"` on the `<meta-data>`; `KanbanApplication` now implements `Configuration.Provider` with injected `HiltWorkerFactory` |
| FAB hidden behind bottom nav bar | `NavHost` modifier had no padding; added `Modifier.padding(bottom = padding.calculateBottomPadding())` |
| Pinned board tab showed "No board pinned" even when pinned | `BoardListViewModel` used `SharingStarted.WhileSubscribed(5000)` — flows paused when off-screen and emitted `null` on resume; changed to `SharingStarted.Eagerly` |
| Crash when navigating to pinned board | `KanbanBoardViewModel` used `checkNotNull(savedStateHandle["boardId"])` but `PinnedBoardScreen` called `KanbanBoardScreen` directly (not via nav), so `SavedStateHandle` was empty; fixed with `@AssistedInject` — `boardId` now passed via factory callback |

---

## Runtime behaviour confirmed working
- App launches and shows the 3-tab navigation
- Boards list: create, rename, delete, pin/unpin (pinned boards show amber star ⭐)
- Pinned board tab navigates to the pinned board's Kanban view
- Kanban board: columns visible, tasks visible
- Reminders list: FAB visible, navigates to Add Reminder screen
- Add Reminder: separate Date and Time fields, style toggle, recurrence toggle
- Date/time pickers default to today + 1 hour
