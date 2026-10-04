package com.imtaqin.fridago

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imtaqin.fridago.core.BinaryManager
import com.imtaqin.fridago.core.FridaController
import com.imtaqin.fridago.core.InstalledApps
import com.imtaqin.fridago.core.LogBus
import com.imtaqin.fridago.core.RootManager
import com.imtaqin.fridago.core.ScriptFetcher
import com.imtaqin.fridago.core.ScriptFile
import com.imtaqin.fridago.core.ScriptMigrator
import com.imtaqin.fridago.core.ScriptSource
import com.imtaqin.fridago.core.ScriptStore
import com.imtaqin.fridago.core.TargetApp
import com.imtaqin.fridago.service.FloatingLogService
import com.imtaqin.fridago.service.FridaForegroundService
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CorvoState(
    val rootAvailable: Boolean? = null,       // null = not yet checked
    val binariesDeployed: Boolean = false,
    val serverRunning: Boolean = false,
    val abi: String = BinaryManager.primaryAbi,
    val apps: List<TargetApp> = emptyList(),
    val includeSystemApps: Boolean = false,
    val scripts: List<ScriptFile> = emptyList(),
    val selectedApp: TargetApp? = null,
    val selectedScript: ScriptFile? = null,
    val busy: Boolean = false,
    val injecting: Boolean = false,
    val importSource: ScriptSource = ScriptSource.CodeShare,
    val importing: Boolean = false,
    val importStatus: String? = null,       // null = idle
    val searchQuery: String = "",
    val searching: Boolean = false,
    val searchResults: List<ScriptFetcher.CodeShareHit> = emptyList(),
    val searchStatus: String? = null,       // null = idle
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx get() = getApplication<Application>()

    private val _state = MutableStateFlow(CorvoState())
    val state: StateFlow<CorvoState> = _state.asStateFlow()

    // Logs live in a process-wide bus so the floating overlay can read them too.
    val log: StateFlow<List<String>> = LogBus.log

    init {
        bootstrap()
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(100)
                flushLog()
            }
        }
    }

    private fun bootstrap() = viewModelScope.launch {
        setBusy(true)
        val root = RootManager.isRootAvailable()
        _state.update {
            it.copy(
                rootAvailable = root,
                binariesDeployed = BinaryManager.isDeployed(ctx),
            )
        }
        logLine(if (root) "[+] root granted" else "[!] root unavailable — grant su to FridaGo")
        ScriptStore.seedDefaults(ctx)
        refreshScripts()
        if (root) refreshServerStatus()
        setBusy(false)
    }

    // ---- logging ------------------------------------------------------------

    private val ts = SimpleDateFormat("HH:mm:ss", Locale.US)

    // Lines are buffered and flushed at most every 100ms: a chatty agent script
    // used to emit hundreds of StateFlow updates per second, each one copying the
    // list and recomposing the whole log — that was the inject-time UI freeze.
    private val pendingLog = ArrayDeque<String>()
    private val logLock = Any()

    fun logLine(line: String) {
        synchronized(logLock) {
            pendingLog.addLast("${ts.format(Date())}  $line")
        }
    }

    private fun flushLog() {
        val batch: List<String>
        synchronized(logLock) {
            if (pendingLog.isEmpty()) return
            batch = pendingLog.toList()
            pendingLog.clear()
        }
        LogBus.append(batch)
    }

    fun clearLog() {
        synchronized(logLock) { pendingLog.clear() }
        LogBus.clear()
    }

    // ---- server -------------------------------------------------------------

    fun refreshServerStatus() = viewModelScope.launch {
        val running = FridaController.isServerRunning(ctx)
        _state.update { it.copy(serverRunning = running) }
    }

    fun toggleServer() = viewModelScope.launch {
        setBusy(true)
        if (_state.value.serverRunning) {
            FridaController.stopServer(ctx, ::logLine)
            FridaForegroundService.stop(ctx)
            _state.update { it.copy(serverRunning = false) }
        } else {
            // Re-check root on every start: su may have been granted after the
            // first probe, and libsu caches a non-root shell from that attempt.
            val root = RootManager.isRootAvailableFresh()
            _state.update { it.copy(rootAvailable = root) }
            if (!root) {
                logLine("[!] root unavailable — grant su to FridaGo, then tap Start again")
                setBusy(false)
                return@launch
            }
            // Deploy-or-download: no-op when the binaries are already on disk.
            val ready = runCatching { BinaryManager.ensureBinaries(ctx, ::logLine) }
                .onSuccess { _state.update { it.copy(binariesDeployed = true) } }
                .onFailure { logLine("[!] ${it.message}") }
                .isSuccess
            if (!ready) {
                setBusy(false)
                return@launch
            }
            FridaForegroundService.start(ctx)
            val started = FridaController.startServer(ctx, ::logLine)
            // Don't leave the keep-alive notification up if the server never came up.
            if (!started) FridaForegroundService.stop(ctx)
            _state.update { it.copy(serverRunning = started) }
        }
        setBusy(false)
    }

    // ---- apps ---------------------------------------------------------------

    fun refreshApps() = viewModelScope.launch {
        setBusy(true)
        val apps = InstalledApps.list(ctx, _state.value.includeSystemApps)
        _state.update { it.copy(apps = apps) }
        logLine("[*] loaded ${apps.size} packages")
        setBusy(false)
    }

    fun setIncludeSystemApps(include: Boolean) {
        _state.update { it.copy(includeSystemApps = include) }
        refreshApps()
    }

    fun selectApp(app: TargetApp) = _state.update { it.copy(selectedApp = app) }

    // ---- scripts ------------------------------------------------------------

    fun refreshScripts() = viewModelScope.launch {
        val scripts = ScriptStore.list(ctx)
        _state.update { st ->
            st.copy(
                scripts = scripts,
                selectedScript = st.selectedScript?.takeIf { sel ->
                    scripts.any { it.path == sel.path }
                } ?: scripts.firstOrNull(),
            )
        }
    }

    fun selectScript(script: ScriptFile) = _state.update { it.copy(selectedScript = script) }

    suspend fun readScript(script: ScriptFile): String = ScriptStore.read(script)

    fun saveScript(name: String, body: String) = viewModelScope.launch {
        val saved = ScriptStore.save(ctx, name, body)
        logLine("[+] saved ${saved.name}")
        refreshScripts()
        _state.update { it.copy(selectedScript = saved) }
    }

    fun deleteScript(script: ScriptFile) = viewModelScope.launch {
        ScriptStore.delete(script)
        logLine("[*] deleted ${script.name}")
        refreshScripts()
    }

    // ---- import -------------------------------------------------------------

    fun setImportSource(source: ScriptSource) =
        _state.update { it.copy(importSource = source, importStatus = null) }

    // ---- search --------------------------------------------------------------

    fun setSearchQuery(q: String) =
        _state.update { it.copy(searchQuery = q) }

    /** Keyword search across Frida CodeShare; results land in [CorvoState.searchResults]. */
    fun searchScripts() {
        val q = _state.value.searchQuery.trim()
        if (q.isEmpty()) {
            _state.update { it.copy(searchStatus = "type a keyword first") }
            return
        }
        _state.update { it.copy(searching = true, searchStatus = null, searchResults = emptyList()) }
        viewModelScope.launch {
            runCatching { ScriptFetcher.searchCodeShare(q) }
                .onSuccess { hits ->
                    logLine("[*] code share search '$q': ${hits.size} hits")
                    _state.update {
                        it.copy(
                            searching = false,
                            searchResults = hits,
                            searchStatus = if (hits.isEmpty()) "no hits" else null,
                        )
                    }
                }
                .onFailure { e ->
                    logLine("[!] search failed: ${e.message}")
                    _state.update { it.copy(searching = false, searchStatus = "search failed: ${e.message}") }
                }
        }
    }

    /** Imports a tapped CodeShare hit by its `owner/slug` reference. */
    fun importHit(hit: ScriptFetcher.CodeShareHit) = importFrom(ScriptSource.CodeShare, hit.ref)

    /** Downloads an agent script from the selected source and saves it locally. */
    fun importScript(input: String) = importFrom(_state.value.importSource, input)

    private fun importFrom(source: ScriptSource, input: String) {
        if (input.isBlank()) {
            _state.update { it.copy(importStatus = "enter a ${source.label} reference") }
            return
        }
        _state.update { it.copy(importing = true, importStatus = null) }
        viewModelScope.launch {
            runCatching { ScriptFetcher.fetch(source, input.trim()) }
                .onSuccess { fetched ->
                    val saved = ScriptStore.save(ctx, fetched.name, fetched.body)
                    logLine("[+] imported ${saved.name} from ${source.label}")
                    refreshScripts()
                    _state.update {
                        it.copy(
                            selectedScript = saved,
                            importing = false,
                            importStatus = "saved ${saved.name}",
                        )
                    }
                }
                .onFailure { e ->
                    logLine("[!] import failed: ${e.message}")
                    _state.update { it.copy(importing = false, importStatus = "failed: ${e.message}") }
                }
        }
    }

    // ---- inject -------------------------------------------------------------

    // Generation counter, not a Job handle: coroutine cancellation can't interrupt
    // the blocking shell exec anyway — the frida-inject *process* is what ends a
    // session. The counter keeps a late-finishing old session from clearing the
    // flag of a newer one.
    private var injectGen = 0

    fun inject() {
        val st = _state.value
        val app = st.selectedApp
        val script = st.selectedScript
        when {
            !st.serverRunning -> logLine("[!] start the server first")
            app == null -> logLine("[!] pick a target app")
            script == null -> logLine("[!] pick a script")
            else -> {
                val gen = ++injectGen
                _state.update { it.copy(injecting = true) }
                // Pop the floating log overlay so output stays visible once the
                // user switches into the target app. No-op without the permission.
                if (Settings.canDrawOverlays(ctx)) FloatingLogService.start(ctx)
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        val migrated = ScriptMigrator.migrate(ctx, script)
                        if (migrated.usedBridge || migrated.usedShim) {
                            logLine(
                                "[*] auto-upgraded script for Frida 17: " +
                                    listOfNotNull(
                                        migrated.usedBridge.takeIf { it }?.let { "java-bridge" },
                                        migrated.usedShim.takeIf { it }?.let { "legacy-api shim" },
                                    ).joinToString(" + "),
                            )
                        }
                        FridaController.inject(ctx, app.packageName, migrated.file.absolutePath, ::logLine)
                    }.onFailure { e ->
                        if (e !is kotlinx.coroutines.CancellationException) {
                            logLine("[!] inject error: ${e.message}")
                        }
                    }
                    if (gen == injectGen) _state.update { it.copy(injecting = false) }
                }
            }
        }
    }

    fun stopInject() {
        // pkill the client on the free main shell; the dedicated stream then
        // returns, the session logs "[=] inject session ended", and the job
        // unwinds on its own — no coroutine cancellation involved.
        viewModelScope.launch {
            FridaController.stopInject(ctx, ::logLine)
            _state.update { it.copy(injecting = false) }
        }
    }

    private fun setBusy(b: Boolean) = _state.update { it.copy(busy = b) }
}
