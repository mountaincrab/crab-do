# Android Kanban + Reminders App — Implementation Plan

> **Target device**: Google Pixel 6 (Android 12–15, API 31–35)  
> **Solo developer, single user initially** — Firebase Auth with anonymous → Google upgrade path  
> **Primary reference**: See attached PDF guide (`Building_an_Offline-First_Android_Kanban_App...`) for architecture rationale  
> **This document**: Complete step-by-step plan for a Claude Code session to implement the app from scratch

---

## Table of Contents

1. [App Overview & Feature List](#1-app-overview--feature-list)
2. [Architecture Summary](#2-architecture-summary)
3. [Human Setup Steps (Firebase & Android Studio)](#3-human-setup-steps-firebase--android-studio)
4. [Project & Gradle Setup](#4-project--gradle-setup)
5. [Package Structure](#5-package-structure)
6. [Data Models — Room Entities](#6-data-models--room-entities)
7. [DAOs](#7-daos)
8. [AppDatabase](#8-appdatabase)
9. [Recurrence Engine](#9-recurrence-engine)
10. [Repository Layer](#10-repository-layer)
11. [Firebase Auth](#11-firebase-auth)
12. [Firestore Sync Layer](#12-firestore-sync-layer)
13. [AlarmManager & Notifications](#13-alarmmanager--notifications)
14. [Hilt Dependency Injection](#14-hilt-dependency-injection)
15. [Navigation Structure](#15-navigation-structure)
16. [ViewModels](#16-viewmodels)
17. [Screens — Boards Feature](#17-screens--boards-feature)
18. [Screens — Reminders Feature](#18-screens--reminders-feature)
19. [Screens — Settings](#19-screens--settings)
20. [User Preferences (DataStore)](#20-user-preferences-datastore)
21. [AndroidManifest](#21-androidmanifest)
22. [Implementation Order (Phases)](#22-implementation-order-phases)
23. [Key Gotchas & Notes](#23-key-gotchas--notes)

---

## 1. App Overview & Feature List

### Boards Section
- Multiple Kanban boards; each board has configurable columns (name + order)
- Tasks belong to exactly one board
- Each task: **title**, **description** (subtitle on card), **checklist/subtasks**, optional **reminder**
- Drag-and-drop cards within a column (reorder) and between columns (move)
- Column configuration: add, rename, delete, reorder columns per board
- Board management: create, rename, delete boards; pin one board as "favourite"

### Reminders Section
- Lightweight standalone reminders: **title only** (no description, no board)
- Each reminder: scheduled date/time, recurrence rule, **alarm style** (persistent ongoing notification with sound) or **notification style** (standard push notification)
- Recurrence options: Daily at time · Weekly on specific day(s) · Every N days · Every N weeks on day(s) · Monthly on day-of-month

### Navigation
- Bottom navigation bar with **3 tabs**:
  - ⭐ **Pinned Board** — opens user's pinned/favourite board directly (defaults to first board if none pinned)
  - ⊞ **All Boards** — board list view; tap to open a board
  - 🔔 **Reminders** — reminders list

### Task Reminders (from Kanban tasks)
- A task can have one reminder: date + time + alarm/notification style
- Non-recurring (one-shot only for task reminders)

### Offline-first
- App is fully functional offline; UI never waits on network
- Room is the single source of truth
- Firestore syncs in background via WorkManager when network is available

---

## 2. Architecture Summary

```
┌─────────────────────────────────────────────────────┐
│  Jetpack Compose UI                                 │
│  (BoardScreen, RemindersScreen, TaskDetailScreen…)  │
└──────────────────────┬──────────────────────────────┘
                       │ observes StateFlow/Flow
┌──────────────────────▼──────────────────────────────┐
│  ViewModels (BoardViewModel, RemindersViewModel…)   │
└──────────────────────┬──────────────────────────────┘
                       │ calls
┌──────────────────────▼──────────────────────────────┐
│  Repository Layer                                   │
│  (reads from Room → returns Flow for UI)            │
│  (writes to Room immediately → enqueues WorkManager)│
└────────┬─────────────────────────┬──────────────────┘
         │                         │
┌────────▼────────┐    ┌───────────▼──────────────────┐
│   Room Database │    │   WorkManager (SyncWorker)    │
│   (local SSOT)  │◄───│   pushes to Firestore,        │
└─────────────────┘    │   pulls remote changes        │
                       └───────────┬──────────────────┘
                                   │
                       ┌───────────▼──────────────────┐
                       │   Cloud Firestore             │
                       └──────────────────────────────┘

AlarmManager ──fires──► ReminderReceiver ──shows──► Notification
     ▲
     │ schedules
AlarmScheduler (called from Repository on reminder save/edit)
```

**Key rules**:
- UI only reads from Room (via Flow). It never reads from Firestore directly.
- All writes go to Room first, then WorkManager enqueues a sync.
- AlarmManager alarms are always rescheduled from Room (not Firestore) — this ensures offline alarm scheduling works.

---

## 3. Human Setup Steps (Firebase & Android Studio)

### 3.1 Android Studio
1. Install **Android Studio Meerkat** (2024.3+) or later.
2. Ensure you have **JDK 17** configured (File → Project Structure → SDK Location → Gradle JDK).

### 3.2 Firebase Project
1. Go to [https://console.firebase.google.com](https://console.firebase.google.com) and click **Add project**.
2. Name it (e.g. `KanbanApp`). Disable Google Analytics if desired (not needed). Click **Create project**.
3. In the project console, click the **Android icon** to add an Android app.
4. Enter package name: `com.yourname.kanbanapp` (use a real reverse-domain you own).
5. Enter a nickname (e.g. `Kanban App`).
6. Leave the SHA-1 field blank for now (add it before enabling Google Sign-In later).
7. Download `google-services.json` and save it — you will place this in `app/` directory of the Android project.
8. Skip the "Add Firebase SDK" step (the plan covers this in Gradle).

### 3.3 Enable Firestore
1. In Firebase console → **Firestore Database** → **Create database**.
2. Choose **Start in test mode** initially (you will add proper rules later in this plan).
3. Pick a region close to you (e.g. `europe-west2` for UK).

### 3.4 Enable Firebase Auth
1. Firebase console → **Authentication** → **Get started**.
2. Enable **Anonymous** provider (Sign-in method tab → Anonymous → Enable → Save).
3. Enable **Google** provider (Google → Enable → add your support email → Save).
4. To get the SHA-1 for Google Sign-In:
   - In Android Studio terminal: `./gradlew signingReport`
   - Copy the **SHA-1** from the `debug` variant.
   - Firebase console → Project Settings → Your Android app → Add fingerprint → paste SHA-1.
   - Re-download `google-services.json` and replace the one in `app/`.

### 3.5 Firebase Emulator Suite (Recommended for Development)
1. Install Node.js (v18+) and Firebase CLI: `npm install -g firebase-tools`
2. In your project root: `firebase login` then `firebase init`
3. Select **Firestore** and **Authentication** emulators.
4. Accept default ports (Auth: 9099, Firestore: 8080, UI: 4000).
5. Start emulators: `firebase emulators:start`
6. The app's debug build will automatically connect to the emulator (see Section 11).

---

## 4. Project & Gradle Setup

### 4.1 Create Android Project
- In Android Studio: **New Project → Empty Activity** (Compose template)
- Package name: `com.yourname.kanbanapp`
- Min SDK: **API 26** (Android 8.0) — covers 98%+ of active devices and simplifies API handling
- Language: **Kotlin**
- Build configuration: **Kotlin DSL** (build.gradle.kts)

### 4.2 Place google-services.json
Copy `google-services.json` into the `app/` directory.

### 4.3 Root `build.gradle.kts`
```kotlin
// Top-level build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}
```

### 4.4 `gradle/libs.versions.toml` (Version Catalog)
```toml
[versions]
agp = "8.7.0"
kotlin = "2.0.21"
coreKtx = "1.15.0"
lifecycleRuntime = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2025.04.00"
navigationCompose = "2.8.5"
room = "2.7.0"
hilt = "2.52"
hiltNavigationCompose = "1.2.0"
workManager = "2.10.0"
hiltWork = "1.2.0"
firebaseBom = "33.7.0"
datastorePrefs = "1.1.1"
ksp = "2.0.21-1.0.28"
gson = "2.11.0"
reorderable = "2.4.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntime" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntime" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hiltWork" }
hilt-work-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "hiltWork" }
workmanager-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workManager" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePrefs" }
gson = { group = "com.google.code.gson", name = "gson", version.ref = "gson" }
reorderable = { group = "sh.calvin.reorderable", name = "reorderable", version.ref = "reorderable" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
google-services = { id = "com.google.gms.google-services", version.ref = "agp" }
```

> **Note**: Check [https://maven.google.com](https://maven.google.com) for latest stable versions before implementing. The versions above were current as of early 2026.

### 4.5 `app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.yourname.kanbanapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yourname.kanbanapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "USE_EMULATOR", "true")
        }
        release {
            buildConfigField("Boolean", "USE_EMULATOR", "false")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.navigation.compose)
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    // WorkManager
    implementation(libs.workmanager.ktx)
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // DataStore
    implementation(libs.datastore.preferences)
    // Gson (for serializing RecurrenceRule)
    implementation(libs.gson)
    // Reorderable (drag-and-drop within columns)
    implementation(libs.reorderable)
    // Debug
    debugImplementation(libs.compose.ui.tooling)
}
```

---

## 5. Package Structure

```
com.yourname.kanbanapp/
├── KanbanApplication.kt
├── MainActivity.kt
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── BoardDao.kt
│   │   │   ├── ColumnDao.kt
│   │   │   ├── TaskDao.kt
│   │   │   ├── SubtaskDao.kt
│   │   │   └── ReminderDao.kt
│   │   └── entity/
│   │       ├── BoardEntity.kt
│   │       ├── ColumnEntity.kt
│   │       ├── TaskEntity.kt
│   │       ├── SubtaskEntity.kt
│   │       └── ReminderEntity.kt
│   ├── remote/
│   │   ├── FirestoreDataSource.kt
│   │   └── SyncWorker.kt
│   ├── model/
│   │   ├── RecurrenceRule.kt
│   │   └── SyncStatus.kt
│   └── repository/
│       ├── BoardRepository.kt
│       ├── TaskRepository.kt
│       ├── SubtaskRepository.kt
│       └── ReminderRepository.kt
│
├── domain/
│   └── RecurrenceEngine.kt
│
├── alarm/
│   ├── AlarmScheduler.kt
│   ├── ReminderReceiver.kt
│   ├── BootReceiver.kt
│   └── AlarmAlertActivity.kt   ← full-screen intent target
│
├── notification/
│   └── NotificationHelper.kt
│
├── ui/
│   ├── navigation/
│   │   ├── AppNavigation.kt
│   │   └── Screen.kt
│   ├── boards/
│   │   ├── BoardListScreen.kt
│   │   ├── BoardListViewModel.kt
│   │   ├── KanbanBoardScreen.kt
│   │   ├── KanbanBoardViewModel.kt
│   │   ├── TaskDetailScreen.kt
│   │   ├── TaskDetailViewModel.kt
│   │   ├── ColumnConfigSheet.kt
│   │   └── components/
│   │       ├── KanbanColumn.kt
│   │       ├── TaskCard.kt
│   │       └── SubtaskItem.kt
│   ├── reminders/
│   │   ├── RemindersScreen.kt
│   │   ├── RemindersViewModel.kt
│   │   ├── AddEditReminderScreen.kt
│   │   ├── AddEditReminderViewModel.kt
│   │   └── components/
│   │       ├── ReminderItem.kt
│   │       └── RecurrencePicker.kt
│   └── settings/
│       ├── SettingsScreen.kt
│       └── SettingsViewModel.kt
│
├── preferences/
│   └── UserPreferencesRepository.kt
│
└── di/
    ├── DatabaseModule.kt
    ├── FirebaseModule.kt
    ├── RepositoryModule.kt
    └── WorkerModule.kt
```

---

## 6. Data Models — Room Entities

### 6.1 `SyncStatus.kt`
```kotlin
package com.yourname.kanbanapp.data.model

enum class SyncStatus {
    SYNCED,    // In sync with Firestore
    PENDING,   // Modified locally, not yet pushed
    SYNCING,   // Currently being pushed
    FAILED     // Last sync attempt failed
}
```

### 6.2 `RecurrenceRule.kt`
```kotlin
package com.yourname.kanbanapp.data.model

import com.google.gson.Gson

/**
 * Represents a recurrence rule for a reminder.
 * Serialized to/from JSON for storage in Room.
 *
 * Examples:
 *   Daily at 09:00           → type=DAILY, interval=1, hour=9, minute=0
 *   Every 3 days at 08:00    → type=EVERY_N_DAYS, interval=3, hour=8, minute=0
 *   Every Monday at 10:00    → type=WEEKLY, interval=1, daysOfWeek=[2], hour=10, minute=0
 *   Mon+Wed+Fri at 07:00     → type=WEEKLY, interval=1, daysOfWeek=[2,4,6], hour=7, minute=0
 *   Every 3 weeks on Friday  → type=WEEKLY, interval=3, daysOfWeek=[6], hour=9, minute=0
 *   Monthly on 1st at 09:00  → type=MONTHLY, dayOfMonth=1, hour=9, minute=0
 *
 * daysOfWeek uses Calendar constants: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
 */
data class RecurrenceRule(
    val type: RecurrenceType,
    val interval: Int = 1,              // Used by EVERY_N_DAYS and WEEKLY (every N weeks)
    val daysOfWeek: List<Int> = emptyList(), // For WEEKLY type
    val dayOfMonth: Int = 1,            // For MONTHLY type (1–28; cap at 28 for simplicity)
    val hour: Int,
    val minute: Int
) {
    enum class RecurrenceType {
        DAILY,        // Every `interval` days at hour:minute
        WEEKLY,       // Every `interval` weeks on `daysOfWeek` at hour:minute
        EVERY_N_DAYS, // Same as DAILY with interval > 1; kept separate for UI clarity
        MONTHLY       // Every month on `dayOfMonth` at hour:minute
    }

    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): RecurrenceRule = Gson().fromJson(json, RecurrenceRule::class.java)

        /** Convenience factory for common cases */
        fun daily(hour: Int, minute: Int) =
            RecurrenceRule(RecurrenceType.DAILY, interval = 1, hour = hour, minute = minute)

        fun weekly(daysOfWeek: List<Int>, hour: Int, minute: Int, everyNWeeks: Int = 1) =
            RecurrenceRule(RecurrenceType.WEEKLY, interval = everyNWeeks,
                daysOfWeek = daysOfWeek, hour = hour, minute = minute)

        fun everyNDays(n: Int, hour: Int, minute: Int) =
            RecurrenceRule(RecurrenceType.EVERY_N_DAYS, interval = n, hour = hour, minute = minute)

        fun monthly(dayOfMonth: Int, hour: Int, minute: Int) =
            RecurrenceRule(RecurrenceType.MONTHLY, dayOfMonth = dayOfMonth.coerceIn(1, 28),
                hour = hour, minute = minute)
    }
}
```

### 6.3 `BoardEntity.kt`
```kotlin
package com.yourname.kanbanapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yourname.kanbanapp.data.model.SyncStatus
import java.util.UUID

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val columnOrder: String = "[]", // JSON array of column IDs, e.g. ["col1","col2"]
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
)
```

### 6.4 `ColumnEntity.kt`
```kotlin
package com.yourname.kanbanapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yourname.kanbanapp.data.model.SyncStatus
import java.util.UUID

@Entity(tableName = "columns")
data class ColumnEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val boardId: String,
    val title: String,
    val order: Double = 0.0,      // Fractional indexing for reorder without touching all rows
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
)
```

### 6.5 `TaskEntity.kt`
```kotlin
package com.yourname.kanbanapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yourname.kanbanapp.data.model.SyncStatus
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val boardId: String,
    val columnId: String,
    val title: String,
    val description: String = "",
    val order: Double = 0.0,           // Fractional indexing within column
    val reminderTimeMillis: Long? = null,  // null = no reminder
    val reminderStyle: ReminderStyle = ReminderStyle.ALARM,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
) {
    enum class ReminderStyle { ALARM, NOTIFICATION }
}
```

### 6.6 `SubtaskEntity.kt`
```kotlin
package com.yourname.kanbanapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yourname.kanbanapp.data.model.SyncStatus
import java.util.UUID

@Entity(tableName = "subtasks")
data class SubtaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val order: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
)
```

### 6.7 `ReminderEntity.kt`
```kotlin
package com.yourname.kanbanapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yourname.kanbanapp.data.model.SyncStatus
import java.util.UUID

/**
 * Standalone reminder (not linked to a task).
 * reminderStyle defaults to ALARM (persistent notification with sound).
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val nextTriggerMillis: Long,        // Next scheduled fire time (epoch ms)
    val reminderStyle: ReminderStyle = ReminderStyle.ALARM,
    val recurrenceRuleJson: String? = null, // null = one-shot; JSON of RecurrenceRule if recurring
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
) {
    enum class ReminderStyle { ALARM, NOTIFICATION }
}
```

---

## 7. DAOs

### 7.1 `BoardDao.kt`
```kotlin
@Dao
interface BoardDao {
    @Query("SELECT * FROM boards WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt")
    fun observeBoards(userId: String): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE id = :boardId AND isDeleted = 0")
    fun observeBoard(boardId: String): Flow<BoardEntity?>

    @Query("SELECT * FROM boards WHERE id = :boardId")
    suspend fun getBoardById(boardId: String): BoardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(board: BoardEntity)

    @Query("SELECT * FROM boards WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedBoards(): List<BoardEntity>

    @Query("SELECT * FROM boards WHERE isDeleted = 1 AND syncStatus != 'SYNCED'")
    suspend fun getDeletedUnsyncedBoards(): List<BoardEntity>

    @Query("UPDATE boards SET syncStatus = 'SYNCED' WHERE id = :boardId")
    suspend fun markSynced(boardId: String)

    @Query("UPDATE boards SET columnOrder = :columnOrder, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :boardId")
    suspend fun updateColumnOrder(boardId: String, columnOrder: String, updatedAt: Long = System.currentTimeMillis())
}
```

### 7.2 `ColumnDao.kt`
```kotlin
@Dao
interface ColumnDao {
    @Query("SELECT * FROM columns WHERE boardId = :boardId AND isDeleted = 0 ORDER BY `order`")
    fun observeColumnsByBoard(boardId: String): Flow<List<ColumnEntity>>

    @Query("SELECT * FROM columns WHERE boardId = :boardId AND isDeleted = 0 ORDER BY `order`")
    suspend fun getColumnsByBoard(boardId: String): List<ColumnEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(column: ColumnEntity)

    @Query("SELECT * FROM columns WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedColumns(): List<ColumnEntity>

    @Query("UPDATE columns SET syncStatus = 'SYNCED' WHERE id = :columnId")
    suspend fun markSynced(columnId: String)

    @Query("UPDATE columns SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :columnId")
    suspend fun softDelete(columnId: String, updatedAt: Long = System.currentTimeMillis())
}
```

### 7.3 `TaskDao.kt`
```kotlin
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE columnId = :columnId AND isDeleted = 0 ORDER BY `order`")
    fun observeTasksByColumn(columnId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE boardId = :boardId AND isDeleted = 0")
    fun observeTasksByBoard(boardId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTask(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isDeleted = 1 AND syncStatus != 'SYNCED'")
    suspend fun getDeletedUnsyncedTasks(): List<TaskEntity>

    @Query("UPDATE tasks SET syncStatus = 'SYNCED' WHERE id = :taskId")
    suspend fun markSynced(taskId: String)

    @Query("SELECT * FROM tasks WHERE reminderTimeMillis IS NOT NULL AND isDeleted = 0")
    suspend fun getTasksWithReminders(): List<TaskEntity>

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :taskId")
    suspend fun softDelete(taskId: String, updatedAt: Long = System.currentTimeMillis())
}
```

### 7.4 `SubtaskDao.kt`
```kotlin
@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId AND isDeleted = 0 ORDER BY `order`")
    fun observeSubtasks(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId AND isDeleted = 0 ORDER BY `order`")
    suspend fun getSubtasksByTask(taskId: String): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subtask: SubtaskEntity)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :subtaskId")
    suspend fun setCompleted(subtaskId: String, isCompleted: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM subtasks WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedSubtasks(): List<SubtaskEntity>

    @Query("UPDATE subtasks SET syncStatus = 'SYNCED' WHERE id = :subtaskId")
    suspend fun markSynced(subtaskId: String)

    @Query("UPDATE subtasks SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :subtaskId")
    suspend fun softDelete(subtaskId: String, updatedAt: Long = System.currentTimeMillis())
}
```

### 7.5 `ReminderDao.kt`
```kotlin
@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId AND isDeleted = 0 ORDER BY nextTriggerMillis")
    fun observeReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isDeleted = 0")
    suspend fun getAllActiveReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedReminders(): List<ReminderEntity>

    @Query("UPDATE reminders SET syncStatus = 'SYNCED' WHERE id = :reminderId")
    suspend fun markSynced(reminderId: String)

    @Query("UPDATE reminders SET nextTriggerMillis = :nextTriggerMillis, updatedAt = :updatedAt WHERE id = :reminderId")
    suspend fun updateNextTrigger(reminderId: String, nextTriggerMillis: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE reminders SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :reminderId")
    suspend fun softDelete(reminderId: String, updatedAt: Long = System.currentTimeMillis())
}
```

---

## 8. AppDatabase

```kotlin
package com.yourname.kanbanapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.yourname.kanbanapp.data.local.dao.*
import com.yourname.kanbanapp.data.local.entity.*
import com.yourname.kanbanapp.data.model.SyncStatus

@Database(
    entities = [
        BoardEntity::class,
        ColumnEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boardDao(): BoardDao
    abstract fun columnDao(): ColumnDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun reminderDao(): ReminderDao
}

class Converters {
    @TypeConverter fun fromSyncStatus(value: SyncStatus): String = value.name
    @TypeConverter fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter fun fromReminderStyle(value: TaskEntity.ReminderStyle): String = value.name
    @TypeConverter fun toTaskReminderStyle(value: String): TaskEntity.ReminderStyle =
        TaskEntity.ReminderStyle.valueOf(value)

    @TypeConverter fun fromEntityReminderStyle(value: ReminderEntity.ReminderStyle): String = value.name
    @TypeConverter fun toEntityReminderStyle(value: String): ReminderEntity.ReminderStyle =
        ReminderEntity.ReminderStyle.valueOf(value)
}
```

**Note**: Room migrations must be added as the schema evolves. For initial development, `fallbackToDestructiveMigration()` is acceptable; remove it before production.

---

## 9. Recurrence Engine

```kotlin
package com.yourname.kanbanapp.domain

import com.yourname.kanbanapp.data.model.RecurrenceRule
import java.util.Calendar

object RecurrenceEngine {

    /**
     * Given a RecurrenceRule and the current time, computes the next
     * trigger time in epoch milliseconds AFTER `afterMillis`.
     * Returns null if the rule is somehow invalid.
     */
    fun nextTriggerAfter(rule: RecurrenceRule, afterMillis: Long): Long? {
        return when (rule.type) {
            RecurrenceRule.RecurrenceType.DAILY -> nextDaily(rule, afterMillis, rule.interval)
            RecurrenceRule.RecurrenceType.EVERY_N_DAYS -> nextDaily(rule, afterMillis, rule.interval)
            RecurrenceRule.RecurrenceType.WEEKLY -> nextWeekly(rule, afterMillis)
            RecurrenceRule.RecurrenceType.MONTHLY -> nextMonthly(rule, afterMillis)
        }
    }

    private fun nextDaily(rule: RecurrenceRule, afterMillis: Long, intervalDays: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = afterMillis }
        // Set to today at trigger time
        cal.set(Calendar.HOUR_OF_DAY, rule.hour)
        cal.set(Calendar.MINUTE, rule.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // If that time is already past, advance by intervalDays
        if (cal.timeInMillis <= afterMillis) {
            cal.add(Calendar.DAY_OF_YEAR, intervalDays)
        }
        return cal.timeInMillis
    }

    private fun nextWeekly(rule: RecurrenceRule, afterMillis: Long): Long? {
        if (rule.daysOfWeek.isEmpty()) return null
        val sortedDays = rule.daysOfWeek.sorted()
        val cal = Calendar.getInstance().apply { timeInMillis = afterMillis }
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun…7=Sat

        cal.set(Calendar.HOUR_OF_DAY, rule.hour)
        cal.set(Calendar.MINUTE, rule.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Find the next day in daysOfWeek that is strictly after afterMillis
        for (day in sortedDays) {
            if (day > currentDayOfWeek || (day == currentDayOfWeek && cal.timeInMillis > afterMillis)) {
                val daysAhead = day - currentDayOfWeek
                cal.add(Calendar.DAY_OF_YEAR, daysAhead)
                return cal.timeInMillis
            }
        }

        // All days this week are past — jump to first day next occurrence week
        val daysUntilNextWeekFirstDay = (7 * rule.interval) - (currentDayOfWeek - sortedDays.first())
        cal.timeInMillis = afterMillis
        cal.set(Calendar.HOUR_OF_DAY, rule.hour)
        cal.set(Calendar.MINUTE, rule.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, daysUntilNextWeekFirstDay)
        return cal.timeInMillis
    }

    private fun nextMonthly(rule: RecurrenceRule, afterMillis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = afterMillis }
        cal.set(Calendar.DAY_OF_MONTH, rule.dayOfMonth.coerceAtMost(
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
        cal.set(Calendar.HOUR_OF_DAY, rule.hour)
        cal.set(Calendar.MINUTE, rule.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= afterMillis) {
            cal.add(Calendar.MONTH, 1)
            // Re-clamp day after month change
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, rule.dayOfMonth.coerceAtMost(maxDay))
        }
        return cal.timeInMillis
    }

    /** Human-readable summary of a rule, e.g. "Every Monday at 09:00" */
    fun describe(rule: RecurrenceRule): String {
        val timeStr = String.format("%02d:%02d", rule.hour, rule.minute)
        return when (rule.type) {
            RecurrenceRule.RecurrenceType.DAILY ->
                if (rule.interval == 1) "Every day at $timeStr"
                else "Every ${rule.interval} days at $timeStr"
            RecurrenceRule.RecurrenceType.EVERY_N_DAYS ->
                "Every ${rule.interval} days at $timeStr"
            RecurrenceRule.RecurrenceType.WEEKLY -> {
                val days = rule.daysOfWeek.joinToString(", ") { dayName(it) }
                if (rule.interval == 1) "Every $days at $timeStr"
                else "Every ${rule.interval} weeks on $days at $timeStr"
            }
            RecurrenceRule.RecurrenceType.MONTHLY ->
                "Monthly on the ${ordinal(rule.dayOfMonth)} at $timeStr"
        }
    }

    private fun dayName(calDay: Int): String = when(calDay) {
        Calendar.SUNDAY -> "Sunday"; Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"; Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"; Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"; else -> "?"
    }

    private fun ordinal(n: Int): String = when {
        n in 11..13 -> "${n}th"
        n % 10 == 1 -> "${n}st"
        n % 10 == 2 -> "${n}nd"
        n % 10 == 3 -> "${n}rd"
        else -> "${n}th"
    }
}
```

---

## 10. Repository Layer

### 10.1 `BoardRepository.kt`
```kotlin
@Singleton
class BoardRepository @Inject constructor(
    private val boardDao: BoardDao,
    private val columnDao: ColumnDao,
    private val workManager: WorkManager
) {
    fun observeBoards(userId: String) = boardDao.observeBoards(userId)
    fun observeBoard(boardId: String) = boardDao.observeBoard(boardId)
    fun observeColumns(boardId: String) = columnDao.observeColumnsByBoard(boardId)

    suspend fun createBoard(userId: String, title: String): BoardEntity {
        val board = BoardEntity(userId = userId, title = title)
        boardDao.upsert(board)
        enqueueSyncWork()
        return board
    }

    suspend fun updateBoard(board: BoardEntity) {
        boardDao.upsert(board.copy(updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING))
        enqueueSyncWork()
    }

    suspend fun deleteBoard(boardId: String) {
        val board = boardDao.getBoardById(boardId) ?: return
        boardDao.upsert(board.copy(isDeleted = true, updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING))
        enqueueSyncWork()
    }

    suspend fun createColumn(boardId: String, title: String): ColumnEntity {
        val existingColumns = columnDao.getColumnsByBoard(boardId)
        val maxOrder = existingColumns.maxOfOrNull { it.order } ?: 0.0
        val column = ColumnEntity(boardId = boardId, title = title, order = maxOrder + 1.0)
        columnDao.upsert(column)

        // Update board's columnOrder JSON
        val board = boardDao.getBoardById(boardId)
        if (board != null) {
            val currentOrder = parseColumnOrder(board.columnOrder).toMutableList()
            currentOrder.add(column.id)
            boardDao.updateColumnOrder(boardId, serializeColumnOrder(currentOrder))
        }
        enqueueSyncWork()
        return column
    }

    suspend fun reorderColumns(boardId: String, newOrderedIds: List<String>) {
        // Update fractional order values
        newOrderedIds.forEachIndexed { index, columnId ->
            val col = columnDao.getColumnsByBoard(boardId).find { it.id == columnId } ?: return@forEachIndexed
            columnDao.upsert(col.copy(order = (index + 1).toDouble(),
                updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING))
        }
        boardDao.updateColumnOrder(boardId, serializeColumnOrder(newOrderedIds))
        enqueueSyncWork()
    }

    suspend fun deleteColumn(columnId: String) {
        columnDao.softDelete(columnId)
        enqueueSyncWork()
    }

    private fun parseColumnOrder(json: String): List<String> =
        try { com.google.gson.Gson().fromJson(json, Array<String>::class.java).toList() }
        catch (e: Exception) { emptyList() }

    private fun serializeColumnOrder(ids: List<String>): String =
        com.google.gson.Gson().toJson(ids)

    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("sync", ExistingWorkPolicy.REPLACE, request)
    }
}
```

### 10.2 `TaskRepository.kt`
```kotlin
@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmScheduler: AlarmScheduler,
    private val workManager: WorkManager
) {
    fun observeTasksByColumn(columnId: String) = taskDao.observeTasksByColumn(columnId)
    fun observeTask(taskId: String) = taskDao.observeTask(taskId)

    suspend fun createTask(boardId: String, columnId: String, title: String,
                           description: String = ""): TaskEntity {
        val tasks = taskDao.observeTasksByColumn(columnId).first()
        val maxOrder = tasks.maxOfOrNull { it.order } ?: 0.0
        val task = TaskEntity(boardId = boardId, columnId = columnId,
            title = title, description = description, order = maxOrder + 1.0)
        taskDao.upsert(task)
        enqueueSyncWork()
        return task
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.upsert(task.copy(updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING))
        // Re-schedule alarm if reminder changed
        task.reminderTimeMillis?.let { time ->
            alarmScheduler.scheduleTaskReminder(task.id, time, task.reminderStyle)
        } ?: alarmScheduler.cancelTaskReminder(task.id)
        enqueueSyncWork()
    }

    /**
     * Moves a task to a new column and/or position.
     * Uses fractional indexing: new order = (orderBefore + orderAfter) / 2
     * Caller provides orderBefore and orderAfter of the surrounding tasks
     * (use 0.0 if inserting at start, use max+1 if inserting at end).
     */
    suspend fun moveTask(taskId: String, newColumnId: String,
                         orderBefore: Double, orderAfter: Double) {
        val task = taskDao.getTaskById(taskId) ?: return
        val newOrder = if (orderAfter <= orderBefore) orderBefore + 1.0
                       else (orderBefore + orderAfter) / 2.0
        taskDao.upsert(task.copy(columnId = newColumnId, order = newOrder,
            updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING))
        enqueueSyncWork()
    }

    suspend fun deleteTask(taskId: String) {
        alarmScheduler.cancelTaskReminder(taskId)
        taskDao.softDelete(taskId)
        enqueueSyncWork()
    }

    /** Called on boot — reschedule all task reminders from Room */
    suspend fun rescheduleAllTaskReminders() {
        taskDao.getTasksWithReminders().forEach { task ->
            task.reminderTimeMillis?.let { time ->
                if (time > System.currentTimeMillis()) {
                    alarmScheduler.scheduleTaskReminder(task.id, time, task.reminderStyle)
                }
            }
        }
    }

    private fun enqueueSyncWork() { /* same as BoardRepository */ }
}
```

### 10.3 `ReminderRepository.kt`
```kotlin
@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val alarmScheduler: AlarmScheduler,
    private val workManager: WorkManager
) {
    fun observeReminders(userId: String) = reminderDao.observeReminders(userId)

    suspend fun createReminder(userId: String, title: String, triggerMillis: Long,
                               style: ReminderEntity.ReminderStyle,
                               recurrenceRule: RecurrenceRule? = null): ReminderEntity {
        val reminder = ReminderEntity(
            userId = userId, title = title, nextTriggerMillis = triggerMillis,
            reminderStyle = style, recurrenceRuleJson = recurrenceRule?.toJson()
        )
        reminderDao.upsert(reminder)
        alarmScheduler.scheduleReminder(reminder)
        enqueueSyncWork()
        return reminder
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.upsert(reminder.copy(updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING))
        alarmScheduler.cancelReminder(reminder.id)
        if (reminder.isEnabled) alarmScheduler.scheduleReminder(reminder)
        enqueueSyncWork()
    }

    suspend fun deleteReminder(reminderId: String) {
        alarmScheduler.cancelReminder(reminderId)
        reminderDao.softDelete(reminderId)
        enqueueSyncWork()
    }

    /** Called after an alarm fires to advance nextTrigger for recurring reminders */
    suspend fun onReminderFired(reminderId: String) {
        val reminder = reminderDao.getReminderById(reminderId) ?: return
        val ruleJson = reminder.recurrenceRuleJson
        if (ruleJson != null) {
            val rule = RecurrenceRule.fromJson(ruleJson)
            val nextTrigger = RecurrenceEngine.nextTriggerAfter(rule, System.currentTimeMillis())
            if (nextTrigger != null) {
                reminderDao.updateNextTrigger(reminderId, nextTrigger)
                val updated = reminder.copy(nextTriggerMillis = nextTrigger)
                alarmScheduler.scheduleReminder(updated)
            }
        }
        // One-shot reminders: do nothing (alarm already fired, no reschedule)
    }

    /** Called on boot — reschedule all enabled reminders */
    suspend fun rescheduleAllReminders() {
        reminderDao.getAllActiveReminders().forEach { reminder ->
            if (reminder.nextTriggerMillis > System.currentTimeMillis()) {
                alarmScheduler.scheduleReminder(reminder)
            } else {
                // Missed fire (device was off) — advance recurrence if applicable
                val ruleJson = reminder.recurrenceRuleJson
                if (ruleJson != null) {
                    val rule = RecurrenceRule.fromJson(ruleJson)
                    val nextTrigger = RecurrenceEngine.nextTriggerAfter(rule, System.currentTimeMillis())
                    if (nextTrigger != null) {
                        reminderDao.updateNextTrigger(reminder.id, nextTrigger)
                        alarmScheduler.scheduleReminder(reminder.copy(nextTriggerMillis = nextTrigger))
                    }
                }
            }
        }
    }

    private fun enqueueSyncWork() { /* same pattern as BoardRepository */ }
}
```

---

## 11. Firebase Auth

### 11.1 `AuthRepository.kt`
```kotlin
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUserId: String? get() = auth.currentUser?.uid

    /**
     * Called on every app launch. Signs in anonymously if no user exists.
     * The UID persists across restarts via SharedPreferences.
     */
    suspend fun ensureAuthenticated(): String {
        val user = auth.currentUser
        if (user != null) return user.uid
        val result = auth.signInAnonymously().await()
        return result.user!!.uid
    }

    /**
     * Links anonymous account to Google.
     * Preserves UID and all Firestore data.
     * Throws FirebaseAuthUserCollisionException if Google account
     * already linked to another Firebase user (handle in UI).
     */
    suspend fun linkWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.currentUser!!.linkWithCredential(credential).await()
    }

    fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous ?: true

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}
```

### 11.2 Firebase Emulator Connection (in `KanbanApplication.kt`)
```kotlin
@HiltAndroidApp
class KanbanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.USE_EMULATOR) {
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
        }
    }
}
```

---

## 12. Firestore Sync Layer

### 12.1 Firestore Data Structure
```
users/{userId}/
  boards/{boardId}
    → title, columnOrder, createdAt, updatedAt (serverTimestamp), isDeleted

  boards/{boardId}/columns/{columnId}
    → boardId, title, order, updatedAt, isDeleted

  boards/{boardId}/tasks/{taskId}
    → boardId, columnId, title, description, order,
      reminderTimeMillis, reminderStyle, updatedAt, isDeleted

  boards/{boardId}/tasks/{taskId}/subtasks/{subtaskId}
    → taskId, title, isCompleted, order, updatedAt, isDeleted

  reminders/{reminderId}
    → title, nextTriggerMillis, reminderStyle, recurrenceRuleJson,
      isEnabled, createdAt, updatedAt, isDeleted
```

### 12.2 Firestore Security Rules
Deploy these rules in the Firebase console (Firestore → Rules tab):
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null
                         && request.auth.uid == userId;
    }
  }
}
```

### 12.3 `SyncWorker.kt`
```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val boardDao: BoardDao,
    private val columnDao: ColumnDao,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val reminderDao: ReminderDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val prefs: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.failure()
        return try {
            pushPendingChanges(userId)
            pullRemoteChanges(userId)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun pushPendingChanges(userId: String) {
        val userRef = firestore.collection("users").document(userId)

        // Push boards
        boardDao.getUnsyncedBoards().forEach { board ->
            userRef.collection("boards").document(board.id)
                .set(board.toFirestoreMap(), SetOptions.merge()).await()
            boardDao.markSynced(board.id)
        }
        // Push soft-deleted boards
        boardDao.getDeletedUnsyncedBoards().forEach { board ->
            userRef.collection("boards").document(board.id)
                .set(mapOf("isDeleted" to true, "updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()).await()
            boardDao.markSynced(board.id)
        }

        // Push columns, tasks, subtasks, reminders — same pattern
        columnDao.getUnsyncedColumns().forEach { col ->
            userRef.collection("boards").document(col.boardId)
                .collection("columns").document(col.id)
                .set(col.toFirestoreMap(), SetOptions.merge()).await()
            columnDao.markSynced(col.id)
        }

        taskDao.getUnsyncedTasks().forEach { task ->
            userRef.collection("boards").document(task.boardId)
                .collection("tasks").document(task.id)
                .set(task.toFirestoreMap(), SetOptions.merge()).await()
            taskDao.markSynced(task.id)
        }

        subtaskDao.getUnsyncedSubtasks().forEach { subtask ->
            // taskId needed to navigate path — fetch parent task
            val task = taskDao.getTaskById(subtask.taskId) ?: return@forEach
            userRef.collection("boards").document(task.boardId)
                .collection("tasks").document(subtask.taskId)
                .collection("subtasks").document(subtask.id)
                .set(subtask.toFirestoreMap(), SetOptions.merge()).await()
            subtaskDao.markSynced(subtask.id)
        }

        reminderDao.getUnsyncedReminders().forEach { reminder ->
            userRef.collection("reminders").document(reminder.id)
                .set(reminder.toFirestoreMap(), SetOptions.merge()).await()
            reminderDao.markSynced(reminder.id)
        }
    }

    private suspend fun pullRemoteChanges(userId: String) {
        val lastSync = prefs.getLastSyncTimestamp()
        val userRef = firestore.collection("users").document(userId)

        // Pull boards modified after lastSync
        userRef.collection("boards")
            .whereGreaterThan("updatedAt", Timestamp(lastSync / 1000, 0))
            .get().await().documents.forEach { doc ->
                val board = doc.toBoardEntity(userId)
                boardDao.upsert(board.copy(syncStatus = SyncStatus.SYNCED))
            }

        // Pull tasks — Firestore collectionGroup query for all tasks across boards
        firestore.collectionGroup("tasks")
            .whereEqualTo("userId", userId)  // store userId on task documents too
            .whereGreaterThan("updatedAt", Timestamp(lastSync / 1000, 0))
            .get().await().documents.forEach { doc ->
                val task = doc.toTaskEntity()
                taskDao.upsert(task.copy(syncStatus = SyncStatus.SYNCED))
            }

        // Similar pulls for columns, subtasks, reminders

        prefs.setLastSyncTimestamp(System.currentTimeMillis())
    }
}

// Extension functions for Firestore mapping (implement in same file or a separate mapper file)
// BoardEntity.toFirestoreMap(), DocumentSnapshot.toBoardEntity(), etc.
// Each maps the entity fields to/from Firestore document fields.
// Include "updatedAt" as FieldValue.serverTimestamp() on write.
```

> **Implementation note**: Write `toFirestoreMap()` extension functions for each entity and corresponding `DocumentSnapshot.toXxxEntity()` parsers. Keep them in a `FirestoreMappers.kt` file. For `updatedAt` on writes, use `FieldValue.serverTimestamp()` to let the server set it — this ensures consistent last-write-wins ordering.

---

## 13. AlarmManager & Notifications

### 13.1 `NotificationHelper.kt`
```kotlin
object NotificationHelper {
    const val CHANNEL_ALARM = "channel_alarm"        // Persistent alarm-style
    const val CHANNEL_NOTIFICATION = "channel_notification" // Standard

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        // Alarm channel: max importance, alarm sound, no bypass of DND
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM, "Alarms", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Persistent reminders that require dismissal"
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            enableVibration(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        // Notification channel: high importance, notification sound
        val notifChannel = NotificationChannel(
            CHANNEL_NOTIFICATION, "Reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Standard reminder notifications"
            enableVibration(true)
        }

        manager.createNotificationChannel(alarmChannel)
        manager.createNotificationChannel(notifChannel)
    }

    fun showAlarmNotification(context: Context, id: String, title: String,
                              notificationId: Int, style: ReminderEntity.ReminderStyle) {
        val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_DISMISS"
            putExtra("reminder_id", id)
            putExtra("notification_id", notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 1000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 2000,
            Intent(context, ReminderReceiver::class.java).apply {
                action = "ACTION_SNOOZE"
                putExtra("reminder_id", id)
                putExtra("notification_id", notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channel = if (style == ReminderEntity.ReminderStyle.ALARM)
            CHANNEL_ALARM else CHANNEL_NOTIFICATION

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification) // Create this vector drawable
            .setContentTitle(title)
            .setContentText("Tap to dismiss")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissPendingIntent)
            .addAction(R.drawable.ic_snooze, "Snooze 10 min", snoozePendingIntent)

        // Alarm style: make it ongoing (can't swipe away)
        if (style == ReminderEntity.ReminderStyle.ALARM) {
            builder.setOngoing(true)

            // Full-screen intent for Android < 14 (locked screen takeover)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
                    putExtra("reminder_id", id)
                    putExtra("notification_id", notificationId)
                    putExtra("title", title)
                }
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context, notificationId + 3000, fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setFullScreenIntent(fullScreenPendingIntent, true)
            }
        }

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, builder.build())
    }
}
```

### 13.2 `AlarmScheduler.kt`
```kotlin
@Singleton
class AlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    // Reminder alarms use the reminder's hashCode as notification ID
    // Task reminder alarms use a separate request code space

    fun scheduleReminder(reminder: ReminderEntity) {
        schedule(
            requestCode = reminderRequestCode(reminder.id),
            triggerMillis = reminder.nextTriggerMillis,
            intent = buildReminderIntent(reminder.id, reminder.title,
                reminder.reminderStyle.toTaskStyle())
        )
    }

    fun scheduleTaskReminder(taskId: String, triggerMillis: Long,
                             style: TaskEntity.ReminderStyle) {
        schedule(
            requestCode = taskRequestCode(taskId),
            triggerMillis = triggerMillis,
            intent = buildTaskReminderIntent(taskId, style)
        )
    }

    fun cancelReminder(reminderId: String) {
        cancel(reminderRequestCode(reminderId))
    }

    fun cancelTaskReminder(taskId: String) {
        cancel(taskRequestCode(taskId))
    }

    private fun schedule(requestCode: Int, triggerMillis: Long, intent: Intent) {
        val alarmManager = context.getSystemService<AlarmManager>()!!
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            else -> {
                // Fallback: 10-minute window. Guide user to grant SCHEDULE_EXACT_ALARM.
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP, triggerMillis, 600_000L, pendingIntent)
            }
        }
    }

    private fun cancel(requestCode: Int) {
        val alarmManager = context.getSystemService<AlarmManager>()!!
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
    }

    private fun buildReminderIntent(id: String, title: String,
                                    style: TaskEntity.ReminderStyle) =
        Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_FIRE_REMINDER"
            putExtra("reminder_id", id)
            putExtra("title", title)
            putExtra("type", "reminder")
            putExtra("style", style.name)
        }

    private fun buildTaskReminderIntent(taskId: String, style: TaskEntity.ReminderStyle) =
        Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_FIRE_REMINDER"
            putExtra("reminder_id", taskId)
            putExtra("type", "task")
            putExtra("style", style.name)
        }

    // Use stable int codes derived from string IDs
    private fun reminderRequestCode(id: String): Int = id.hashCode() and 0x7FFFFFFF
    private fun taskRequestCode(id: String): Int = (id + "_task").hashCode() and 0x7FFFFFFF

    private fun ReminderEntity.ReminderStyle.toTaskStyle() =
        if (this == ReminderEntity.ReminderStyle.ALARM) TaskEntity.ReminderStyle.ALARM
        else TaskEntity.ReminderStyle.NOTIFICATION
}
```

### 13.3 `ReminderReceiver.kt`
```kotlin
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_FIRE_REMINDER" -> handleFire(context, intent)
            "ACTION_DISMISS" -> handleDismiss(context, intent)
            "ACTION_SNOOZE" -> handleSnooze(context, intent)
        }
    }

    private fun handleFire(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminder_id") ?: return
        val title = intent.getStringExtra("title") ?: "Reminder"
        val styleStr = intent.getStringExtra("style") ?: "ALARM"
        val style = try { ReminderEntity.ReminderStyle.valueOf(styleStr) }
                    catch (e: Exception) { ReminderEntity.ReminderStyle.ALARM }
        val notificationId = reminderId.hashCode() and 0x7FFFFFFF

        NotificationHelper.showAlarmNotification(context, reminderId, title, notificationId, style)

        // Update next trigger for recurring reminders (run in a coroutine scope)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val db = EntryPoint.get(context) // Use Hilt EntryPoint to get repository
            db.reminderRepository().onReminderFired(reminderId)
        }
    }

    private fun handleDismiss(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", -1)
        if (notificationId != -1) {
            context.getSystemService(NotificationManager::class.java).cancel(notificationId)
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminder_id") ?: return
        val notificationId = intent.getIntExtra("notification_id", -1)
        // Cancel current notification
        if (notificationId != -1) {
            context.getSystemService(NotificationManager::class.java).cancel(notificationId)
        }
        // Re-schedule 10 minutes from now
        val snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000L
        val alarmManager = context.getSystemService<AlarmManager>()!!
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminderId.hashCode() and 0x7FFFFFFF,
            Intent(context, ReminderReceiver::class.java).apply {
                action = "ACTION_FIRE_REMINDER"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
    }
}

// Hilt EntryPoint for use inside BroadcastReceiver
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun reminderRepository(): ReminderRepository
    fun taskRepository(): TaskRepository
}
```

### 13.4 `BootReceiver.kt`
```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val relevantActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED
        )
        if (intent.action !in relevantActions) return

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext, ReceiverEntryPoint::class.java)
            entryPoint.reminderRepository().rescheduleAllReminders()
            entryPoint.taskRepository().rescheduleAllTaskReminders()
        }
    }
}
```

### 13.5 `AlarmAlertActivity.kt`
This activity is shown as a full-screen alert on Android < 14 when the device is locked.
```kotlin
@AndroidEntryPoint
class AlarmAlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val title = intent.getStringExtra("title") ?: "Reminder"
        val notificationId = intent.getIntExtra("notification_id", -1)
        val reminderId = intent.getStringExtra("reminder_id") ?: ""

        setContent {
            // Simple full-screen UI with title, Dismiss and Snooze buttons
            // On dismiss: cancel notification, finish activity
            // On snooze: send broadcast to ReminderReceiver with ACTION_SNOOZE, finish
            AlarmAlertScreen(
                title = title,
                onDismiss = {
                    if (notificationId != -1)
                        getSystemService(NotificationManager::class.java).cancel(notificationId)
                    finish()
                },
                onSnooze = {
                    sendBroadcast(Intent(this, ReminderReceiver::class.java).apply {
                        action = "ACTION_SNOOZE"
                        putExtra("reminder_id", reminderId)
                        putExtra("notification_id", notificationId)
                    })
                    finish()
                }
            )
        }
    }
}
```

---

## 14. Hilt Dependency Injection

### 14.1 `DatabaseModule.kt`
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "kanban_db")
            .fallbackToDestructiveMigration() // Remove before production
            .build()

    @Provides fun provideBoardDao(db: AppDatabase) = db.boardDao()
    @Provides fun provideColumnDao(db: AppDatabase) = db.columnDao()
    @Provides fun provideTaskDao(db: AppDatabase) = db.taskDao()
    @Provides fun provideSubtaskDao(db: AppDatabase) = db.subtaskDao()
    @Provides fun provideReminderDao(db: AppDatabase) = db.reminderDao()
}
```

### 14.2 `FirebaseModule.kt`
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    @Provides @Singleton fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
```

### 14.3 `WorkerModule.kt`
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    @Provides @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
```

---

## 15. Navigation Structure

### 15.1 `Screen.kt`
```kotlin
sealed class Screen(val route: String) {
    // Bottom nav destinations
    object PinnedBoard : Screen("pinned_board")
    object BoardList : Screen("board_list")
    object Reminders : Screen("reminders")

    // Sub-screens (not in bottom nav)
    object KanbanBoard : Screen("board/{boardId}") {
        fun createRoute(boardId: String) = "board/$boardId"
    }
    object TaskDetail : Screen("task/{taskId}") {
        fun createRoute(taskId: String) = "task/$taskId"
    }
    object AddEditReminder : Screen("reminder/edit?reminderId={reminderId}") {
        fun createRoute(reminderId: String? = null) =
            if (reminderId != null) "reminder/edit?reminderId=$reminderId"
            else "reminder/edit"
    }
    object Settings : Screen("settings")
}
```

### 15.2 `AppNavigation.kt`
```kotlin
@Composable
fun AppNavigation(navController: NavHostController, startDestination: String) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Screen.PinnedBoard.route,
                    onClick = { navController.navigate(Screen.PinnedBoard.route) },
                    icon = { Icon(Icons.Default.Star, "Pinned Board") },
                    label = { Text("Board") }
                )
                NavigationBarItem(
                    selected = currentRoute == Screen.BoardList.route,
                    onClick = { navController.navigate(Screen.BoardList.route) },
                    icon = { Icon(Icons.Default.GridView, "All Boards") },
                    label = { Text("Boards") }
                )
                NavigationBarItem(
                    selected = currentRoute == Screen.Reminders.route,
                    onClick = { navController.navigate(Screen.Reminders.route) },
                    icon = { Icon(Icons.Default.Notifications, "Reminders") },
                    label = { Text("Reminders") }
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = startDestination,
                modifier = Modifier.padding(padding)) {
            composable(Screen.PinnedBoard.route) { PinnedBoardScreen(navController) }
            composable(Screen.BoardList.route) { BoardListScreen(navController) }
            composable(Screen.Reminders.route) { RemindersScreen(navController) }
            composable(Screen.KanbanBoard.route,
                arguments = listOf(navArgument("boardId") { type = NavType.StringType })
            ) { backStackEntry ->
                KanbanBoardScreen(
                    boardId = backStackEntry.arguments!!.getString("boardId")!!,
                    navController = navController
                )
            }
            composable(Screen.TaskDetail.route,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType })
            ) { backStackEntry ->
                TaskDetailScreen(
                    taskId = backStackEntry.arguments!!.getString("taskId")!!,
                    navController = navController
                )
            }
            composable(Screen.AddEditReminder.route,
                arguments = listOf(navArgument("reminderId") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                })
            ) { backStackEntry ->
                AddEditReminderScreen(
                    reminderId = backStackEntry.arguments?.getString("reminderId"),
                    navController = navController
                )
            }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
        }
    }
}
```

**Pinned Board Screen**: `PinnedBoardScreen` is a thin wrapper — it reads the `pinnedBoardId` from `UserPreferencesRepository`. If set, it renders `KanbanBoardScreen(boardId = pinnedBoardId)` inline. If not set, it shows a prompt: "No board pinned — go to Boards and pin one."

---

## 16. ViewModels

### 16.1 `BoardListViewModel.kt`
```kotlin
@HiltViewModel
class BoardListViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val userId = authRepository.currentUserId ?: ""

    val boards: StateFlow<List<BoardEntity>> =
        boardRepository.observeBoards(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedBoardId: StateFlow<String?> =
        prefsRepository.pinnedBoardId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun createBoard(title: String) {
        viewModelScope.launch { boardRepository.createBoard(userId, title) }
    }

    fun deleteBoard(boardId: String) {
        viewModelScope.launch { boardRepository.deleteBoard(boardId) }
    }

    fun pinBoard(boardId: String) {
        viewModelScope.launch { prefsRepository.setPinnedBoardId(boardId) }
    }

    fun unpinBoard() {
        viewModelScope.launch { prefsRepository.setPinnedBoardId(null) }
    }
}
```

### 16.2 `KanbanBoardViewModel.kt`
```kotlin
@HiltViewModel
class KanbanBoardViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val taskRepository: TaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val boardId: String = checkNotNull(savedStateHandle["boardId"])

    val board: StateFlow<BoardEntity?> =
        boardRepository.observeBoard(boardId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val columns: StateFlow<List<ColumnEntity>> =
        boardRepository.observeColumns(boardId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map of columnId → tasks for that column
    // Combine column list with individual column task flows
    val tasksByColumn: StateFlow<Map<String, List<TaskEntity>>> =
        columns.flatMapLatest { cols ->
            if (cols.isEmpty()) flowOf(emptyMap())
            else combine(cols.map { col ->
                taskRepository.observeTasksByColumn(col.id)
                    .map { tasks -> col.id to tasks }
            }) { pairs -> pairs.toMap() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun createTask(columnId: String, title: String) {
        viewModelScope.launch { taskRepository.createTask(boardId, columnId, title) }
    }

    fun moveTask(taskId: String, targetColumnId: String,
                 orderBefore: Double, orderAfter: Double) {
        viewModelScope.launch {
            taskRepository.moveTask(taskId, targetColumnId, orderBefore, orderAfter)
        }
    }

    fun addColumn(title: String) {
        viewModelScope.launch { boardRepository.createColumn(boardId, title) }
    }

    fun deleteColumn(columnId: String) {
        viewModelScope.launch { boardRepository.deleteColumn(columnId) }
    }

    fun reorderColumns(newOrderedIds: List<String>) {
        viewModelScope.launch { boardRepository.reorderColumns(boardId, newOrderedIds) }
    }
}
```

### 16.3 `TaskDetailViewModel.kt`
```kotlin
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val subtaskRepository: SubtaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])

    val task: StateFlow<TaskEntity?> =
        taskRepository.observeTask(taskId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subtasks: StateFlow<List<SubtaskEntity>> =
        subtaskRepository.observeSubtasks(taskId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTitle(title: String) {
        viewModelScope.launch {
            task.value?.let { taskRepository.updateTask(it.copy(title = title)) }
        }
    }

    fun updateDescription(description: String) {
        viewModelScope.launch {
            task.value?.let { taskRepository.updateTask(it.copy(description = description)) }
        }
    }

    fun setReminder(timeMillis: Long, style: TaskEntity.ReminderStyle) {
        viewModelScope.launch {
            task.value?.let {
                taskRepository.updateTask(it.copy(reminderTimeMillis = timeMillis,
                    reminderStyle = style))
            }
        }
    }

    fun clearReminder() {
        viewModelScope.launch {
            task.value?.let {
                taskRepository.updateTask(it.copy(reminderTimeMillis = null))
            }
        }
    }

    fun addSubtask(title: String) {
        viewModelScope.launch { subtaskRepository.createSubtask(taskId, title) }
    }

    fun toggleSubtask(subtaskId: String, isCompleted: Boolean) {
        viewModelScope.launch { subtaskRepository.setCompleted(subtaskId, isCompleted) }
    }

    fun deleteSubtask(subtaskId: String) {
        viewModelScope.launch { subtaskRepository.deleteSubtask(subtaskId) }
    }

    fun deleteTask() {
        viewModelScope.launch { taskRepository.deleteTask(taskId) }
    }
}
```

### 16.4 `RemindersViewModel.kt`
```kotlin
@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val userId = authRepository.currentUserId ?: ""

    val reminders: StateFlow<List<ReminderEntity>> =
        reminderRepository.observeReminders(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch { reminderRepository.deleteReminder(reminderId) }
    }

    fun toggleEnabled(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderRepository.updateReminder(reminder.copy(isEnabled = !reminder.isEnabled))
        }
    }
}
```

### 16.5 `AddEditReminderViewModel.kt`
```kotlin
@HiltViewModel
class AddEditReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val existingReminderId: String? = savedStateHandle["reminderId"]
    private val userId = authRepository.currentUserId ?: ""

    // Mutable UI state
    var title by mutableStateOf("")
    var selectedDateTime by mutableStateOf<Long>(System.currentTimeMillis() + 3600_000L)
    var selectedStyle by mutableStateOf(ReminderEntity.ReminderStyle.ALARM)
    var recurrenceRule by mutableStateOf<RecurrenceRule?>(null)
    var isRecurring by mutableStateOf(false)

    init {
        if (existingReminderId != null) {
            viewModelScope.launch {
                val reminder = reminderRepository.getReminderById(existingReminderId) ?: return@launch
                title = reminder.title
                selectedDateTime = reminder.nextTriggerMillis
                selectedStyle = reminder.reminderStyle
                recurrenceRule = reminder.recurrenceRuleJson?.let { RecurrenceRule.fromJson(it) }
                isRecurring = recurrenceRule != null
            }
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (existingReminderId == null) {
                reminderRepository.createReminder(
                    userId = userId, title = title, triggerMillis = selectedDateTime,
                    style = selectedStyle, recurrenceRule = if (isRecurring) recurrenceRule else null
                )
            } else {
                val existing = reminderRepository.getReminderById(existingReminderId) ?: return@launch
                reminderRepository.updateReminder(existing.copy(
                    title = title, nextTriggerMillis = selectedDateTime,
                    reminderStyle = selectedStyle,
                    recurrenceRuleJson = if (isRecurring) recurrenceRule?.toJson() else null
                ))
            }
            onSuccess()
        }
    }
}
```

---

## 17. Screens — Boards Feature

### 17.1 `BoardListScreen.kt`
UI elements:
- `LazyColumn` of board cards, each showing board title
- Long-press or swipe to reveal: Delete, Pin/Unpin options
- FAB: "+" to create new board → shows a dialog/bottom sheet with a title text field
- Each board card has a ⭐ icon to pin/unpin it (filled star = pinned, outline = not pinned)
- Tap board card → navigate to `Screen.KanbanBoard.createRoute(boardId)`

### 17.2 `KanbanBoardScreen.kt`
This is the main Kanban view. Structure:

```kotlin
@Composable
fun KanbanBoardScreen(boardId: String, navController: NavController,
                      viewModel: KanbanBoardViewModel = hiltViewModel()) {
    val columns by viewModel.columns.collectAsStateWithLifecycle()
    val tasksByColumn by viewModel.tasksByColumn.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(board?.title ?: "") },
                actions = {
                    IconButton(onClick = { showColumnConfigSheet = true }) {
                        Icon(Icons.Default.Settings, "Configure columns")
                    }
                }
            )
        }
    ) {
        // Drag-and-drop state
        var draggingTaskId by remember { mutableStateOf<String?>(null) }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(columns, key = { it.id }) { column ->
                KanbanColumn(
                    column = column,
                    tasks = tasksByColumn[column.id] ?: emptyList(),
                    draggingTaskId = draggingTaskId,
                    onDragStarted = { draggingTaskId = it },
                    onDragEnded = { draggingTaskId = null },
                    onCardDropped = { taskId, targetColumnId, orderBefore, orderAfter ->
                        viewModel.moveTask(taskId, targetColumnId, orderBefore, orderAfter)
                    },
                    onCardTapped = { taskId ->
                        navController.navigate(Screen.TaskDetail.createRoute(taskId))
                    },
                    onAddCard = { title -> viewModel.createTask(column.id, title) }
                )
            }
            // "Add column" button at end of row
            item {
                AddColumnButton { viewModel.addColumn(it) }
            }
        }
    }
}
```

### 17.3 `KanbanColumn.kt` (drag-and-drop)
Use two mechanisms together:
1. **Within-column reorder**: `sh.calvin.reorderable.ReorderableColumn` from the `reorderable` library. This handles drag-to-reorder within the same list.
2. **Cross-column drag**: `Modifier.dragAndDropSource` on each card and `Modifier.dragAndDropTarget` on each column. The `ClipData` carries the `taskId`.

```kotlin
@Composable
fun KanbanColumn(
    column: ColumnEntity,
    tasks: List<TaskEntity>,
    onCardDropped: (taskId: String, targetColumnId: String,
                    orderBefore: Double, orderAfter: Double) -> Unit,
    // ... other params
) {
    var isDropTarget by remember { mutableStateOf(false) }

    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val taskId = event.toAndroidDragEvent().clipData.getItemAt(0).text.toString()
                // Insert at end of column
                val maxOrder = tasks.maxOfOrNull { it.order } ?: 0.0
                onCardDropped(taskId, column.id, maxOrder, maxOrder + 2.0)
                isDropTarget = false
                return true
            }
            override fun onEntered(event: DragAndDropEvent) { isDropTarget = true }
            override fun onExited(event: DragAndDropEvent) { isDropTarget = false }
            override fun onEnded(event: DragAndDropEvent) { isDropTarget = false }
        }
    }

    Card(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dropTarget)
            .then(if (isDropTarget) Modifier.border(2.dp, MaterialTheme.colorScheme.primary,
                RoundedCornerShape(12.dp)) else Modifier)
    ) {
        Column {
            // Column header with title + delete button
            ColumnHeader(column)

            // Within-column reorderable list
            val reorderState = rememberReorderableLazyListState(
                onMove = { from, to ->
                    // Compute fractional order for moved item
                    val reorderedTasks = tasks.toMutableList().apply { add(to.index, removeAt(from.index)) }
                    val movedTask = reorderedTasks[to.index]
                    val before = reorderedTasks.getOrNull(to.index - 1)?.order ?: 0.0
                    val after = reorderedTasks.getOrNull(to.index + 1)?.order ?: (before + 2.0)
                    onCardDropped(movedTask.id, column.id, before, after)
                }
            )

            LazyColumn(state = reorderState.listState) {
                items(tasks, key = { it.id }) { task ->
                    ReorderableItem(reorderState, key = task.id) { isDragging ->
                        TaskCard(
                            task = task,
                            isDragging = isDragging,
                            modifier = Modifier
                                .longPressDraggable(reorderState) // within-column
                                .dragAndDropSource {             // cross-column
                                    detectTapGestures(onLongPress = {
                                        startTransfer(DragAndDropTransferData(
                                            ClipData.newPlainText("taskId", task.id)
                                        ))
                                    })
                                },
                            onTap = { onCardTapped(task.id) }
                        )
                    }
                }
                // "Add card" row at bottom
                item {
                    AddCardRow { onAddCard(it) }
                }
            }
        }
    }
}
```

> **Implementation note**: The interaction between `ReorderableItem` and `dragAndDropSource` requires care. The long-press gesture for within-column reorder and the long-press for cross-column must not conflict. Use `longPressDraggable(reorderState)` for within-column (provided by the reorderable library) and `dragAndDropSource` with `detectTapGestures(onLongPress = {...})` for cross-column. Test that both work reliably.

### 17.4 `TaskCard.kt`
- Shows: task `title` (prominent), `description` (subtitle, single line, ellipsized)
- If subtasks present: small progress indicator (e.g. "2/4 ✓")
- If reminder set: small bell icon with the date
- Tap → navigate to TaskDetail

### 17.5 `TaskDetailScreen.kt`
Full-screen or modal sheet with:
- Editable **title** text field
- Editable **description** text field (multiline)
- **Checklist section**: list of `SubtaskItem` rows (checkbox + title), editable inline; "Add item" row at bottom
- **Reminder section**: shows current reminder if set. "Set reminder" button → date/time picker dialog + alarm/notification toggle. "Clear reminder" option.
- **Delete task** button (bottom, destructive colour)

### 17.6 `ColumnConfigSheet.kt`
Bottom sheet showing:
- Reorderable list of current columns (long-press drag handle to reorder)
- Each row: column name (editable inline tap), delete button
- "Add column" field at bottom
- Uses same `reorderable` library for the list

---

## 18. Screens — Reminders Feature

### 18.1 `RemindersScreen.kt`
- List of `ReminderEntity` items, sorted by `nextTriggerMillis`
- Each item (`ReminderItem`): title, next trigger time, recurrence description (from `RecurrenceEngine.describe()`), alarm/notification icon, enabled toggle switch
- Swipe left to delete (with undo snackbar)
- FAB: "+" → navigate to `Screen.AddEditReminder.createRoute()`
- Tap existing reminder → `Screen.AddEditReminder.createRoute(reminderId)`

### 18.2 `AddEditReminderScreen.kt`
Form with:
1. **Title** text field (required)
2. **Date & Time picker** — use Material3 `DatePickerDialog` + `TimePicker` composables. Show selected date/time as text, tap to change.
3. **Reminder style toggle**: two-button row — "🔔 Alarm" | "📳 Notification". Alarm = persistent + sound; Notification = standard.
4. **Recurring toggle switch**: "Repeat"
5. **Recurrence picker** (visible only when recurring = true): `RecurrencePicker` composable (see below)
6. **Save** button

### 18.3 `RecurrencePicker.kt`
This is the most complex UI component. It should render as a card with these fields:

```
Repeat every: [N] [days / weeks / months]
              (number input) (dropdown)

