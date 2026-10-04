package com.imtaqin.fridago.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.imtaqin.fridago.R

/**
 * Corvo typography. [Inter] carries the whole UI; [CorvoMono] (JetBrains Mono)
 * is used wherever we show machine text — endpoints, package names, log lines,
 * the agent editor. The scale below is tightened for a dense tool UI: slightly
 * negative tracking on big text, generous line height on body.
 */

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val CorvoMono = FontFamily(
    Font(R.font.jetbrainsmono_regular, FontWeight.Normal),
    Font(R.font.jetbrainsmono_medium, FontWeight.Medium),
)

private fun inter(
    weight: FontWeight,
    size: Int,
    line: Int,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

val CorvoTypography = Typography(
    headlineMedium = inter(FontWeight.Bold, 28, 34, -0.4),
    headlineSmall = inter(FontWeight.Bold, 24, 30, -0.3),
    titleLarge = inter(FontWeight.SemiBold, 20, 26, -0.2),
    titleMedium = inter(FontWeight.SemiBold, 16, 22),
    titleSmall = inter(FontWeight.Medium, 14, 20),
    bodyLarge = inter(FontWeight.Normal, 16, 23),
    bodyMedium = inter(FontWeight.Normal, 14, 20),
    bodySmall = inter(FontWeight.Normal, 12, 17),
    labelLarge = inter(FontWeight.SemiBold, 14, 18, 0.1),
    labelMedium = inter(FontWeight.Medium, 12, 16, 0.3),
    labelSmall = inter(FontWeight.Medium, 11, 15, 0.4),
)
