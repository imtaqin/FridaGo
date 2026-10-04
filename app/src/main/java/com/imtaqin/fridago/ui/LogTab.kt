package com.imtaqin.fridago.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imtaqin.fridago.service.FloatingLogService
import com.imtaqin.fridago.ui.theme.CorvoMono
import com.imtaqin.fridago.ui.theme.Status

/**
 * Terminal-style log viewer. Lines are color-coded by prefix: `[!]` errors,
 * `[+]` success, `[*]` notes, everything else neutral. A "Float over apps"
 * toggle pops the same stream out into a draggable overlay window.
 */
@Composable
fun LogTab(log: List<String>, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val floating by FloatingLogService.running.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.scrollToItem(log.lastIndex)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            FilledTonalButton(
                onClick = {
                    if (floating) {
                        FloatingLogService.stop(ctx)
                    } else if (Settings.canDrawOverlays(ctx)) {
                        FloatingLogService.start(ctx)
                    } else {
                        ctx.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${ctx.packageName}"),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    if (floating) Icons.Filled.Close else Icons.Filled.OpenInNew,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (floating) "Close floating" else "Float over apps")
            }
        }

        if (log.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No output yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Server and inject logs will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            CorvoCard(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                container = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(log) { line ->
                        val color = when {
                            line.contains("[!]") -> Status.Err
                            line.contains("[+]") -> Status.Ok
                            line.contains("[*]") -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            line,
                            fontFamily = CorvoMono,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}
