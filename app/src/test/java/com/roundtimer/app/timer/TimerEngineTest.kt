package com.roundtimer.app.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {

    private fun engine(events: MutableList<TimerEvent> = mutableListOf()): Pair<TimerEngine, MutableList<TimerEvent>> {
        val list = events
        return TimerEngine { list += it } to list
    }

    @Test
    fun `sequencia completa prepare round rest round fim`() {
        val (engine, _) = engine()
        val config = TimerConfig(rounds = 2, roundDurationSec = 10, restDurationSec = 5, prepareDurationSec = 3)
        engine.start(config, nowMs = 0)

        assertEquals(Phase.PREPARE, engine.snapshot.phase)
        assertEquals(3000, engine.snapshot.remainingMs)

        engine.tick(3000)
        assertEquals(Phase.ROUND, engine.snapshot.phase)
        assertEquals(1, engine.snapshot.roundNumber)

        engine.tick(13000)
        assertEquals(Phase.REST, engine.snapshot.phase)

        engine.tick(18000)
        assertEquals(Phase.ROUND, engine.snapshot.phase)
        assertEquals(2, engine.snapshot.roundNumber)

        engine.tick(28000)
        assertEquals(Phase.FINISHED, engine.snapshot.phase)
        assertFalse(engine.snapshot.running)
    }

    @Test
    fun `sem preparacao comeca direto no round`() {
        val (engine, _) = engine()
        engine.start(TimerConfig(rounds = 1, roundDurationSec = 10, restDurationSec = 5, prepareDurationSec = 0), 0)
        assertEquals(Phase.ROUND, engine.snapshot.phase)
        assertEquals(1, engine.snapshot.roundNumber)
    }

    @Test
    fun `sem descanso nao ha fase rest`() {
        val (engine, events) = engine()
        engine.start(TimerConfig(rounds = 2, roundDurationSec = 10, restDurationSec = 0, prepareDurationSec = 0), 0)
        engine.tick(10000)
        assertEquals(Phase.ROUND, engine.snapshot.phase)
        assertEquals(2, engine.snapshot.roundNumber)
        assertEquals(1, events.count { it == TimerEvent.Bell })
    }

    @Test
    fun `countdown decresce corretamente`() {
        val (engine, _) = engine()
        engine.start(TimerConfig(rounds = 1, roundDurationSec = 10, restDurationSec = 0, prepareDurationSec = 0), 0)
        engine.tick(2500)
        assertEquals(7500, engine.snapshot.remainingMs)
        engine.tick(5000)
        assertEquals(5000, engine.snapshot.remainingMs)
    }

    @Test
    fun `pause congela e resume continua`() {
        val (engine, _) = engine()
        engine.start(TimerConfig(rounds = 1, roundDurationSec = 10, restDurationSec = 0, prepareDurationSec = 0), 0)
        engine.tick(2000) // restam 8s
        engine.pause(2000)
        assertTrue(engine.snapshot.paused)
        assertEquals(8000, engine.snapshot.remainingMs)

        engine.tick(5000) // pausado: não muda
        assertEquals(8000, engine.snapshot.remainingMs)

        engine.resume(5000)
        engine.tick(8000) // passaram 3s após retomar
        assertEquals(5000, engine.snapshot.remainingMs)
        assertFalse(engine.snapshot.paused)
    }

    @Test
    fun `skip pula para proxima fase`() {
        val (engine, events) = engine()
        engine.start(TimerConfig(rounds = 2, roundDurationSec = 10, restDurationSec = 5, prepareDurationSec = 0), 0)
        engine.skip(1000)
        assertEquals(Phase.REST, engine.snapshot.phase)
        assertEquals(5000, engine.snapshot.remainingMs)
        assertTrue(events.contains(TimerEvent.Bell))

        engine.skip(1000)
        assertEquals(Phase.ROUND, engine.snapshot.phase)
        assertEquals(2, engine.snapshot.roundNumber)
        assertEquals(10000, engine.snapshot.remainingMs)
    }

    @Test
    fun `skip no ultimo round finaliza`() {
        val (engine, _) = engine()
        engine.start(TimerConfig(rounds = 1, roundDurationSec = 10, restDurationSec = 5, prepareDurationSec = 0), 0)
        engine.skip(1000)
        assertEquals(Phase.FINISHED, engine.snapshot.phase)
        assertFalse(engine.snapshot.running)
    }

    @Test
    fun `eventos de aviso em 10s e 3-2-1`() {
        val (engine, events) = engine()
        engine.start(TimerConfig(rounds = 1, roundDurationSec = 12, restDurationSec = 0, prepareDurationSec = 0), 0)
        for (t in 0..12000 step 50) engine.tick(t.toLong())

        val warnings = events.filterIsInstance<TimerEvent.Warning>().map { it.secondsLeft }
        assertEquals(listOf(10, 3, 2, 1), warnings)
        assertTrue(events.contains(TimerEvent.Bell))
    }

    @Test
    fun `campainha toca em cada transicao e no fim`() {
        val (engine, events) = engine()
        engine.start(TimerConfig(rounds = 2, roundDurationSec = 10, restDurationSec = 5, prepareDurationSec = 0), 0)
        for (t in 0..30000 step 100) engine.tick(t.toLong())
        // round 1 -> rest, rest -> round 2, round 2 -> finished = 3 sinos
        assertEquals(3, events.count { it == TimerEvent.Bell })
        assertEquals(Phase.FINISHED, engine.snapshot.phase)
    }

    @Test
    fun `stop volta para idle`() {
        val (engine, _) = engine()
        engine.start(TimerConfig(rounds = 3, roundDurationSec = 10, restDurationSec = 5, prepareDurationSec = 3), 0)
        engine.tick(1500)
        engine.stop()
        assertEquals(Phase.IDLE, engine.snapshot.phase)
        assertFalse(engine.snapshot.running)
    }

    @Test
    fun `progresso evolui de 0 a 1 durante a fase`() {
        val (engine, _) = engine()
        engine.start(TimerConfig(rounds = 1, roundDurationSec = 10, restDurationSec = 0, prepareDurationSec = 0), 0)
        assertEquals(0f, engine.snapshot.progress, 0.001f)
        engine.tick(5000)
        assertEquals(0.5f, engine.snapshot.progress, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `config invalida lanca excecao`() {
        TimerConfig(rounds = 0)
    }
}
