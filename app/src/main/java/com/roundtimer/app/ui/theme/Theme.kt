package com.roundtimer.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.roundtimer.app.R

// Paleta monocromática: preto e branco puros.
val PureBlack = Color(0xFF000000)
val PureWhite = Color(0xFFFFFFFF)
val DarkSurface = Color(0xFF161616)
val SurfaceVariant = Color(0xFF242424)
val GrayText = Color(0xFF9E9E9E)

/** Fonte principal do timer em tela cheia. */
val BlackOpsOneFont = FontFamily(Font(R.font.blackopsone_regular))

private val DarkColors = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    secondary = PureWhite,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = GrayText,
)

@Composable
fun RoundTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
