package com.mountaincrab.crabdo.data.model

enum class SyncStatus {
    SYNCED,    // In sync with Firestore
    PENDING,   // Modified locally, not yet pushed
    SYNCING,   // Currently being pushed
    FAILED     // Last sync attempt failed
}
