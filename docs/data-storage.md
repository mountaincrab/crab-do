# Data Storage

Crabban uses an **offline-first** architecture: the Android app reads and writes to a local SQLite database, and a background worker syncs that data to Firebase Cloud Firestore when the device is online.

## The two stores

### 1. Room (local, device-side)

**Room** is Google's ORM/persistence library for Android. It sits on top of SQLite and turns Kotlin `@Entity` classes into database tables, `@Dao` interfaces into typed queries, and exposes reactive `Flow<T>` streams the UI collects. All reads and writes in the UI go through Room — the app is fully usable with no network.

Database: `AppDatabase` (version 3), defined in `data/local/AppDatabase.kt`.

### 2. Firebase Cloud Firestore (remote, cloud-side)

Firestore is a hosted NoSQL document database. It's the system of record across devices and the backing store for any future clients. Firebase Auth provides the `userId` that scopes every document.

## Offline behaviour and sync

The UI never talks to Firestore directly — it only ever reads from and writes to Room. That means the app behaves identically online and offline: every screen loads, every mutation succeeds, and Compose `Flow`s emit immediately because the data is local.

**What happens on a local mutation (e.g. create a task):**
1. Repository writes the row to Room with `syncStatus = PENDING` and `updatedAt = now`.
2. Repository calls `enqueueSyncWork()`, which schedules a `SyncWorker` job via WorkManager with a `NetworkType.CONNECTED` constraint and exponential backoff. `ExistingWorkPolicy.REPLACE` coalesces rapid edits into one run.
3. UI updates instantly from the Room `Flow` — no waiting on network.

**What `SyncWorker` does when it runs** (`data/remote/SyncWorker.kt`):
1. **Push**: queries each DAO for unsynced rows (`syncStatus != 'SYNCED'`), writes each one to its Firestore document path with `SetOptions.merge()`, then marks the row `SYNCED`. Deletes are pushed as `{isDeleted: true}` tombstones rather than removing the document, so other clients can observe the deletion on their next pull.
2. **Pull**: for each top-level collection, queries `where("updatedAt", ">", lastSyncTimestamp)` and upserts the results into Room with `syncStatus = SYNCED`. The last-sync timestamp is persisted in DataStore.
3. On exception, retries up to 3 times (`Result.retry()`), then gives up (`Result.failure()`); the rows stay `PENDING` and will be picked up by the next sync.

**Conflict resolution:** last-writer-wins by `updatedAt`. Pushes use `FieldValue.serverTimestamp()` so Firestore's clock decides the canonical order; pulls compare against the local last-sync timestamp. There is no merge — the most recent write to a given document path replaces the previous one. This is fine for single-user-multi-device but would need rethinking for multi-user collaboration.

**Offline durability:** because every write is committed to SQLite first and only flagged for sync afterwards, the app survives being killed, the device rebooting, or being offline for days. WorkManager persists pending jobs across reboots, so the next time the device has network the queued sync runs automatically.

## Schema

All entities share four housekeeping fields: `id` (UUID string PK), `updatedAt` (millis), `syncStatus` (`PENDING | SYNCED | ERROR`), `isDeleted` (soft-delete flag).

### `boards`
| field | type | notes |
|---|---|---|
| id | String | PK |
| userId | String | Firebase Auth uid |
| title | String | |
| columnOrder | String | JSON array of column ids |
| createdAt | Long | |

### `columns`
| field | type | notes |
|---|---|---|
| id | String | PK |
| boardId | String | FK → boards |
| title | String | |
| order | Double | fractional index for drag-reorder |

### `tasks`
| field | type | notes |
|---|---|---|
| id | String | PK |
| boardId | String | FK → boards |
| columnId | String | FK → columns |
| title | String | |
| description | String | |
| order | Double | fractional index |
| reminderTimeMillis | Long? | optional one-shot reminder |
| reminderStyle | enum | `ALARM` \| `NOTIFICATION` |

### `subtasks`
| field | type | notes |
|---|---|---|
| id | String | PK |
| taskId | String | FK → tasks |
| title | String | |
| isCompleted | Boolean | |
| order | Double | fractional index |

### `reminders` (standalone reminders, not attached to a task)
| field | type | notes |
|---|---|---|
| id | String | PK |
| userId | String | |
| title | String | |
| nextTriggerMillis | Long | |
| reminderStyle | enum | `ALARM` \| `NOTIFICATION` |
| recurrenceRuleJson | String? | serialised `RecurrenceRule` |
| isEnabled | Boolean | |
| snoozedUntilMillis | Long? | |
| createdAt | Long | |

## Firestore layout

Everything is nested under the authenticated user:

```
users/{userId}
  ├── boards/{boardId}          ← board doc
  │     ├── columns/{columnId}  ← column doc
  │     └── tasks/{taskId}      ← task doc
  │           └── subtasks/{subtaskId}
  └── reminders/{reminderId}
```

Field names match the Room columns one-to-one (see `data/remote/FirestoreMappers.kt`), except `updatedAt` is written as a Firestore server `Timestamp` for consistent clock ordering during pull.

## Building a web app against the same data

**Yes.** Firestore is the shared source of truth and has first-class web support.

- Use the Firebase JS SDK (`firebase/firestore`, `firebase/auth`) in your web app.
- Authenticate the same user (same Firebase project) so your `userId` matches.
- Read/write the exact same document paths listed above. Field names and types are documented in `FirestoreMappers.kt`.
- You get real-time updates for free via `onSnapshot` — no sync worker needed client-side; Firestore handles it.
- You'll want to mirror the soft-delete convention (`isDeleted = true`) rather than hard-deleting, so the Android app's pull sync still sees the tombstone.

Caveats:
- Only data that the Android app has actually synced will be visible — anything still `PENDING` on a phone that's offline won't be in Firestore yet.
- Firestore security rules must allow `users/{uid}/**` reads/writes where `request.auth.uid == uid`. Check the rules in the Firebase console before going live.
- There's no server-side schema enforcement — the web app and Android app must agree on field names and enum values by convention.
