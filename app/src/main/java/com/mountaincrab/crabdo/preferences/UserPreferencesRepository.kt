package com.mountaincrab.crabdo.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PINNED_BOARD_ID = stringPreferencesKey("pinned_board_id")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    val pinnedBoardId: Flow<String?> = context.dataStore.data.map { it[Keys.PINNED_BOARD_ID] }

    suspend fun setPinnedBoardId(boardId: String?) {
        context.dataStore.edit { prefs ->
            if (boardId == null) prefs.remove(Keys.PINNED_BOARD_ID)
            else prefs[Keys.PINNED_BOARD_ID] = boardId
        }
    }

    suspend fun getLastSyncTimestamp(): Long =
        context.dataStore.data.first()[Keys.LAST_SYNC_TIMESTAMP] ?: 0L

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_TIMESTAMP] = timestamp }
    }
}
