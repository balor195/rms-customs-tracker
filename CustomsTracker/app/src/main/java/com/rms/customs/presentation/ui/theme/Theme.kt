package com.rms.customs.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RmsGreen        = Color(0xFF1B5E20)
private val RmsGreenLight   = Color(0xFF2E7D32)
private val RmsGold         = Color(0xFFF9A825)
private val RmsGoldLight    = Color(0xFFFBC02D)
private val OverdueRed      = Color(0xFFB71C1C)
private val WarnAmber       = Color(0xFFF57F17)
private val OkGreen         = Color(0xFF1B5E20)

private val LightColors = lightColorScheme(
    primary          = RmsGreenLight,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    secondary        = RmsGold,
    onSecondary      = Color.Black,
    background       = Color(0xFFF5F5F5),
    surface          = Color.White,
    error            = OverdueRed,
)

private val DarkColors = darkColorScheme(
    primary          = Color(0xFF81C784),
    onPrimary        = Color.Black,
    primaryContainer = RmsGreen,
    secondary        = RmsGoldLight,
    onSecondary      = Color.Black,
    background       = Color(0xFF121212),
    surface          = Color(0xFF1E1E1E),
    error            = Color(0xFFEF9A9A),
)

@Composable
fun CustomsTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

object CustomsColors {
    val Overdue  = OverdueRed
    val Warning  = WarnAmber
    val OnTime   = OkGreen
    val Military = Color(0xFF1A237E)  // deep navy for Military Command highlight
}
