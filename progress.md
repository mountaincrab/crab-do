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
- `AlarmScheduler` — wraps `AlarmManager`; uses `setExactAndAllowWhileIdle` on API 31+ when permission granted, falls back to `setWindow` otherwise; logs scheduling to logcat (`AlarmScheduler` tag) for diagnosis
- `ReminderReceiver` — `BroadcastReceiver` handling fire / dismiss / snooze actions; advances recurrence on fire via Hilt `EntryPoint`; logs to `ReminderReceiver` tag on fire
- `BootReceiver` — reschedules all alarms on boot, timezone change, app update
- `AlarmRingerService` — foreground service started by `ReminderReceiver` for ALARM-style reminders; loops alarm sound via `MediaPlayer` until dismissed or snoozed; holds a `PARTIAL_WAKE_LOCK` (10-min cap); posts the foreground notification with Dismiss / Snooze actions and full-screen intent
- `AlarmAlertActivity` — full-screen Compose UI shown over lock screen (all API levels); uses `CrabbanTheme`
- `NotificationHelper` — two channels: `ALARM` (ongoing, max importance, alarm sound) and `NOTIFICATION` (standard high-importance); NOTIFICATION-style reminders still use one-shot notification; ALARM-style reminders use `AlarmRingerService`

### Dependency injection (Hilt)
- `DatabaseModule` — provides `AppDatabase` and all DAOs
- `FirebaseModule` — provides `FirebaseAuth` and `FirebaseFirestore` singletons
- `WorkerModule` — provides `WorkManager`
- `RepositoryModule` — provides all repositories
- `KanbanApplication` implements `Configuration.Provider` to supply Hilt's `HiltWorkerFactory` to WorkManager (required for Hilt-injected workers)

### Navigation & UI
4-tab standard bottom nav: **Board** (pinned) · **Boards** (list) · **Reminders** · **Settings**
- Standard Material3 `NavigationBar` with labels always visible, surface background
- Nav bar visible on all 4 tabs **plus** the Kanban board detail screen (so the tab context is preserved while viewing a board); hidden only on AddEditReminder / TaskDetail

| Screen | Notes |
|--------|-------|
| `BoardListScreen` | List of boards with neubrutalist 1.5dp border + 4dp radius cards; pin/unpin ⭐ (amber); rename/delete via dropdown; compact 36dp icon buttons; FAB to create |
| `KanbanBoardScreen` | Horizontal `LazyRow` of columns; drag-and-drop between columns (`dragAndDropSource`/`dragAndDropTarget`); within-column reorder (Reorderable library) |
| `ColumnConfigSheet` | Bottom sheet: rename, delete, drag-to-reorder columns |
| `TaskDetailScreen` | Edit title/description; checklist subtasks; set/clear reminder with separate date + time pickers; time picker has clock/keyboard toggle |
| `PinnedBoardScreen` | Renders pinned board inline; shows prompt if none pinned |
| `RemindersScreen` | Section-grouped list (TODAY / UPCOMING / PAST with amber headers); swipe-to-delete; compact rows (8dp vertical padding, 20dp icon, 85% scaled Switch); snoozed items show "Snoozing until HH:mm" |
| `AddEditReminderScreen` | Title (auto-focused with keyboard on new), date/time fields, style toggle, recurrence; time picker has clock/keyboard toggle; toast on save showing time-until |
| `RecurrencePicker` | Interval + period dropdown; day-of-week chips (weeks); day-of-month input (months); time input |
| `SettingsScreen` | Account section (anonymous / Google link); pinned board picker; exact-alarm permission banner |
| `KanbanColumn` | 260dp wide; column header uppercase with letter-spacing + amber task count badge; 5dp card gap; Add card opens `AlertDialog` with title + description fields, auto-focuses title |

### User preferences (DataStore)
- `UserPreferencesRepository` — persists `pinnedBoardId`, `lastSyncTimestamp`, and `timeInputKeyboard` (remembered across sessions)

