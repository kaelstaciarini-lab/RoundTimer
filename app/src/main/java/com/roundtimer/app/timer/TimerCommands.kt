package com.roundtimer.app.timer

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Atalhos para a UI enviar comandos ao [TimerService]. */
object TimerCommands {

    fun start(context: Context, config: TimerConfig) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_ROUNDS, config.rounds)
            putExtra(TimerService.EXTRA_ROUND_SEC, config.roundDurationSec)
            putExtra(TimerService.EXTRA_REST_SEC, config.restDurationSec)
            putExtra(TimerService.EXTRA_PREPARE_SEC, config.prepareDurationSec)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun pause(context: Context) = send(context, TimerService.ACTION_PAUSE)

    fun resume(context: Context) = send(context, TimerService.ACTION_RESUME)

    fun skip(context: Context) = send(context, TimerService.ACTION_SKIP)

    fun stop(context: Context) = send(context, TimerService.ACTION_STOP)

    private fun send(context: Context, action: String) {
        context.startService(Intent(context, TimerService::class.java).setAction(action))
    }
}
