package com.lzx.imagehistogramanalyzer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = IndigoPrimary,
    secondary = TealSecondary,
    background = LightBackground,
)

private val DarkColors = darkColorScheme(
    primary = IndigoPrimaryDark,
    secondary = TealSecondaryDark,
    background = DarkBackground,
)

@Composable
fun ImageHistogramAnalyzerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