### Theme system (dark navy, user-selectable)
- `CrabbanTheme` accepts an `AppTheme` enum with 3 user-selectable dark palettes: **Deep Navy**, **Charcoal**, **Slate**
- All schemes share the same accents: primary `#4F7CFF` (blue), secondary `#8B5CF6` (purple), error `#EF4444`, tertiary `#10B981`
- Background/surface tokens differ per scheme (Deep Navy: `#0A1020` / `#131A2E`; Charcoal: `#0A0A0A` / `#141414`; Slate: `#161820` / `#20232E`)
- `AppPalette` data class exposed via `LocalAppPalette` CompositionLocal for tokens that don't fit in `ColorScheme` (gradient start/end, card border, alarm tint)
- `accentGradient()` helper returns a `Brush.linearGradient(purple → blue)` used by `GradientIconBlock` — a reusable rounded-square icon badge used in top-bar titles
- Theme selection persisted in DataStore (`UserPreferencesRepository.appTheme`); exposed via `ThemeViewModel` (Hilt) and observed in `MainActivity`
- All hardcoded amber/neubrutalist colours (`#F5C518`, `#1A1A1A`, `#0D0D0D`) replaced with `MaterialTheme.colorScheme` tokens across boards, reminders, add/edit, kanban column
- `SettingsScreen` redesigned around this palette: gradient icon header, card-grouped sections (Account, Appearance, Pinned Board, About), and an **Appearance** swatch picker (3-up) with a check badge on the selected theme — tap to switch live
- Status bar + navigation bar both match `colorScheme.background`

### Home screen widget (`RemindersWidget`)
- Jetpack Glance `GlanceAppWidget` — shows up to 6 upcoming reminders, sorted by trigger time
- Snoozed reminders shown with 💤 prefix and snooze-until time
- "+" button in widget header opens `AddEditReminderScreen` via `MainActivity` intent
- Adjustable size (horizontal + vertical resize); minimum 160×110dp
- Widget refreshes on `APPWIDGET_UPDATE` broadcast; `ReminderRepository.createReminder/updateReminder` sends broadcast to trigger immediate refresh
- `RemindersWidgetReceiver` registered in manifest with `reminders_widget_info.xml`
- `MainActivity` handles `open_add_reminder` intent extra (from widget or `onNewIntent`)

### Permissions
- `POST_NOTIFICATIONS` runtime permission requested in `MainActivity` on first launch (API 33+)
- `SCHEDULE_EXACT_ALARM` — banner in `SettingsScreen` links to system settings if not granted

### Delete reminders
- Swipe-to-delete on each row in `RemindersScreen` (inline delete icon removed for compactness)
- Delete icon in `AddEditReminderScreen` top bar when editing — shows confirmation dialog before deleting

### Snooze duration picker (`SnoozePickerActivity`)
- When an alarm fires, pressing **Snooze** (notification action or full-screen `AlarmAlertActivity`) opens `SnoozePickerActivity`
- Shows preset buttons: 5 / 10 / 15 / 20 / 30 minutes
- "Custom…" option reveals a number input for any duration
- "Dismiss alarm" button dismisses without rescheduling
- Snooze time is **saved to Room** (`ReminderEntity.snoozedUntilMillis`) so the reminders list shows "Snoozing until HH:mm"
- `snoozedUntilMillis` is cleared when the alarm fires next (in `ReminderReceiver.handleFire`)

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
- [ ] Column pull-sync in `SyncWorker` (boards and reminders pull; columns/tasks/subtasks push only for now)
- [ ] Firestore composite indexes (will be prompted in logcat on first run with real Firebase)

