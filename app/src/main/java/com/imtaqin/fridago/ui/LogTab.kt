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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
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
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LogTab(log: List<String>) {
    val ctx = LocalContext.current
    val floating by FloatingLogService.running.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.scrollToItem(log.lastIndex)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
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
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Icon(
                    if (floating) Icons.Filled.Close else Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (floating) "Close floating" else "Float over apps",
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }

        if (log.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No output yet", style = MiuixTheme.textStyles.title2)
                    Text(
                        "Server and inject logs appear here.",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
            }
        } else {
            Card(Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.dp)) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(log) { line ->
                        Text(
                            line,
                            fontFamily = CorvoMono,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = when {
                                line.contains("[!]") -> Status.Err
                                line.contains("[+]") -> Status.Ok
                                line.contains("[*]") -> MiuixTheme.colorScheme.primary
                                else -> MiuixTheme.colorScheme.onSurfaceContainerVariant
                            },
                        )
                    }
                }
            }
        }
    }
}