On: [Mon] [Tue] [Wed] [Thu] [Fri] [Sat] [Sun]
    (day-of-week chip toggles — visible when weeks selected)

Day of month: [1–28 number input — visible when months selected]

At: [HH] : [MM]
    (time input, or re-use the time from the main date/time picker)
```

Internally, the composable outputs a `RecurrenceRule` to its parent via `onRuleChanged: (RecurrenceRule) -> Unit` callback.

---

## 19. Screens — Settings

`SettingsScreen.kt` contains:
- **Account section**: shows "Signed in anonymously" or Google account email. Button: "Link Google Account" (triggers Credential Manager Google Sign-In flow) or "Sign out" (with warning that anonymous accounts cannot be recovered).
- **Pinned board**: dropdown to select which board is pinned (reads from `UserPreferencesRepository`).
- **Exact alarm permission** banner (visible if `!alarmManager.canScheduleExactAlarms()` on API 31+): "Tap to grant exact alarm permission" → opens `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` intent.
- **Notification permission** (visible if not granted on API 33+): "Tap to allow notifications" → `requestPermissions`.
- **About** section: version name.

---

## 20. User Preferences (DataStore)

```kotlin
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.createDataStoreWithDefaults()

    private object Keys {
        val PINNED_BOARD_ID = stringPreferencesKey("pinned_board_id")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    val pinnedBoardId: Flow<String?> = dataStore.data.map { it[Keys.PINNED_BOARD_ID] }

    suspend fun setPinnedBoardId(boardId: String?) {
        dataStore.edit { prefs ->
            if (boardId == null) prefs.remove(Keys.PINNED_BOARD_ID)
            else prefs[Keys.PINNED_BOARD_ID] = boardId
        }
    }

    suspend fun getLastSyncTimestamp(): Long =
        dataStore.data.first()[Keys.LAST_SYNC_TIMESTAMP] ?: 0L

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { it[Keys.LAST_SYNC_TIMESTAMP] = timestamp }
    }
}

