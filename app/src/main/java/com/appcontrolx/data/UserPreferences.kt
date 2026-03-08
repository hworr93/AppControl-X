package com.appcontrolx.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
    }

    private val safeDataStoreFlow: Flow<Preferences> = context.dataStore.data
        .catch { emit(emptyPreferences()) }

    val isFirstLaunch: Flow<Boolean> = safeDataStoreFlow.map { preferences ->
        preferences[IS_FIRST_LAUNCH] ?: true
    }

    val themeMode: Flow<ThemeMode> = safeDataStoreFlow.map { preferences ->
        when (preferences[THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val isSetupCompleted: Flow<Boolean> = safeDataStoreFlow.map { preferences ->
        preferences[SETUP_COMPLETED] ?: false
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = isFirst
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SETUP_COMPLETED] = completed
        }
    }

    suspend fun resetSetup() {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = true
            preferences[SETUP_COMPLETED] = false
        }
    }
}
