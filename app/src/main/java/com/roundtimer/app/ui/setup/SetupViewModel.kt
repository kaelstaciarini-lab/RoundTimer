package com.roundtimer.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.roundtimer.app.data.db.Preset
import com.roundtimer.app.data.db.PresetDao
import com.roundtimer.app.data.settings.SettingsRepository
import com.roundtimer.app.timer.TimerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SetupViewModel(
    private val settings: SettingsRepository,
    private val presetDao: PresetDao,
) : ViewModel() {

    private val _config = MutableStateFlow(TimerConfig())
    val config: StateFlow<TimerConfig> = _config.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    /** Recarrega a última configuração salva (chamado ao entrar na tela). */
    fun reload() {
        viewModelScope.launch {
            val s = settings.settings.first()
            _config.value = s.lastConfig
            _soundEnabled.value = s.soundEnabled
            _vibrationEnabled.value = s.vibrationEnabled
        }
    }

    fun updateConfig(transform: (TimerConfig) -> TimerConfig) {
        _config.update(transform)
    }

    fun setSound(enabled: Boolean) {
        _soundEnabled.value = enabled
        viewModelScope.launch { settings.setSoundEnabled(enabled) }
    }

    fun setVibration(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        viewModelScope.launch { settings.setVibrationEnabled(enabled) }
    }

    /** Persiste a config atual como "última usada" (chamado ao iniciar). */
    fun persistConfig() {
        viewModelScope.launch { settings.saveLastConfig(_config.value) }
    }

    fun savePreset(name: String) {
        viewModelScope.launch {
            presetDao.insert(Preset.from(name.trim(), _config.value))
        }
    }

    class Factory(
        private val settings: SettingsRepository,
        private val presetDao: PresetDao,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SetupViewModel(settings, presetDao) as T
    }
}