// Extension to create DataStore
private fun Context.createDataStoreWithDefaults() =
    createDataStore(name = "user_preferences")
```

---

## 21. AndroidManifest

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
    <!-- API 33+ runtime permission for notifications -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <!-- Foreground service for alarm (if needed) -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

    <application
        android:name=".KanbanApplication"
        android:allowBackup="true"
        android:usesCleartextTraffic="true"> <!-- for emulator; remove in production -->

        <activity android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Full-screen alarm alert activity -->
        <activity
            android:name=".alarm.AlarmAlertActivity"
            android:exported="false"
            android:showOnLockScreen="true"
            android:turnScreenOn="true"
            android:theme="@style/Theme.App.Fullscreen" />

        <!-- Alarm receiver -->
        <receiver
            android:name=".alarm.ReminderReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="ACTION_FIRE_REMINDER" />
                <action android:name="ACTION_DISMISS" />
                <action android:name="ACTION_SNOOZE" />
            </intent-filter>
        </receiver>

        <!-- Boot receiver -->
        <receiver
            android:name=".alarm.BootReceiver"
            android:exported="true"
            android:permission="android.permission.RECEIVE_BOOT_COMPLETED">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
                <action android:name="android.intent.action.TIMEZONE_CHANGED" />
                <action android:name="android.intent.action.TIME_SET" />
            </intent-filter>
        </receiver>

        <!-- WorkManager initializer -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false">
            <meta-data
                android:name="androidx.work.impl.WorkManagerInitializer"
                android:value="androidx.startup" />
        </provider>

        <!-- Firebase (no explicit entries needed — handled by google-services plugin) -->
    </application>

</manifest>
```

