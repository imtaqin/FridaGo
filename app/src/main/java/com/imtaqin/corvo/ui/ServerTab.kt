package com.imtaqin.corvo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.imtaqin.corvo.CorvoState
import com.imtaqin.corvo.MainViewModel
import com.imtaqin.corvo.core.BinaryManager
import com.imtaqin.corvo.core.FridaController
import com.imtaqin.corvo.ui.theme.Status

@Composable
fun ServerTab(state: CorvoState, vm: MainViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        // ---- hero status card -------------------------------------------------
        val running = state.serverRunning
        CorvoCard(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            container = if (running) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Frida Server",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (running) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    StatusPill(
                        text = if (running) "Running" else "Stopped",
                        dot = if (running) Status.Ok else Status.Idle,
                    )
                }
                Spacer(Modifier.height(12.dp))
                MonoText(
                    FridaController.endpoint,
                    color = if (running) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 13,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (running) "On-device instrumentation is live."
                    else "One tap: binaries download if needed, then the server starts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (running) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---- details ---------------------------------------------------------
        SectionLabel("Status")
        CorvoCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column {
                val (rootText, rootColor) = when (state.rootAvailable) {
                    null -> "checking…" to Status.Idle
                    true -> "granted" to Status.Ok
                    false -> "unavailable" to Status.Err
                }
                InfoRow("Root", rootText, valueColor = rootColor)
                InfoRow("Device ABI", state.abi, mono = true)
                val (binText, binColor) = when {
                    state.binariesDeployed -> "on device" to Status.Ok
                    else -> "downloads on start" to Status.Idle
                }
                InfoRow("Binaries", binText, valueColor = binColor)
                InfoRow(
                    "frida-server",
                    if (running) "running" else "stopped",
                    valueColor = if (running) Status.Ok else Status.Idle,
                )
            }
        }

        // ---- actions ---------------------------------------------------------
        SectionLabel("Control")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Button(
                onClick = { vm.toggleServer() },
                enabled = !state.busy,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (running) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primary,
                    contentColor = if (running) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (state.busy) {
                    CircularProgressIndicator(
                        Modifier.height(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (running) "Stop server" else "Start server",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { vm.refreshServerStatus() },
                enabled = !state.busy,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh status")
            }
        }

        Text(
            "Requires root. frida-server ${BinaryManager.FRIDA_VERSION} · works with PC tools over the endpoint above.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}
