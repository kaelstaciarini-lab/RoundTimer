package com.roundtimer.app.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ponte entre o [TimerService] (que escreve) e a UI (que lê).
 * Singleton pois existe apenas um timer ativo por vez no app.
 */
object TimerStateHolder {
    private val _state = MutableStateFlow(TimerSnapshot())
    val state: StateFlow<TimerSnapshot> = _state.asStateFlow()

    fun update(snapshot: TimerSnapshot) {
        _state.value = snapshot
    }

    fun reset() {
        _state.value = TimerSnapshot()
    }
}
