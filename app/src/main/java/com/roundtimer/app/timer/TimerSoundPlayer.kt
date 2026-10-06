package com.roundtimer.app.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.roundtimer.app.R

/** Reproduz campainha/beeps e controla vibração. */
class TimerSoundPlayer(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val bellId = soundPool.load(context, R.raw.bell, 1)
    private val beepId = soundPool.load(context, R.raw.beep, 1)

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    var soundEnabled = true
    var vibrationEnabled = true

    fun playBell() {
        if (soundEnabled) soundPool.play(bellId, 1f, 1f, 1, 0, 1f)
        vibrate(VIBRATION_BELL)
    }

    fun playBeep() {
        if (soundEnabled) soundPool.play(beepId, 1f, 1f, 1, 0, 1f)
        vibrate(VIBRATION_BEEP)
    }

    private fun vibrate(pattern: LongArray) {
        if (!vibrationEnabled || !vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    fun release() {
        soundPool.release()
    }

    private companion object {
        val VIBRATION_BELL = longArrayOf(0, 250, 100, 250)
        val VIBRATION_BEEP = longArrayOf(0, 80)
    }
}