---

## 22. Implementation Order (Phases)

Follow these phases in order. Each phase should be independently buildable and testable.

---

### Phase 1 — Foundation (no UI yet)
**Goal**: App compiles, Room works, Firebase connects.

1. Create Android project with settings from Section 4.
2. Add `google-services.json` to `app/`.
3. Configure `libs.versions.toml` and `app/build.gradle.kts`.
4. Create `KanbanApplication.kt` with Hilt annotation and Firebase emulator wiring.
5. Implement all Room entities (Section 6).
6. Implement `Converters.kt` and `AppDatabase.kt` (Section 8).
7. Implement all DAOs (Section 7).
8. Implement `RecurrenceRule.kt` (Section 6.2) and `RecurrenceEngine.kt` (Section 9).
9. Implement `DatabaseModule.kt` and `FirebaseModule.kt` (Section 14).
10. Write a simple test: insert a `BoardEntity` into Room and query it back.

---

### Phase 2 — Auth & Preferences
**Goal**: App authenticates anonymously on first launch; UID is available everywhere.

1. Implement `AuthRepository.kt` (Section 11.1).
2. Implement `UserPreferencesRepository.kt` (Section 20).
3. In `MainActivity.kt`: call `authRepository.ensureAuthenticated()` in a `LaunchedEffect` before showing any UI. Show a loading spinner until auth resolves.
4. Create `MainActivity.kt` with `AppNavigation` scaffold (Section 15.2) — at this point all screens can be placeholder `Text("TODO")` composables.
5. Add `WorkerModule.kt` (Section 14.3).
6. Implement `RepositoryModule.kt` providing all repositories.
7. Test: run the app, confirm anonymous sign-in works, UID printed to logcat.

