package com.imtaqin.fridago.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imtaqin.fridago.MainViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    Server("Server", Icons.Filled.Dns),
    Inject("Inject", Icons.Filled.Bolt),
    Logs("Logs", Icons.Filled.Terminal),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorvoRoot(vm: MainViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = Tab.entries
    val state by vm.state.collectAsStateWithLifecycle()
    val log by vm.log.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(tabs[tab].label, fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    if (tabs[tab] == Tab.Logs) {
                        TextButton(onClick = { vm.clearLog() }) { Text("Clear") }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { inner ->
        when (tabs[tab]) {
            Tab.Server -> ServerTab(state, vm, Modifier.padding(inner))
            Tab.Inject -> InjectTab(state, vm, Modifier.padding(inner))
            Tab.Logs -> LogTab(log, Modifier.padding(inner))
        }
    }
}
