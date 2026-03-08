package com.appcontrolx.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.remove
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.appcontrolx.model.ActionHistoryItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.actionHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "action_history")

@Singleton
class ActionHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ACTION_HISTORY_JSON = stringPreferencesKey("action_history_json")
        private const val MAX_HISTORY_ITEMS = 200
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val safeDataStoreFlow: Flow<Preferences> = context.actionHistoryDataStore.data
        .catch { emit(emptyPreferences()) }

    val actionHistory: Flow<List<ActionHistoryItem>> = safeDataStoreFlow.map { preferences ->
        decode(preferences[ACTION_HISTORY_JSON]).take(MAX_HISTORY_ITEMS)
    }

    suspend fun addAction(item: ActionHistoryItem) {
        context.actionHistoryDataStore.edit { preferences ->
            val current = decode(preferences[ACTION_HISTORY_JSON])
            val updated = (listOf(item) + current).take(MAX_HISTORY_ITEMS)
            preferences[ACTION_HISTORY_JSON] = encode(updated)
        }
    }

    suspend fun clearHistory() {
        context.actionHistoryDataStore.edit { preferences ->
            preferences.remove(ACTION_HISTORY_JSON)
        }
    }

    private fun decode(raw: String?): List<ActionHistoryItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(ActionHistoryItem.serializer()), raw)
        }.getOrElse {
            emptyList()
        }
    }

    private fun encode(items: List<ActionHistoryItem>): String {
        return runCatching {
            json.encodeToString(ListSerializer(ActionHistoryItem.serializer()), items)
        }.getOrDefault("[]")
    }
}