---

### Phase 3 — Board & Task Data Layer (no sync yet)
**Goal**: Create/read boards, columns, and tasks locally.

1. Implement `BoardRepository.kt` (Section 10.1) — skip `enqueueSyncWork()` for now (stub it as a no-op).
2. Implement `TaskRepository.kt` (Section 10.2) — skip alarm scheduling and sync for now.
3. Implement `SubtaskRepository.kt` (similar to TaskRepository).
4. Write unit tests for fractional indexing in `moveTask`.
5. Implement `BoardListViewModel.kt` and `BoardListScreen.kt`.
6. Implement `KanbanBoardViewModel.kt`.
7. Implement `KanbanColumn.kt` with within-column reorder only (using Reorderable library) — skip cross-column drag for now.
8. Implement `TaskCard.kt` (title + description, no reminder display yet).
9. Implement `TaskDetailScreen.kt` with title and description editing only (no subtasks or reminders yet).
10. Test: create a board, add columns, add tasks, edit tasks, reorder tasks within a column.

---

### Phase 4 — Drag-and-Drop (cross-column)
**Goal**: Tasks can be dragged between columns.

1. Add `Modifier.dragAndDropSource` to `TaskCard.kt`.
2. Add `Modifier.dragAndDropTarget` to `KanbanColumn.kt`.
3. Wire up `onCardDropped` callback through to `KanbanBoardViewModel.moveTask()`.
4. Test: drag a task from one column to another. Confirm order is preserved correctly.

