package com.rms.customs.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── RMS Brand palette ────────────────────────────────────────────────────────
private val NavyDeep        = Color(0xFF1A237E)   // primary – authority / trust
private val NavyMid         = Color(0xFF1565C0)   // secondary – actions / links
private val NavyLight       = Color(0xFFE8EAF6)   // primaryContainer – tonal chips / selection
private val NavyLightDark   = Color(0xFF283593)   // primaryContainer in dark mode

// Semantic status colours (unchanged – they are correct for their roles)
private val OverdueRed      = Color(0xFFB71C1C)
private val WarnAmber       = Color(0xFFF57F17)
private val OkGreen         = Color(0xFF1B5E20)
private val GoldAccentColor  = Color(0xFFF9A825)   // badges, monetary values only

private val LightColors = lightColorScheme(
    primary               = NavyDeep,
    onPrimary             = Color.White,
    primaryContainer      = NavyLight,
    onPrimaryContainer    = NavyDeep,
    secondary             = NavyMid,
    onSecondary           = Color.White,
    secondaryContainer    = Color(0xFFBBDEFB),
    onSecondaryContainer  = Color(0xFF003C8F),
    background            = Color(0xFFF0F2F8),    // cool-tinted off-white
    onBackground          = Color(0xFF0D1033),
    surface               = Color.White,
    onSurface             = Color(0xFF0D1033),
    surfaceVariant        = Color(0xFFE8EAF6),
    onSurfaceVariant      = Color(0xFF3949AB),
    error                 = OverdueRed,
    onError               = Color.White,
)

private val DarkColors = darkColorScheme(
    primary               = Color(0xFF7986CB),    // light indigo – readable on dark
    onPrimary             = Color(0xFF000051),
    primaryContainer      = NavyLightDark,
    onPrimaryContainer    = NavyLight,
    secondary             = Color(0xFF64B5F6),
    onSecondary           = Color(0xFF003C8F),
    secondaryContainer    = Color(0xFF1565C0),
    onSecondaryContainer  = Color(0xFFBBDEFB),
    background            = Color(0xFF0F111A),    // navy-tinted dark
    onBackground          = Color(0xFFE8EAF6),
    surface               = Color(0xFF1A1C2A),    // dark navy surface
    onSurface             = Color(0xFFE8EAF6),
    surfaceVariant        = Color(0xFF252842),
    onSurfaceVariant      = Color(0xFF9FA8DA),
    error                 = Color(0xFFEF9A9A),
    onError               = Color(0xFF690000),
)

@Composable
fun CustomsTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = RmsTypography,
        content     = content,
    )
}

object CustomsColors {
    val Overdue     = OverdueRed
    val Warning     = WarnAmber
    val OnTime      = OkGreen
    val Military    = NavyDeep     // primary navy now doubles as the Military highlight
    val GoldAccent  = GoldAccentColor  // use only for monetary values / special badges
}
