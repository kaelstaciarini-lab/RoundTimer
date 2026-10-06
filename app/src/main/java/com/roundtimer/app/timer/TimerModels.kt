package com.roundtimer.app.timer

/** Fases do timer durante um treino. */
enum class Phase {
    IDLE,
    PREPARE,
    ROUND,
    REST,
    FINISHED
}

/** Configuração de um treino. */
data class TimerConfig(
    val rounds: Int = 3,
    val roundDurationSec: Int = 180,
    val restDurationSec: Int = 60,
    val prepareDurationSec: Int = 10,
) {
    init {
        require(rounds in 1..99) { "rounds deve estar entre 1 e 99" }
        require(roundDurationSec in 5..3600) { "duração do round inválida" }
        require(restDurationSec in 0..1800) { "duração do descanso inválida" }
        require(prepareDurationSec in 0..300) { "preparação inválida" }
    }
}

/** Eventos disparados pelo motor (para sons e vibração). */
sealed interface TimerEvent {
    /** Campainha: início/fim de cada fase. */
    data object Bell : TimerEvent

    /** Aviso nos últimos segundos da fase (10s e 3-2-1). */
    data class Warning(val secondsLeft: Int) : TimerEvent
}

/** Estado imutável exposto para a UI. */
data class TimerSnapshot(
    val phase: Phase = Phase.IDLE,
    val roundNumber: Int = 0,
    val totalRounds: Int = 0,
    val remainingMs: Long = 0L,
    val phaseDurationMs: Long = 0L,
    val paused: Boolean = false,
    val running: Boolean = false,
) {
    /** Progresso da fase atual: 0f no início, 1f no fim. */
    val progress: Float
        get() = if (phaseDurationMs <= 0) 0f
        else (1f - remainingMs.toFloat() / phaseDurationMs).coerceIn(0f, 1f)
}