---

### Phase 5 — Subtasks
**Goal**: Checklist items on tasks.

1. Implement `SubtaskRepository.kt` fully.
2. Add subtask section to `TaskDetailScreen.kt` with `SubtaskItem` composable.
3. Implement `TaskDetailViewModel.kt` fully (subtask operations).
4. Test: add subtasks to a task, check/uncheck them.

---

### Phase 6 — AlarmManager & Notifications
**Goal**: Alarms fire and show persistent notifications.

1. Create vector drawable icons: `ic_notification`, `ic_dismiss`, `ic_snooze` (24dp Material icons).
2. Create `AlarmAlertActivity.kt` layout with title text, Dismiss and Snooze buttons.
3. Implement `NotificationHelper.kt` (Section 13.1) — create notification channels in `KanbanApplication.onCreate()`.
4. Implement `AlarmScheduler.kt` (Section 13.2).
5. Implement `ReminderReceiver.kt` (Section 13.3). Register in Manifest.
6. Implement `BootReceiver.kt` (Section 13.4). Register in Manifest.
7. Connect `AlarmScheduler` into `TaskRepository.updateTask()`.
8. Add reminder UI to `TaskDetailScreen.kt`: date/time picker dialog + style toggle.
9. Test: set a task reminder 1 minute in the future. Confirm notification fires. Test dismiss and snooze.
10. Test boot resilience: reboot device; confirm alarm re-registers.

