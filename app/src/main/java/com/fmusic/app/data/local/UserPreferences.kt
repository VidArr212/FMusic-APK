package com.fmusic.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fmusic_settings")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
        val KEY_SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
        val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val KEY_NORMALIZE_VOLUME = booleanPreferencesKey("normalize_volume")
        val KEY_SKIP_NON_MUSIC = booleanPreferencesKey("skip_non_music")
    }

    val apiBaseUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_API_BASE_URL] ?: "http://10.0.2.2:3000/"
    }

    val sleepTimerMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_SLEEP_TIMER_MINUTES] ?: 0
    }

    val skipNonMusic: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SKIP_NON_MUSIC] ?: true
    }

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_API_BASE_URL] = url
        }
    }

    suspend fun setSleepTimerMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SLEEP_TIMER_MINUTES] = minutes
        }
    }

    suspend fun setSkipNonMusic(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SKIP_NON_MUSIC] = enable
        }
    }
}
