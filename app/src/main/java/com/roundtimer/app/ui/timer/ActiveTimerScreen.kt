package com.roundtimer.app.ui.timer

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roundtimer.app.R
import com.roundtimer.app.timer.Phase
import com.roundtimer.app.timer.TimerSnapshot
import com.roundtimer.app.timer.TimerStateHolder
import com.roundtimer.app.ui.theme.BlackOpsOneFont
import java.util.Locale

/**
 * Tela do timer em tela cheia, monocromática: fundo preto com o contador gigante
 * em branco (invertido durante o descanso). Qualquer toque volta às configurações
 * e o timer continua rodando em 2º plano (controlável pela notificação).
 */
@Composable
fun ActiveTimerScreen(onFinished: () -> Unit) {
    val state by TimerStateHolder.state.collectAsStateWithLifecycle()

    LockLandscapeWhileActive()
    KeepScreenOn(enabled = state.running)
    HideSystemBars()

    // Preto e branco: inversão total durante o descanso.
    val resting = state.phase == Phase.REST
    val background = if (resting) Color.White else Color.Black
    val contentColor = if (resting) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .pointerInput(Unit) { detectTapGestures { onFinished() } },
    ) {
        GiantCountdown(
            remainingMs = state.remainingMs,
            color = contentColor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Contagem regressiva gigante usando a fonte Unifraktur Cook diretamente. */
@Composable
private fun GiantCountdown(remainingMs: Long, color: Color, modifier: Modifier = Modifier) {
    val totalSeconds = (remainingMs + 999) / 1000
    val minutes = (totalSeconds / 60).toString()
    val seconds = String.format(Locale.US, "%02d", totalSeconds % 60)

    BoxWithConstraints(modifier = modifier) {
        val textMeasurer = rememberTextMeasurer()

        val refStyle = TextStyle(
            fontFamily = BlackOpsOneFont,
            fontWeight = FontWeight.Normal,
            fontSize = 120.sp,
        )
        val refText = textMeasurer.measure(AnnotatedString("00:00"), refStyle)
        val totalRefWidth = refText.size.width.toFloat()
        val lineHeight = refText.size.height.toFloat()

        val scale = minOf(
            constraints.maxWidth.toFloat() / (totalRefWidth * 1.05f),
            constraints.maxHeight.toFloat() * 0.9f / lineHeight,
        ) * 1.12f
        val fontSize = (120f * scale).sp

        Text(
            text = "$minutes:$seconds",
            color = color,
            fontFamily = BlackOpsOneFont,
            fontWeight = FontWeight.Normal,
            fontSize = fontSize,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** Esconde as barras do sistema enquanto o timer está na tela. */
@Composable
private fun HideSystemBars() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun LockLandscapeWhileActive() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = (context as? Activity)?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
