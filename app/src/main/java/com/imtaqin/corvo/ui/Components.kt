package com.imtaqin.corvo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imtaqin.corvo.ui.theme.CorvoMono

/**
 * Small shared design pieces so every tab speaks the same visual language:
 * [SectionLabel] headers, [CorvoCard] surfaces, [InfoRow] key/value rows and
 * [StatusPill] / [StatChip] pill badges.
 */

private val CardShape = RoundedCornerShape(20.dp)

/** Section header above a card group. */
@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Elevated-but-flat content surface used everywhere. */
@Composable
fun CorvoCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = CardShape,
        color = container,
        modifier = modifier,
    ) {
        content()
    }
}

/** Key/value status row inside a card. */
@Composable
fun InfoRow(label: String, value: String, mono: Boolean = false, valueColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            value,
            style = if (mono) MaterialTheme.typography.bodyMedium.copy(fontFamily = CorvoMono)
            else MaterialTheme.typography.bodyMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Pill badge with a leading status dot. */
@Composable
fun StatusPill(text: String, dot: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(dot.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = dot,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Neutral pill badge for small metadata (stats, versions, tags). */
@Composable
fun StatChip(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Small monospace text block (endpoints, code, package names). */
@Composable
fun MonoText(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, size: Int = 12) {
    Text(
        text,
        fontFamily = CorvoMono,
        fontSize = size.sp,
        color = color,
        lineHeight = (size + 6).sp,
    )
}
