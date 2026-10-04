package com.imtaqin.fridago.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Corvo design tokens.
 *
 * A restrained violet-on-ink palette: deep near-black surfaces for dark (it is
 * a developer tool) and soft paper-white for light. The brand scheme is used
 * in both modes so the app keeps a consistent identity; [Status] holds the
 * semantic colors that stay stable across themes.
 */

// ---- brand: violet + mint accent -------------------------------------------

private val Violet = Color(0xFF6750D8)
private val VioletDark = Color(0xFF9B8AFB)
private val Mint = Color(0xFF12896F)
private val MintDark = Color(0xFF5ED3B6)

private val InkDarkScheme = darkColorScheme(
    primary = VioletDark,
    onPrimary = Color(0xFF211653),
    primaryContainer = Color(0xFF372B78),
    onPrimaryContainer = Color(0xFFE7E0FF),
    secondary = MintDark,
    onSecondary = Color(0xFF05332A),
    secondaryContainer = Color(0xFF0E463A),
    onSecondaryContainer = Color(0xFFC6F4E6),
    background = Color(0xFF0C0E14),
    onBackground = Color(0xFFE7EAF2),
    surface = Color(0xFF0C0E14),
    onSurface = Color(0xFFE7EAF2),
    surfaceVariant = Color(0xFF1B202C),
    onSurfaceVariant = Color(0xFFA3ABB9),
    surfaceContainerLowest = Color(0xFF090B10),
    surfaceContainerLow = Color(0xFF10131A),
    surfaceContainer = Color(0xFF141821),
    surfaceContainerHigh = Color(0xFF1A1F2A),
    surfaceContainerHighest = Color(0xFF222836),
    outline = Color(0xFF2C3242),
    outlineVariant = Color(0xFF232936),
    error = Color(0xFFFF8087),
    onError = Color(0xFF4B0006),
    errorContainer = Color(0xFF7A2129),
    onErrorContainer = Color(0xFFFFDADA),
)

private val PaperLightScheme = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E0FF),
    onPrimaryContainer = Color(0xFF221655),
    secondary = Mint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC9F2E5),
    onSecondaryContainer = Color(0xFF043027),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF171A22),
    surface = Color(0xFFF6F7FB),
    onSurface = Color(0xFF171A22),
    surfaceVariant = Color(0xFFE8EAF1),
    onSurfaceVariant = Color(0xFF59606F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F2F8),
    surfaceContainer = Color(0xFFEAECF3),
    surfaceContainerHigh = Color(0xFFE3E5EE),
    surfaceContainerHighest = Color(0xFFD9DCE7),
    outline = Color(0xFFBEC4D1),
    outlineVariant = Color(0xFFD5D9E3),
    error = Color(0xFFB3262E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

/** Semantic colors — constant regardless of theme. */
object Status {
    val Ok = Color(0xFF22C55E)
    val Warn = Color(0xFFF5A623)
    val Err = Color(0xFFEF4444)
    val Idle = Color(0xFF8B93A3)
}

@Composable
fun CorvoTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) InkDarkScheme else PaperLightScheme
    MaterialTheme(colorScheme = colorScheme, typography = CorvoTypography, content = content)
}