---

### Phase 7 — Reminders Section
**Goal**: Standalone recurring reminders work end-to-end.

1. Implement `ReminderRepository.kt` fully (Section 10.3).
2. Implement `RecurrencePicker.kt` (Section 18.3).
3. Implement `AddEditReminderScreen.kt` and `AddEditReminderViewModel.kt`.
4. Implement `RemindersScreen.kt` and `RemindersViewModel.kt`.
5. Connect `ReminderRepository.onReminderFired()` in `ReminderReceiver.handleFire()`.
6. Test: create a daily recurring reminder, confirm it fires and reschedules.
7. Test: create a "every Mon+Wed at 09:00" reminder, manually advance time to confirm correct next trigger computation via `RecurrenceEngine`.

---

### Phase 8 — Firestore Sync
**Goal**: Data syncs to/from Firestore when online.

1. Implement `FirestoreMappers.kt` with `toFirestoreMap()` and `DocumentSnapshot.toXxxEntity()` for all entity types.
2. Implement `SyncWorker.kt` (Section 12.3) fully.
3. Un-stub `enqueueSyncWork()` in all repositories.
4. Write Firestore Security Rules (Section 12.2) — deploy to Firebase console.
5. Test with Firebase Emulator: create a board offline, go online, confirm it appears in Firestore emulator UI at `localhost:4000`.
6. Test conflict: modify a task on device, modify same task in Firestore emulator UI, sync — confirm last-write-wins with server timestamp.

