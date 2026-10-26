package com.mountaincrab.crabdo.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/** Keep the offline replica complete without materializing an entire remote
 * collection in one response. Snapshot cursors include the document-ID tie-break.
 * A partial cache must never advance the sync watermark as if it were complete. */
suspend fun Query.forEachDocumentPage(consume: suspend (DocumentSnapshot) -> Unit) {
    var cursor: DocumentSnapshot? = null
    while (true) {
        val request = cursor?.let { startAfter(it) } ?: this
        val documents = request.limit(200).get(Source.SERVER).await().documents
        for (document in documents) consume(document)
        if (documents.size < 200) return
        cursor = documents.last()
    }
}
