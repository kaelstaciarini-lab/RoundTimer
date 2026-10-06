package com.roundtimer.app.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.roundtimer.app.MainActivity
import com.roundtimer.app.R
import com.roundtimer.app.RoundTimerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Serviço em foreground que mantém o timer rodando mesmo com a tela apagada,
 * com notificação persistente (tempo restante + ações de controle).
 */
class TimerService : Service() {

    companion object {
        const val ACTION_START = "com.roundtimer.app.action.START"
        const val ACTION_PAUSE = "com.roundtimer.app.action.PAUSE"
        const val ACTION_RESUME = "com.roundtimer.app.action.RESUME"
        const val ACTION_SKIP = "com.roundtimer.app.action.SKIP"
        const val ACTION_STOP = "com.roundtimer.app.action.STOP"
        const val ACTION_OPEN_TIMER = "com.roundtimer.app.action.OPEN_TIMER"

        const val EXTRA_ROUNDS = "extra_rounds"
        const val EXTRA_ROUND_SEC = "extra_round_sec"
        const val EXTRA_REST_SEC = "extra_rest_sec"
        const val EXTRA_PREPARE_SEC = "extra_prepare_sec"

        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1
        private const val TICK_MS = 100L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var engine: TimerEngine? = null
    private var tickJob: Job? = null
    private var finishJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastNotifiedSecond = Long.MIN_VALUE
    private var lastNotifiedPaused = false
    private var lastPublishedSecond = Long.MIN_VALUE
    private var lastPublishedPhase: Phase? = null
    private var lastPublishedRound = Int.MIN_VALUE
    private var lastPublishedPaused = false

    private lateinit var soundPlayer: TimerSoundPlayer
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        soundPlayer = TimerSoundPlayer(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        observeSettings()
    }

    private fun observeSettings() {
        val settings = (application as RoundTimerApp).container.settings
        scope.launch {
            settings.settings.collect {
                soundPlayer.soundEnabled = it.soundEnabled
                soundPlayer.vibrationEnabled = it.vibrationEnabled
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer(intent)
            ACTION_PAUSE -> engine?.let {
                it.pause(SystemClock.elapsedRealtime())
                TimerStateHolder.update(it.snapshot)
                updateNotification(it.snapshot, force = true)
            }
            ACTION_RESUME -> engine?.let {
                it.resume(SystemClock.elapsedRealtime())
                TimerStateHolder.update(it.snapshot)
                updateNotification(it.snapshot, force = true)
            }
            ACTION_SKIP -> engine?.skip(SystemClock.elapsedRealtime())
            ACTION_STOP -> shutdown()
            else -> shutdown()
        }
        return START_NOT_STICKY
    }

    private fun startTimer(intent: Intent) {
        val config = TimerConfig(
            rounds = intent.getIntExtra(EXTRA_ROUNDS, 3),
            roundDurationSec = intent.getIntExtra(EXTRA_ROUND_SEC, 180),
            restDurationSec = intent.getIntExtra(EXTRA_REST_SEC, 60),
            prepareDurationSec = intent.getIntExtra(EXTRA_PREPARE_SEC, 10),
        )

        finishJob?.cancel()
        engine = TimerEngine(eventSink = ::handleEvent).also {
            it.start(config, SystemClock.elapsedRealtime())
        }
        TimerStateHolder.update(engine!!.snapshot)
        resetPublishedState(engine!!.snapshot)

        startForegroundWithNotification(engine!!.snapshot)
        acquireWakeLock()

        tickJob?.cancel()
        tickJob = scope.launch { tickLoop() }
    }

    private suspend fun tickLoop() {
        while (currentCoroutineContextIsActive()) {
            val e = engine ?: break
            e.tick(SystemClock.elapsedRealtime())
            val snap = e.snapshot
            publishStateIfChanged(snap)
            updateNotification(snap)
            if (snap.phase == Phase.FINISHED) {
                onFinished()
                break
            }
            delay(TICK_MS)
        }
    }

    private fun currentCoroutineContextIsActive(): Boolean =
        tickJob?.isActive ?: false

    private fun publishStateIfChanged(snapshot: TimerSnapshot) {
        val wholeSecond = snapshot.remainingMs / 1000
        if (
            wholeSecond != lastPublishedSecond ||
            snapshot.phase != lastPublishedPhase ||
            snapshot.roundNumber != lastPublishedRound ||
            snapshot.paused != lastPublishedPaused
        ) {
            TimerStateHolder.update(snapshot)
            lastPublishedSecond = wholeSecond
            lastPublishedPhase = snapshot.phase
            lastPublishedRound = snapshot.roundNumber
            lastPublishedPaused = snapshot.paused
        }
    }

    private fun resetPublishedState(snapshot: TimerSnapshot) {
        lastPublishedSecond = snapshot.remainingMs / 1000
        lastPublishedPhase = snapshot.phase
        lastPublishedRound = snapshot.roundNumber
        lastPublishedPaused = snapshot.paused
    }

    private fun handleEvent(event: TimerEvent) {
        when (event) {
            TimerEvent.Bell -> soundPlayer.playBell()
            is TimerEvent.Warning -> soundPlayer.playBeep()
        }
    }

    private fun onFinished() {
        releaseWakeLock()
        updateNotification(engine!!.snapshot, force = true)
        // Mantém a notificação de "fim" visível por alguns segundos e encerra.
        finishJob?.cancel()
        finishJob = scope.launch {
            delay(4000)
            shutdown()
        }
    }

    private fun shutdown() {
        tickJob?.cancel()
        finishJob?.cancel()
        engine?.stop()
        engine = null
        TimerStateHolder.reset()
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        finishJob?.cancel()
        scope.cancel()
        soundPlayer.release()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------- Notificação ----------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundWithNotification(snapshot: TimerSnapshot) {
        val notification = buildNotification(snapshot)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(snapshot: TimerSnapshot, force: Boolean = false) {
        val wholeSecond = snapshot.remainingMs / 1000
        if (!force && wholeSecond == lastNotifiedSecond && snapshot.paused == lastNotifiedPaused) return
        lastNotifiedSecond = wholeSecond
        lastNotifiedPaused = snapshot.paused
        notificationManager.notify(NOTIFICATION_ID, buildNotification(snapshot))
    }

    private fun buildNotification(snapshot: TimerSnapshot): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).setAction(ACTION_OPEN_TIMER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(notificationText(snapshot))

        when (snapshot.phase) {
            Phase.ROUND, Phase.REST, Phase.PREPARE -> {
                if (snapshot.paused) {
                    builder.addAction(0, getString(R.string.action_resume), servicePendingIntent(ACTION_RESUME, 1))
                } else {
                    builder.addAction(0, getString(R.string.action_pause), servicePendingIntent(ACTION_PAUSE, 2))
                }
                builder.addAction(0, getString(R.string.action_stop), servicePendingIntent(ACTION_STOP, 3))
                builder.setShowWhen(false)
            }
            Phase.FINISHED -> builder.setOngoing(false)
                .addAction(0, getString(R.string.action_stop), servicePendingIntent(ACTION_STOP, 4))
            else -> {}
        }
        return builder.build()
    }

    private fun notificationText(snapshot: TimerSnapshot): String {
        val phaseLabel = when (snapshot.phase) {
            Phase.PREPARE -> getString(R.string.phase_prepare)
            Phase.ROUND -> getString(R.string.phase_round_number, snapshot.roundNumber, snapshot.totalRounds)
            Phase.REST -> getString(R.string.phase_rest)
            Phase.FINISHED -> return getString(R.string.phase_finished)
            Phase.IDLE -> return ""
        }
        return getString(R.string.notification_timer_format, phaseLabel, formatTime(snapshot.remainingMs))
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, TimerService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "roundtimer:timer").also {
            it.acquire(12 * 60 * 60 * 1000L) // teto de 12h de segurança
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms + 999) / 1000
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }
}
