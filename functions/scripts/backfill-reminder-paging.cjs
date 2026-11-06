/* Normalize legacy documents before enabling the paging queries.
 * Dry run: node scripts/backfill-reminder-paging.cjs
 * Apply:   node scripts/backfill-reminder-paging.cjs --apply
 * Uses the configured Firebase Admin credentials; never logs document contents.
 */
function missingFields(data, oneOff) {
  const patch = {}
  if (data.isDeleted === undefined) patch.isDeleted = false
  if (oneOff && data.isCompleted === undefined) patch.isCompleted = false
  // Null sorts at the end of descending history and preserves an unknown time.
  if (oneOff && data.completedAt === undefined) patch.completedAt = null
  if (data.updatedAt === undefined) patch.updatedAt = new Date(data.createdAt ?? data.scheduledAt ?? 0)
  return patch
}

async function main() {
  const { initializeApp } = require('firebase-admin/app')
  const { getFirestore, FieldPath } = require('firebase-admin/firestore')
  initializeApp()
  const db = getFirestore()
  const apply = process.argv.includes('--apply')
  let scanned = 0, affected = 0
  for (const name of ['reminders', 'recurringReminders']) {
    const base = db.collectionGroup(name).orderBy(FieldPath.documentId()).limit(200)
    let cursor
    while (true) {
      const page = await (cursor ? base.startAfter(cursor) : base).get()
      for (const doc of page.docs) {
        scanned++
        if (Object.keys(missingFields(doc.data(), name === 'reminders')).length === 0) continue
        affected++
        if (apply) await db.runTransaction(async (transaction) => {
          const current = await transaction.get(doc.ref)
          if (!current.exists) return
          const patch = missingFields(current.data(), name === 'reminders')
          if (Object.keys(patch).length) transaction.update(doc.ref, patch)
        })
      }
      if (page.size < 200) break
      cursor = page.docs[page.docs.length - 1]
    }
  }
  console.log(`${apply ? 'Updated' : 'Would update'} ${affected} of ${scanned} reminders.`)
}

module.exports = { missingFields }
if (require.main === module) main().catch((error) => { console.error(error.message); process.exitCode = 1 })
