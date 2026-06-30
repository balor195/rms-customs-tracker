package com.rms.customs.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand colors visible across both screens
val ColorDelayed  = Color(0xFFD32F2F)   // red   — overdue / blocked
val ColorWarning  = Color(0xFFF57C00)   // amber — approaching SLA
val ColorOk       = Color(0xFF388E3C)   // green — on track / done
val ColorSurface  = Color(0xFFF5F5F5)   // page background

private val LightColors = lightColorScheme(
    primary          = Color(0xFF1565C0),   // deep blue — military/professional
    onPrimary        = Color.White,
    secondary        = Color(0xFF37474F),
    tertiary         = Color(0xFF00695C),
    background       = ColorSurface,
    surface          = Color.White,
    onBackground     = Color(0xFF212121),
    onSurface        = Color(0xFF212121),
    surfaceVariant   = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF616161),
)

@Composable
fun CustomsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
