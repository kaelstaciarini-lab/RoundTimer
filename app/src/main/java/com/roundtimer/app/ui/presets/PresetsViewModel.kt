package com.roundtimer.app.ui.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.roundtimer.app.data.db.Preset
import com.roundtimer.app.data.db.PresetDao
import com.roundtimer.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PresetsViewModel(
    private val presetDao: PresetDao,
    private val settings: SettingsRepository,
) : ViewModel() {

    val presets: StateFlow<List<Preset>> = presetDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(preset: Preset) {
        viewModelScope.launch { presetDao.delete(preset) }
    }

    /** Torna o preset a configuração atual (carregada pela tela de setup). */
    fun select(preset: Preset) {
        viewModelScope.launch { settings.saveLastConfig(preset.toConfig()) }
    }

    class Factory(
        private val presetDao: PresetDao,
        private val settings: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PresetsViewModel(presetDao, settings) as T
    }
}
