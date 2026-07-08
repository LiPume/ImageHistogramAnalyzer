package com.lzx.imagehistogramanalyzer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SkyPrimary,
    onPrimary = OnSkyPrimary,
    primaryContainer = SkyPrimaryContainer,
    onPrimaryContainer = OnSkyPrimaryContainer,
    secondary = MintSecondary,
    onSecondary = OnMintSecondary,
    secondaryContainer = MintSecondaryContainer,
    onSecondaryContainer = OnMintSecondaryContainer,
    tertiary = CitrusTertiary,
    onTertiary = OnCitrusTertiary,
    tertiaryContainer = CitrusTertiaryContainer,
    onTertiaryContainer = OnCitrusTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightBackground,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
)

@Composable
fun ImageHistogramAnalyzerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