### Polish / known rough edges
- Launcher icon is a placeholder "C" on orange — replace with real artwork
- `TaskCard` subtask counts are hardcoded to 0 (the `KanbanBoardViewModel` doesn't yet join subtask counts into the task list — needs a separate query or a combined DAO result)
- No empty-state handling on the Kanban board itself if a board has zero columns
- `fallbackToDestructiveMigration()` is set in `DatabaseModule` — fine for development, must be replaced with proper migrations before any public release
- `ReminderEntity` is now version 2 (added `snoozedUntilMillis` column) — destructive migration will wipe existing data on upgrade
- Standalone reminders not confirmed firing on real device — logging added to `AlarmScheduler` and `ReminderReceiver` to diagnose; run `adb logcat AlarmScheduler:D ReminderReceiver:D '*:S'` while testing

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
| Notifications not firing on API 33+ | `POST_NOTIFICATIONS` is a runtime permission — manifest declaration alone is not enough; added `registerForActivityResult(RequestPermission)` call in `MainActivity` |
| App crash on launch after adding `snoozeDurationMinutes` field | Room schema hash mismatch (new column added without bumping DB version); resolved by moving snooze config to fire-time picker (`SnoozePickerActivity`) and reverting the entity change |
| App hangs on spinner on real device | `signInAnonymously()` reaches real Firebase servers with placeholder credentials and hangs indefinitely; added `withTimeout(5_000)` around the call so it falls back to offline mode within 5 s |
| Alarm sound stops when phone touched/unlocked | Notification sound plays once and stops; replaced with `AlarmRingerService` foreground service that loops `MediaPlayer` until dismissed or snoozed |
| Full-screen alarm not showing on lock screen | `setFullScreenIntent` was guarded to pre-Android-14 only; removed the API version check — `USE_FULL_SCREEN_INTENT` is auto-granted on Android 14+ for `CATEGORY_ALARM` apps |
| Board tab sometimes shows board list instead of pinned board | `launchSingleTop` reused the existing back stack entry without recomposing when PinnedBoard was already root; fixed by using `popUpTo(startDestinationId, inclusive = true)` so the tab always creates a fresh entry |
| DatePicker shows wrong date (tomorrow instead of today) | Material3 `DatePicker` requires UTC midnight of the local date for `initialSelectedDateMillis`; was receiving a full local timestamp causing day-off-by-one; fixed with `localDateToUtcMidnight()` helper in both `AddEditReminderScreen` and `TaskDetailScreen` |
| `ReminderRepository` context param missing in DI | Added `@ApplicationContext Context` to `provideReminderRepository()` in `RepositoryModule` after adding context to the constructor for widget refresh broadcasts |
| `isTimeInputKeyboard` property setter clashes with `setTimeInputKeyboard()` method | `var isXxx` generates a JVM setter `setXxx` that conflicts with an explicit method of the same name; renamed the method to `updateTimeInputKeyboard()` |
| Glance `GlanceId` unresolved | Lives in `androidx.glance` not `androidx.glance.appwidget`; fixed import |
| Bottom nav bar visible on detail screens | Only show nav on the 3 tab routes; checked `currentRoute in tabRoutes` and conditionally render `bottomBar` |
| Column card had lavender surfaceVariant background | Added `CardDefaults.cardColors(containerColor = surface)` + 2dp border to `KanbanColumn` Card |
| FilterChip selected/unselected visually identical | Added `FilterChipDefaults.filterChipColors(selectedContainerColor = amber)` so selected chip fills solid amber |
| TaskCard hard shadow broken (black rect offset behind card) | Removed `hardShadow` / `drawBehind` entirely; clean 1.5dp border only; reminder dates as amber pill badges |
| Deleted alarms still firing | `AlarmScheduler.cancel()` built the lookup `Intent` with no action, so `PendingIntent.filterEquals` never matched and `FLAG_NO_CREATE` always returned null — the alarm was never cancelled. Fixed by setting `action = ACTION_FIRE_REMINDER` on the cancel intent, plus a fallback `getBroadcast(FLAG_UPDATE_CURRENT) + cancel()` as belt-and-braces. Also added an `isDeleted` guard in `ReminderRepository.onReminderFired` for race protection |
| Widget not refreshing after delete/edit | `ReminderRepository.notifyWidgets()` was only called from `createReminder`; added to `updateReminder`, `deleteReminder`, and `onReminderFired` |
| Bottom nav disappeared when drilling into a board | `KanbanBoard.route` wasn't in the `showBottomBar` allow-list; added it so the tab context is preserved while viewing a board |
| "Add column" button styled inconsistently with "Add card" | Replaced the full-height `OutlinedCard` with a `TextButton` + primary-tinted `+` icon matching the Add card button |

---

## Runtime behaviour confirmed working
- App launches and shows the 3-tab navigation
- Boards list: create, rename, delete, pin/unpin (pinned boards show amber star ⭐)
- Pinned board tab navigates to the pinned board's Kanban view
- Kanban board: columns visible, tasks visible
- Reminders list: FAB visible, navigates to Add Reminder screen; bin icon on each row deletes; swipe-to-delete also works
- Add Reminder: separate Date and Time fields, style toggle, recurrence toggle; delete icon in top bar when editing
- Date/time pickers default to today + 1 hour
- `POST_NOTIFICATIONS` permission dialog shown on first launch (API 33+)
- Snooze picker: tapping Snooze on a notification or alarm screen opens duration selector (5/10/15/20/30 min + custom)
- ALARM-style reminders: alarm keeps ringing (looping) until dismissed or snoozed
- Board tab: always navigates to the pinned board correctly regardless of back stack state
- Date picker: defaults to today (not tomorrow) in both standalone reminders and task reminders
