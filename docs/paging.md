# Board and reminder paging

Both clients initially show 50 tasks per column. Each column has its own Load more control. Completed one-off reminders and deleted one-off/recurring reminders are queried only when their history section is expanded, in increments of 50. Active reminders remain fully observed so alarms continue to work.

## Live queries and ordering

The clients grow a single live prefix instead of attaching independent cursor pages. Each query requests one extra row to detect whether another page exists. This keeps inserts, moves, deletes, restores, and changed sort values consistent across page boundaries. Loading another page resubscribes to the enlarged prefix, so it can reread already loaded documents; reads are bounded by the requested window, not strictly 50 new reads per click. Every paged sort includes document ID as a tie-breaker.

- Tasks: `order`, then ID, ascending.
- Completed reminders: `completedAt`, then ID, descending. Unknown completion times sort last.
- Deleted reminders: `updatedAt`, then ID, descending.

Web badges show the number loaded, with `+` when more exist. Android board totals come from Room count queries; reminder history headers show loaded counts. Subtask listeners attach only to tasks in the loaded board windows. Card editors observe the task independently, so moving it outside a window does not close its editor.

A drag at the end of a partially loaded column uses the extra row's order as its next neighbor. Creating a task or moving through the card menu queries the real minimum order, so tasks go to the top even when the column is only partially loaded. Large web column/board deletions traverse all matching tasks in batches of 400 writes, including unloaded tasks. Interrupted deletions can be retried.

## Android offline behavior

Room queries use SQL LIMIT, rather than slicing full lists in the UI. Android retains its offline replica for alarms, widgets, sharing, and offline editing. Sync, board refresh, and shared-board imports fetch remote documents in cursor batches of 200. This bounds individual reads and materialized responses; it does **not** reduce the total initial reads or local storage needed for a complete offline sync. The foreground board listener is limited per column; unseen tasks receive remote updates through background sync.

A Firestore REMOVED event in a limited query is not treated as deletion. The repository re-reads that document to distinguish a moved/evicted row from a deletion, and preserves locally pending writes. Background sync records its start time as the next watermark so edits during a long paged pull are eligible on the next sync.

## Rollout

Deploy the indexes in `firestore.indexes.json` and wait for them to finish building **before releasing either client**. The normal release workflow may deploy surfaces concurrently, so deploy the indexes ahead of that release.

Older reminder documents can lack `isCompleted`, `completedAt`, or `updatedAt`. Firestore filters and ordering exclude documents missing those fields. The migration script fills only missing values and rechecks each document transactionally, preserving concurrent edits:

```sh
cd functions
npm ci
# With the intended project's Firebase Admin credentials configured:
node scripts/backfill-reminder-paging.cjs          # dry run
node scripts/backfill-reminder-paging.cjs --apply  # apply if needed
```

Run the dry run again to confirm no documents need normalization. This migration and index deployment were not run against production as part of the implementation.

## Verification

```sh
cd webapp
npm test
npm run build
```

From the repository root:

```sh
python3 -m unittest discover -s tests -p 'test_paging_queries.py'
node --test functions/scripts/backfill-reminder-paging.test.cjs
```

The host-SQLite tests execute the actual DAO queries against the exported Room v9 schema. They cover multiple pages, equal sort values, user/column isolation, null completion times, deletions, restores, and moves across a page boundary. They do not verify Room code generation or Compose compilation.

Android compilation/device testing remains required on a compatible Android build host. The implementation environment is ARM64 and its installed `aapt2` is x86-64, so no Android build or instrumented tests were run here.
