package com.roundtimer.app.timer

/**
 * Motor do timer: máquina de estados pura, sem dependências do Android.
 * Tempo baseado em relógio monotônico fornecido externamente (testável).
 *
 * Sequência: PREPARE (opcional) → ROUND 1 → REST → ... → ROUND N → FINISHED
 * (o descanso só existe entre rounds, nunca após o último).
 */
class TimerEngine(
    private val eventSink: (TimerEvent) -> Unit = {},
) {
    private data class Segment(val phase: Phase, val roundNumber: Int, val durationMs: Long)

    private var segments: List<Segment> = emptyList()
    private var index = 0
    private var segmentEndMs = 0L
    private var remainingWhenPausedMs = 0L
    private var paused = false
    private var running = false
    private var lastWholeSecond = Long.MAX_VALUE
    private var totalRounds = 0

    var snapshot = TimerSnapshot()
        private set

    fun start(config: TimerConfig, nowMs: Long) {
        segments = buildSegments(config)
        totalRounds = config.rounds
        index = 0
        running = true
        paused = false
        segmentEndMs = nowMs + segments[0].durationMs
        lastWholeSecond = Long.MAX_VALUE
        updateSnapshot(segments[0].durationMs)
    }

    fun tick(nowMs: Long) {
        if (!running || paused) return
        while (running && segmentEndMs - nowMs <= 0) {
            advance()
        }
        if (!running) return
        val remaining = segmentEndMs - nowMs
        val wholeSecond = (remaining + 999L) / 1000L
        if (wholeSecond != lastWholeSecond) {
            lastWholeSecond = wholeSecond
            if (wholeSecond == 10L || wholeSecond in 1..3) {
                eventSink(TimerEvent.Warning(wholeSecond.toInt()))
            }
        }
        updateSnapshot(remaining)
    }

    fun pause(nowMs: Long) {
        if (!running || paused) return
        remainingWhenPausedMs = segmentEndMs - nowMs
        paused = true
        updateSnapshot(remainingWhenPausedMs)
    }

    fun resume(nowMs: Long) {
        if (!running || !paused) return
        segmentEndMs = nowMs + remainingWhenPausedMs
        paused = false
        lastWholeSecond = Long.MAX_VALUE
        updateSnapshot(remainingWhenPausedMs)
    }

    fun skip(nowMs: Long) {
        if (!running) return
        paused = false
        advance()
        if (running) {
            // Re-ancora no instante atual: a fase pulada começa com duração cheia.
            segmentEndMs = nowMs + segments[index].durationMs
            updateSnapshot(segments[index].durationMs)
        }
    }

    /** Encerra e volta para IDLE. */
    fun stop() {
        running = false
        paused = false
        snapshot = TimerSnapshot()
    }

    private fun buildSegments(config: TimerConfig): List<Segment> {
        val list = mutableListOf<Segment>()
        if (config.prepareDurationSec > 0) {
            list += Segment(Phase.PREPARE, 0, config.prepareDurationSec * 1000L)
        }
        for (r in 1..config.rounds) {
            list += Segment(Phase.ROUND, r, config.roundDurationSec * 1000L)
            if (r < config.rounds && config.restDurationSec > 0) {
                list += Segment(Phase.REST, r, config.restDurationSec * 1000L)
            }
        }
        return list
    }

    private fun advance() {
        eventSink(TimerEvent.Bell)
        index++
        if (index >= segments.size) {
            running = false
            paused = false
            snapshot = snapshot.copy(
                phase = Phase.FINISHED,
                remainingMs = 0L,
                phaseDurationMs = 0L,
                paused = false,
                running = false,
            )
            return
        }
        // Agenda ancorada no fim anterior para não acumular drift.
        segmentEndMs += segments[index].durationMs
        lastWholeSecond = Long.MAX_VALUE
    }

    private fun updateSnapshot(remainingMs: Long) {
        val seg = segments[index]
        snapshot = TimerSnapshot(
            phase = seg.phase,
            roundNumber = seg.roundNumber,
            totalRounds = totalRounds,
            remainingMs = remainingMs.coerceAtLeast(0L),
            phaseDurationMs = seg.durationMs,
            paused = paused,
            running = running,
        )
    }
}