---

### Phase 9 — Settings & Account Linking
**Goal**: Google Sign-In works; permissions are managed gracefully.

1. Add `implementation("com.google.android.gms:play-services-auth:21.2.0")` to Gradle (Credential Manager API).
2. Implement Google Sign-In flow in `SettingsViewModel.kt` using `CredentialManager`.
3. Implement `SettingsScreen.kt` fully (Section 19).
4. Add `SCHEDULE_EXACT_ALARM` permission banner logic.
5. Add `POST_NOTIFICATIONS` runtime permission request (triggered when user first creates a reminder).
6. Test: link anonymous account to Google, verify same UID is preserved, verify all data still accessible.

---

### Phase 10 — Polish & Edge Cases
1. Handle `FirebaseAuthUserCollisionException` in account linking (show dialog explaining the conflict).
2. Handle Firestore `permission-denied` on listener timeout — implement listener reattachment.
3. Add empty states to all list screens ("No boards yet — tap + to create one").
4. Add error states to ViewModels (wrap with `Result<T>` or a sealed `UiState`).
5. Add `ColumnConfigSheet.kt` for column rename/delete/reorder on the Kanban board screen.
6. Remove `fallbackToDestructiveMigration()` and write a proper Room migration (even if it's just version 1 → 1, set it up for future migrations).
7. Remove `android:usesCleartextTraffic="true"` from Manifest (emulator no longer needed in release).
8. Set up ProGuard rules for Gson, Firebase, and Room.
9. Test on Pixel 6 running Android 15: confirm `SCHEDULE_EXACT_ALARM` flow, confirm full-screen intent degrades gracefully (heads-up notification instead of full-screen popup).

---

## 23. Key Gotchas & Notes

### AlarmManager
- **Always use `PendingIntent.FLAG_IMMUTABLE`** on API 31+. Omitting it causes a crash.
- **`setRepeating()` is inexact since API 19** — never use it. Always use single-fire + reschedule in the receiver.
- **`setExactAndAllowWhileIdle()`** is rate-limited in deep Doze mode (~1 per 9 min). For a personal-use app this is acceptable.
- **Pixel Adaptive Battery** may bucket the app as "Rare" — guide user in Settings to set the app to "Unrestricted" battery in Android Settings → Apps → [App] → Battery.
- **Android 14+ and `USE_FULL_SCREEN_INTENT`**: The full-screen lock-screen takeover is restricted to alarm/clock classified apps. The app falls back to a heads-up notification which is still loud and persistent (ongoing). This is documented in the code with `Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE` checks.

### Room
- Use `ORDER BY \`order\`` (backtick-escape the column name `order` since it's a reserved SQL word).
- `@Insert(onConflict = REPLACE)` is used as the upsert pattern — it deletes + re-inserts, which resets the row. This is fine for this app's use case.
- Export the schema (`exportSchema = true`) and commit the JSON files to version control — this is required for writing proper migrations later.

### Firestore
- **Transactions don't work offline** — never use them for task/board mutations. Use direct `.set()` with `SetOptions.merge()`.
- Firestore snapshot listeners can throw `permission-denied` after extended background time when auth tokens expire. Wrap listeners in try-catch and reconnect on this exception.
- **Subcollection queries** (e.g. `collectionGroup("tasks")`) require a **composite index** in Firestore — create these in the Firebase console when prompted by the error message in logcat during testing.
- **1 MB document limit** — never embed task arrays in board documents. Always use subcollections.

### Temp IDs for offline-created entities
The PDF guide mentions Todoist's pattern of using negative temp IDs. This app uses UUID strings instead, which avoids the issue entirely — UUIDs are stable and never need remapping.

### Fractional indexing
When reordering, compute: `newOrder = (orderBefore + orderAfter) / 2`. After many reorders, the precision of `Double` (64-bit) is sufficient for thousands of reorders without needing to renormalize. If gaps become too small (< 1e-10), run a normalization pass that reassigns integer orders — this is unlikely to be needed in a personal app but should be noted.

### Notification permission on Android 13+
Request `POST_NOTIFICATIONS` at a **contextually appropriate** moment — not on first launch. The right time is when the user first creates a reminder (in `AddEditReminderScreen` after tapping Save, or shown as a pre-prompt before the system dialog).

### Firestore Security Rules deployment
After writing rules locally (Section 12.2), deploy them:
```bash
firebase deploy --only firestore:rules
```
Or paste them directly into the Firebase console under Firestore → Rules.

### Google Sign-In deprecation
`GoogleSignInClient` is deprecated. Use the **Credential Manager API** (`androidx.credentials`). Add dependency:
```
implementation("androidx.credentials:credentials:1.3.0")
implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
```
Follow the [Credential Manager documentation](https://developer.android.com/identity/sign-in/credential-manager) for the full sign-in flow.

---

*End of implementation plan. Hand this document to a Claude Code session along with the original architecture PDF to begin implementation.*
