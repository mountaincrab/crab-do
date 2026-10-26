import { DocumentData, Query, getDocsFromServer, limit, query, serverTimestamp, writeBatch } from 'firebase/firestore'
import { db } from '../firebase'

/** Source must filter isDeleted == false. Each committed batch removes itself
 * from that query, so retrying after an interrupted deletion is safe. */
export async function softDeletePages(source: Query<DocumentData>, extraFields: Record<string, unknown> = {}) {
  while (true) {
    const page = await getDocsFromServer(query(source, limit(400)))
    if (page.empty) return
    const batch = writeBatch(db)
    page.docs.forEach((item) => batch.update(item.ref, {
      ...extraFields, isDeleted: true, updatedAt: serverTimestamp(),
    }))
    await batch.commit()
  }
}

export const clearedTaskReminder = {
  reminderTimeMillis: null, snoozedUntilMillis: null, reminderTransitionColumnId: null,
}
