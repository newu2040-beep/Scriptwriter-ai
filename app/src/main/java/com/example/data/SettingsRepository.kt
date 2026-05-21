package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val preferredApiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[API_KEY]
    }
    
    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE]
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(API_KEY)
        }
    }
    
    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = isDark
        }
    }
}
