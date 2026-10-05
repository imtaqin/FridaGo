package com.imtaqin.fridago.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.imtaqin.fridago.R

/**
 * Monospace family (JetBrains Mono) for machine text — endpoints, package names,
 * log lines and the agent editor. The rest of the UI uses Miuix's own typography.
 */
val CorvoMono = FontFamily(
    Font(R.font.jetbrainsmono_regular, FontWeight.Normal),
    Font(R.font.jetbrainsmono_medium, FontWeight.Medium),
)
