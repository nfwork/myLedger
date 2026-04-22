package com.myledger.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = PrimaryDark,
    secondary = TealLight,
    background = Bg,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Line,
)

@Composable
fun MyLedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
