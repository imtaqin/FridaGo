package com.imtaqin.fridago.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * FridaGo theme, built on Miuix (Xiaomi HyperOS design language). Follows the
 * system light/dark setting; the whole UI uses Miuix components for a clean,
 * native HyperOS look. [Status] holds semantic colors that stay constant across
 * themes.
 */
object Status {
    val Ok = Color(0xFF22C55E)
    val Warn = Color(0xFFF5A623)
    val Err = Color(0xFFEF4444)
    val Idle = Color(0xFF8B93A3)
}

@Composable
fun CorvoTheme(content: @Composable () -> Unit) {
    val controller = remember { ThemeController(ColorSchemeMode.System) }
    MiuixTheme(controller = controller, content = content)
}
