package com.roundtimer.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.roundtimer.app.timer.TimerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class UserSettings(
    val lastConfig: TimerConfig = TimerConfig(),
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ROUNDS = intPreferencesKey("last_rounds")
        val ROUND_SEC = intPreferencesKey("last_round_sec")
        val REST_SEC = intPreferencesKey("last_rest_sec")
        val PREPARE_SEC = intPreferencesKey("last_prepare_sec")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            lastConfig = TimerConfig(
                rounds = prefs[Keys.ROUNDS] ?: 3,
                roundDurationSec = prefs[Keys.ROUND_SEC] ?: 180,
                restDurationSec = prefs[Keys.REST_SEC] ?: 60,
                prepareDurationSec = prefs[Keys.PREPARE_SEC] ?: 10,
            ),
            soundEnabled = prefs[Keys.SOUND] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION] ?: true,
        )
    }

    suspend fun saveLastConfig(config: TimerConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ROUNDS] = config.rounds
            prefs[Keys.ROUND_SEC] = config.roundDurationSec
            prefs[Keys.REST_SEC] = config.restDurationSec
            prefs[Keys.PREPARE_SEC] = config.prepareDurationSec
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION] = enabled }
    }
}
