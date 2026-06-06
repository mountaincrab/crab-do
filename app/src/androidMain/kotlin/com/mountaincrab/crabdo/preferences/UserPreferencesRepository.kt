package com.mountaincrab.crabdo.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val PINNED_BOARD_ID = stringPreferencesKey("pinned_board_id")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val TIME_INPUT_KEYBOARD = booleanPreferencesKey("time_input_keyboard")
        val APP_THEME = stringPreferencesKey("app_theme")
        val AUTO_DISMISS_MINUTES = intPreferencesKey("auto_dismiss_minutes")
    }

    /** Minutes an unattended ALARM keeps ringing before it is auto-dismissed. 0 = never. */
    val autoDismissMinutes: Flow<Int> =
        context.dataStore.data.map { it[Keys.AUTO_DISMISS_MINUTES] ?: DEFAULT_AUTO_DISMISS_MINUTES }

    suspend fun getAutoDismissMinutes(): Int =
        context.dataStore.data.first()[Keys.AUTO_DISMISS_MINUTES] ?: DEFAULT_AUTO_DISMISS_MINUTES

    suspend fun setAutoDismissMinutes(value: Int) {
        context.dataStore.edit { it[Keys.AUTO_DISMISS_MINUTES] = value }
    }

    val pinnedBoardId: Flow<String?> = context.dataStore.data.map { it[Keys.PINNED_BOARD_ID] }

    val timeInputKeyboard: Flow<Boolean> = context.dataStore.data.map { it[Keys.TIME_INPUT_KEYBOARD] ?: false }

    val appTheme: Flow<String?> = context.dataStore.data.map { it[Keys.APP_THEME] }

    suspend fun setAppTheme(themeName: String) {
        context.dataStore.edit { it[Keys.APP_THEME] = themeName }
    }

    suspend fun setTimeInputKeyboard(value: Boolean) {
        context.dataStore.edit { it[Keys.TIME_INPUT_KEYBOARD] = value }
    }

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

    suspend fun clearSyncState() {
        context.dataStore.edit { it[Keys.LAST_SYNC_TIMESTAMP] = 0L }
    }

    companion object {
        const val DEFAULT_AUTO_DISMISS_MINUTES = 5
    }
}
