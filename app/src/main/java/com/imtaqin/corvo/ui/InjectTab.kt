package com.imtaqin.corvo.ui

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
import androidx.compose.material.icons.filled.Code
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imtaqin.corvo.CorvoState
import com.imtaqin.corvo.MainViewModel
import com.imtaqin.corvo.core.ScriptSource
import com.imtaqin.corvo.core.TargetApp
import com.imtaqin.corvo.ui.theme.CorvoMono
import com.imtaqin.corvo.ui.theme.Status
import kotlinx.coroutines.launch

private const val NEW_SCRIPT_TEMPLATE = """// Frida agent
Java.perform(function () {
    console.log("[fridago] attached");
});
"""

/**
 * The whole injection flow on one screen: choose a target app, pick a script
 * from the library (or write/import one), then inject — no tab hopping.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InjectTab(state: CorvoState, vm: MainViewModel, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var appPickerOpen by remember { mutableStateOf(state.selectedApp == null) }
    var appQuery by remember { mutableStateOf("") }
    var scriptMode by rememberSaveable { mutableIntStateOf(0) } // 0 = Library, 1 = Editor
    var addOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var importInput by remember { mutableStateOf("") }

    LaunchedEffect(state.selectedScript?.path) {
        val sel = state.selectedScript
        if (sel != null) {
            name = sel.name
            body = vm.readScript(sel)
        }
    }

    val filteredApps = remember(state.apps, appQuery) {
        if (appQuery.isBlank()) state.apps
        else state.apps.filter {
            it.label.contains(appQuery, true) || it.packageName.contains(appQuery, true)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        // ===== 1 · TARGET APP ===================================================
        SectionLabel("Target app")
        CorvoCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { appPickerOpen = !appPickerOpen }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val sel = state.selectedApp
                    if (sel != null) {
                        AppAvatar(sel.packageName, sel.label)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                sel.label,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                sel.packageName,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = CorvoMono),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Icon(
                            Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Choose a target app",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Icon(
                        if (appPickerOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (appPickerOpen) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = appQuery,
                            onValueChange = { appQuery = it },
                            placeholder = { Text("Search apps") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = state.includeSystemApps,
                                    onCheckedChange = { vm.setIncludeSystemApps(it) },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "System apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(onClick = { vm.refreshApps() }, shape = MaterialTheme.shapes.large) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (state.apps.isEmpty()) "Load" else "Reload")
                            }
                        }
                        if (filteredApps.isEmpty()) {
                            Text(
                                if (state.apps.isEmpty()) "Tap Load to scan installed apps."
                                else "Nothing matches “$appQuery”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        } else {
                            LazyColumn(Modifier.fillMaxWidth().height(260.dp)) {
                                items(filteredApps, key = { it.packageName }) { app ->
                                    AppRow(
                                        app = app,
                                        selected = state.selectedApp?.packageName == app.packageName,
                                        onClick = {
                                            vm.selectApp(app)
                                            appPickerOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== 2 · SCRIPT =======================================================
        SectionLabel("Script")
        CorvoCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Library", "Editor").forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = scriptMode == i,
                            onClick = { scriptMode = i },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(label) }
                    }
                }

                if (scriptMode == 0) {
                    // ---- library ----
                    if (state.scripts.isEmpty()) {
                        Text(
                            "No scripts yet — add one from CodeShare or a URL below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column {
                            state.scripts.forEach { s ->
                                val isSelected = state.selectedScript?.path == s.path
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { vm.selectScript(s) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Filled.Description,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        s.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "active",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            vm.deleteScript(s)
                                            if (isSelected) { name = ""; body = "" }
                                        }
                                    }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ---- add / discover (collapsible) ----
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().clickable { addOpen = !addOpen }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Add from CodeShare or URL",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (addOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (addOpen) AddScriptSection(state, vm, importInput, onImportInput = { importInput = it })
                } else {
                    // ---- editor ----
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("file-name.js") },
                        leadingIcon = { Icon(Icons.Filled.Code, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        placeholder = { Text("// agent source") },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = CorvoMono),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (name.isNotBlank()) vm.saveScript(name, body) },
                            enabled = name.isNotBlank(),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.weight(1f),
                        ) { Text("Save") }
                        OutlinedButton(
                            onClick = {
                                name = "agent-${System.currentTimeMillis() / 1000}.js"
                                body = NEW_SCRIPT_TEMPLATE
                            },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.weight(1f),
                        ) { Text("New") }
                    }
                }
            }
        }

        // ===== 3 · INJECT =======================================================
        val sel = state.selectedApp
        val script = state.selectedScript
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(
                    "app: ${sel?.label ?: "none"}",
                    color = if (sel != null) Status.Ok else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatChip(
                    "script: ${script?.name ?: "none"}",
                    color = if (script != null) Status.Ok else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    if (state.injecting) {
                        vm.stopInject()
                    } else {
                        // Ask for the overlay permission once so logs can float
                        // over the target app; inject either way.
                        if (!Settings.canDrawOverlays(ctx)) {
                            ctx.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${ctx.packageName}"),
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                        vm.inject()
                    }
                },
                enabled = state.injecting || (state.serverRunning && sel != null && script != null),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.injecting) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primary,
                    contentColor = if (state.injecting) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(
                    if (state.injecting) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.injecting) "Stop injection" else "Inject",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (!state.serverRunning && !state.injecting) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Start the server on the Server tab first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---- add-from-source section (CodeShare search + URL import) -----------------

@Composable
private fun AddScriptSection(
    state: CorvoState,
    vm: MainViewModel,
    importInput: String,
    onImportInput: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // CodeShare keyword search
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { vm.setSearchQuery(it) },
            placeholder = { Text("Search CodeShare: ssl pinning, …") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { vm.searchScripts() },
            enabled = !state.searching && state.searchQuery.isNotBlank(),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.searching) {
                CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("Search CodeShare")
        }
        state.searchStatus?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.searchResults.forEach { hit ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !state.importing) { vm.importHit(hit) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        hit.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatChip("@${hit.owner}")
                        StatChip("♥ ${hit.likes}")
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Download, contentDescription = "import", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Direct URL import
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScriptSource.entries.forEach { src ->
                FilterChip(
                    selected = state.importSource == src,
                    onClick = { vm.setImportSource(src) },
                    label = { Text(src.label) },
                )
            }
        }
        OutlinedTextField(
            value = importInput,
            onValueChange = onImportInput,
            placeholder = { Text(state.importSource.hint) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { vm.importScript(importInput) },
            enabled = !state.importing && importInput.isNotBlank(),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.importing) {
                CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (state.importing) "Fetching…" else "Fetch script")
        }
        state.importStatus?.let { status ->
            Text(
                status,
                style = MaterialTheme.typography.bodyMedium,
                color = if (status.startsWith("failed")) MaterialTheme.colorScheme.error else Status.Ok,
            )
        }
    }
}

// ---- shared app bits --------------------------------------------------------

@Composable
private fun AppAvatar(packageName: String, label: String, sizeDp: Int = 42) {
    Box(
        Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(avatarColor(packageName)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AppRow(app: TargetApp, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppAvatar(app.packageName, app.label)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = CorvoMono),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "selected", tint = MaterialTheme.colorScheme.primary)
        } else if (app.isSystem) {
            Icon(
                Icons.Filled.Public,
                contentDescription = "system app",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun avatarColor(key: String): Color {
    val hues = listOf(
        Color(0xFF6750D8), Color(0xFF12896F), Color(0xFFB4531A),
        Color(0xFF1E6FBF), Color(0xFFA8336B), Color(0xFF4E7A27),
    )
    return hues[(key.hashCode() and Int.MAX_VALUE) % hues.size]
}
