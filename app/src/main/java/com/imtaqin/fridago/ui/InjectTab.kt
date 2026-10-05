package com.imtaqin.fridago.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imtaqin.fridago.CorvoState
import com.imtaqin.fridago.MainViewModel
import com.imtaqin.fridago.core.ScriptSource
import com.imtaqin.fridago.core.TargetApp
import com.imtaqin.fridago.ui.theme.CorvoMono
import com.imtaqin.fridago.ui.theme.Status
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val NEW_SCRIPT_TEMPLATE = """// Frida agent
Java.perform(function () {
    console.log("[fridago] attached");
});
"""

private val InjectRed = Color(0xFFEF4444)

/** Whole injection flow on one screen: target app, script (library/editor/import), inject. */
@Composable
fun InjectTab(state: CorvoState, vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var appPickerOpen by remember { mutableStateOf(state.selectedApp == null) }
    var appQuery by remember { mutableStateOf("") }
    var scriptMode by remember { mutableIntStateOf(0) } // 0 Library, 1 Editor
    var addOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var importInput by remember { mutableStateOf("") }

    LaunchedEffect(state.selectedScript?.path) {
        val sel = state.selectedScript
        if (sel != null) { name = sel.name; body = vm.readScript(sel) }
    }

    val filteredApps = remember(state.apps, appQuery) {
        if (appQuery.isBlank()) state.apps
        else state.apps.filter { it.label.contains(appQuery, true) || it.packageName.contains(appQuery, true) }
    }
    val muted = MiuixTheme.colorScheme.onSurfaceContainerVariant

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
    ) {
        // ===== TARGET APP =====
        SmallTitle("Target app")
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            val sel = state.selectedApp
            BasicComponent(
                title = sel?.label ?: "Choose a target app",
                summary = sel?.packageName,
                startAction = {
                    if (sel != null) AppAvatar(sel.packageName, sel.label)
                    else Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = muted, modifier = Modifier.padding(end = 4.dp))
                },
                endActions = {
                    Icon(if (appPickerOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = muted)
                },
                onClick = { appPickerOpen = !appPickerOpen },
            )
            if (appPickerOpen) {
                HorizontalDivider()
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(
                        value = appQuery,
                        onValueChange = { appQuery = it },
                        label = "Search apps",
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = state.includeSystemApps, onCheckedChange = { vm.setIncludeSystemApps(it) })
                            Spacer(Modifier.width(8.dp))
                            Text("System apps", style = MiuixTheme.textStyles.body2, color = muted)
                        }
                        Button(onClick = { vm.refreshApps() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (state.apps.isEmpty()) "Load" else "Reload")
                        }
                    }
                    if (filteredApps.isEmpty()) {
                        Text(
                            if (state.apps.isEmpty()) "Tap Load to scan installed apps." else "No match for “$appQuery”",
                            style = MiuixTheme.textStyles.body2, color = muted,
                        )
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().height(260.dp)) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                AppRow(app, state.selectedApp?.packageName == app.packageName) {
                                    vm.selectApp(app); appPickerOpen = false
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== SCRIPT =====
        SmallTitle("Script")
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegButton("Library", scriptMode == 0, Modifier.weight(1f)) { scriptMode = 0 }
                    SegButton("Editor", scriptMode == 1, Modifier.weight(1f)) { scriptMode = 1 }
                }

                if (scriptMode == 0) {
                    if (state.scripts.isEmpty()) {
                        Text("No scripts yet — add one from CodeShare or a URL below.", style = MiuixTheme.textStyles.body2, color = muted)
                    } else {
                        Column {
                            state.scripts.forEach { s ->
                                val isSel = state.selectedScript?.path == s.path
                                BasicComponent(
                                    title = s.name,
                                    startAction = {
                                        Icon(Icons.Filled.Description, contentDescription = null, tint = if (isSel) MiuixTheme.colorScheme.primary else muted, modifier = Modifier.padding(end = 4.dp).size(22.dp))
                                    },
                                    endActions = {
                                        if (isSel) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "active", tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        IconButton(onClick = {
                                            scope.launch { vm.deleteScript(s); if (isSel) { name = ""; body = "" } }
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "delete", tint = muted, modifier = Modifier.size(20.dp))
                                        }
                                    },
                                    onClick = { vm.selectScript(s) },
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth().clickable { addOpen = !addOpen }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add from CodeShare or URL", color = MiuixTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Icon(if (addOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = muted)
                    }
                    if (addOpen) AddScriptSection(state, vm, importInput, onImportInput = { importInput = it }, muted = muted)
                } else {
                    TextField(value = name, onValueChange = { name = it }, label = "file-name.js", useLabelAsPlaceholder = true, singleLine = true, modifier = Modifier.fillMaxWidth())
                    TextField(
                        value = body,
                        onValueChange = { body = it },
                        label = "// agent source",
                        useLabelAsPlaceholder = true,
                        textStyle = TextStyle(fontFamily = CorvoMono, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { if (name.isNotBlank()) vm.saveScript(name, body) }, enabled = name.isNotBlank(), colors = ButtonDefaults.buttonColorsPrimary(), modifier = Modifier.weight(1f)) {
                            Text("Save", color = MiuixTheme.colorScheme.onPrimary)
                        }
                        Button(onClick = { name = "agent-${System.currentTimeMillis() / 1000}.js"; body = NEW_SCRIPT_TEMPLATE }, modifier = Modifier.weight(1f)) {
                            Text("New")
                        }
                    }
                }
            }
        }

        // ===== INJECT =====
        val sel = state.selectedApp
        val script = state.selectedScript
        SmallTitle("Inject")
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            BasicComponent(title = "App", summary = sel?.label ?: "none selected", endActions = { if (sel != null) Text("ready", color = Status.Ok, style = MiuixTheme.textStyles.body2) })
            BasicComponent(title = "Script", summary = script?.name ?: "none selected", endActions = { if (script != null) Text("ready", color = Status.Ok, style = MiuixTheme.textStyles.body2) })
        }
        Box(Modifier.padding(12.dp)) {
            val injecting = state.injecting
            Button(
                onClick = {
                    if (injecting) vm.stopInject()
                    else {
                        if (!Settings.canDrawOverlays(ctx)) {
                            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                        vm.inject()
                    }
                },
                enabled = injecting || (state.serverRunning && sel != null && script != null),
                colors = if (injecting) ButtonDefaults.buttonColors(color = InjectRed) else ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(if (injecting) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = MiuixTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text(if (injecting) "Stop injection" else "Inject", color = MiuixTheme.colorScheme.onPrimary)
            }
        }
        if (!state.serverRunning && !state.injecting) {
            Text("Start the server on the Server tab first.", style = MiuixTheme.textStyles.body2, color = muted, modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

@Composable
private fun SegButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = if (selected) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
        modifier = modifier,
    ) {
        Text(label, color = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AddScriptSection(
    state: CorvoState,
    vm: MainViewModel,
    importInput: String,
    onImportInput: (String) -> Unit,
    muted: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextField(
            value = state.searchQuery,
            onValueChange = { vm.setSearchQuery(it) },
            label = "Search CodeShare: ssl pinning, …",
            useLabelAsPlaceholder = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { vm.searchScripts() }, enabled = !state.searching && state.searchQuery.isNotBlank(), colors = ButtonDefaults.buttonColorsPrimary(), modifier = Modifier.fillMaxWidth()) {
            if (state.searching) { InfiniteProgressIndicator(size = 18.dp, color = MiuixTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)) }
            Text("Search CodeShare", color = MiuixTheme.colorScheme.onPrimary)
        }
        state.searchStatus?.let { Text(it, style = MiuixTheme.textStyles.body2, color = muted) }
        state.searchResults.forEach { hit ->
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().clickable(enabled = !state.importing) { vm.importHit(hit) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(hit.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text("@${hit.owner} · ♥ ${hit.likes}", style = MiuixTheme.textStyles.body2, color = muted)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Download, contentDescription = "import", tint = MiuixTheme.colorScheme.primary)
            }
        }

        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScriptSource.entries.forEach { src ->
                val on = state.importSource == src
                Button(onClick = { vm.setImportSource(src) }, colors = if (on) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors()) {
                    Text(src.label, color = if (on) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface)
                }
            }
        }
        TextField(value = importInput, onValueChange = onImportInput, label = state.importSource.hint, useLabelAsPlaceholder = true, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.importScript(importInput) }, enabled = !state.importing && importInput.isNotBlank(), colors = ButtonDefaults.buttonColorsPrimary(), modifier = Modifier.fillMaxWidth()) {
            if (state.importing) { InfiniteProgressIndicator(size = 18.dp, color = MiuixTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)) }
            Text(if (state.importing) "Fetching…" else "Fetch script", color = MiuixTheme.colorScheme.onPrimary)
        }
        state.importStatus?.let { Text(it, style = MiuixTheme.textStyles.body2, color = if (it.startsWith("failed")) Status.Err else Status.Ok) }
    }
}

@Composable
private fun AppAvatar(packageName: String, label: String) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(avatarColor(packageName)).padding(end = 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.trim().take(1).uppercase(), color = Color.White)
    }
}

@Composable
private fun AppRow(app: TargetApp, selected: Boolean, onClick: () -> Unit) {
    BasicComponent(
        title = app.label,
        summary = app.packageName,
        startAction = { AppAvatar(app.packageName, app.label) },
        endActions = {
            if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = "selected", tint = MiuixTheme.colorScheme.primary)
            else if (app.isSystem) Icon(Icons.Filled.Public, contentDescription = "system", tint = MiuixTheme.colorScheme.onSurfaceContainerVariant, modifier = Modifier.size(18.dp))
        },
        onClick = onClick,
    )
}

private fun avatarColor(key: String): Color {
    val hues = listOf(
        Color(0xFF6750D8), Color(0xFF12896F), Color(0xFFB4531A),
        Color(0xFF1E6FBF), Color(0xFFA8336B), Color(0xFF4E7A27),
    )
    return hues[(key.hashCode() and Int.MAX_VALUE) % hues.size]
}
