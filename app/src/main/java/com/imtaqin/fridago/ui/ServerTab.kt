package com.imtaqin.fridago.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.imtaqin.fridago.CorvoState
import com.imtaqin.fridago.MainViewModel
import com.imtaqin.fridago.core.BinaryManager
import com.imtaqin.fridago.core.FridaController
import com.imtaqin.fridago.ui.theme.CorvoMono
import com.imtaqin.fridago.ui.theme.Status
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ServerTab(state: CorvoState, vm: MainViewModel) {
    val muted = MiuixTheme.colorScheme.onSurfaceContainerVariant
    val running = state.serverRunning

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Text(
            "On-device Frida control. Server and client run locally over ${FridaController.endpoint}.",
            style = MiuixTheme.textStyles.body2,
            color = muted,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )

        SmallTitle("Status")
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            val (rootText, rootColor) = when (state.rootAvailable) {
                null -> "checking…" to Status.Idle
                true -> "granted" to Status.Ok
                false -> "unavailable" to Status.Err
            }
            StatusRow("Root", rootText, rootColor)
            StatusRow("Device ABI", state.abi, muted, mono = true)
            val (binText, binColor) =
                if (state.binariesDeployed) "on device" to Status.Ok
                else "downloads on start" to Status.Idle
            StatusRow("Binaries", binText, binColor)
            StatusRow(
                "frida-server",
                if (running) "running" else "stopped",
                if (running) Status.Ok else Status.Idle,
            )
        }

        SmallTitle("Control")
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            ArrowPreference(
                title = if (running) "Stop server" else "Start server",
                summary = "Endpoint ${FridaController.endpoint}",
                enabled = !state.busy,
                onClick = { vm.toggleServer() },
            )
            ArrowPreference(
                title = "Refresh status",
                onClick = { vm.refreshServerStatus() },
            )
        }

        if (state.busy) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InfiniteProgressIndicator()
            }
        }

        Text(
            "Requires root. frida-server ${BinaryManager.FRIDA_VERSION} · works with PC tools over the endpoint above.",
            style = MiuixTheme.textStyles.body2,
            color = muted,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String, valueColor: Color, mono: Boolean = false) {
    BasicComponent(
        title = label,
        endActions = {
            Text(
                value,
                style = MiuixTheme.textStyles.body2,
                color = valueColor,
                fontFamily = if (mono) CorvoMono else null,
            )
        },
    )
}
