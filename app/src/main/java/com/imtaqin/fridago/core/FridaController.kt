package com.imtaqin.fridago.core

import android.content.Context
import kotlinx.coroutines.delay

/**
 * Orchestrates the two frida roles over root:
 *  - [startServer]/[stopServer] manage a detached `frida-server` listening on localhost.
 *  - [inject] runs the self-contained `frida-inject` client against a target package.
 */
object FridaController {

    const val HOST = "127.0.0.1"
    const val PORT = 27042
    val endpoint: String get() = "$HOST:$PORT"

    /** How long [startServer] waits for the detached server to bind before giving up. */
    private const val START_TIMEOUT_MS = 3_000L
    private const val START_POLL_MS = 200L

    /**
     * Matches the frida-server process by the absolute path we deployed it to,
     * so we never accidentally match the `.log` file or an unrelated process.
     * The pattern is anchored to the executable so the short-lived launcher shell
     * (whose command line also contains the log path) is excluded.
     */
    private fun serverPattern(ctx: Context): String = "^${BinaryManager.serverPath(ctx)} "

    /** True if a frida-server process started from our deployed binary is alive. */
    suspend fun isServerRunning(ctx: Context): Boolean {
        val res = RootManager.exec("pgrep -f '${serverPattern(ctx)}'")
        return res.isSuccess && res.out.any { it.isNotBlank() }
    }

    /**
     * Puts SELinux in permissive mode (required for Frida on Android 12+) and
     * launches frida-server detached so it survives the issuing shell.
     *
     * @return true once the server is confirmed listening, false if it never came up.
     */
    suspend fun startServer(ctx: Context, onLine: (String) -> Unit): Boolean {
        val server = BinaryManager.serverPath(ctx)
        onLine("[*] setenforce 0 (SELinux permissive)")
        RootManager.exec("setenforce 0")

        if (isServerRunning(ctx)) {
            onLine("[=] frida-server already running on $endpoint")
            return true
        }

        val log = "${ctx.filesDir}/frida-server.log"
        onLine("[*] starting $server -l $endpoint")
        // setsid + & fully detaches; stdout/stderr go to a log file we can tail.
        RootManager.exec("setsid $server -l $endpoint > $log 2>&1 &")

        // The server binds asynchronously; poll instead of checking once immediately.
        var waited = 0L
        while (waited < START_TIMEOUT_MS) {
            if (isServerRunning(ctx)) {
                onLine("[+] frida-server up on $endpoint")
                return true
            }
            delay(START_POLL_MS)
            waited += START_POLL_MS
        }

        onLine("[!] frida-server failed to start")
        tailLog(log).forEach { onLine("    $it") }
        return false
    }

    /** Returns the last few lines of the server log, for surfacing startup errors. */
    private suspend fun tailLog(logPath: String, lines: Int = 5): List<String> {
        val res = RootManager.exec("tail -n $lines $logPath")
        return res.out.filter { it.isNotBlank() }.ifEmpty { listOf("(log empty — $logPath)") }
    }

    /** Kills the frida-server instance we started. */
    suspend fun stopServer(ctx: Context, onLine: (String) -> Unit) {
        onLine("[*] stopping frida-server")
        val res = RootManager.exec("pkill -f '${serverPattern(ctx)}'")
        onLine(if (res.isSuccess) "[+] stopped" else "[=] no running frida-server found")
    }

    /**
     * Spawns [pkg] under instrumentation and loads [scriptPath] into it via
     * frida-inject, streaming the client's output line-by-line to [onLine].
     * Blocks for the lifetime of the injected session.
     */
    suspend fun inject(
        ctx: Context,
        pkg: String,
        scriptPath: String,
        onLine: (String) -> Unit,
    ) {
        val inject = BinaryManager.injectPath(ctx)
        onLine("[*] inject -> $pkg  (script: ${scriptPath.substringAfterLast('/')})")
        // Kill any stale client left from a previous tap so sessions never stack.
        RootManager.exec("pkill -f '^$inject '")
        // Dedicated shell: frida-inject occupies its shell for the whole session
        // and libsu serializes jobs per shell — sharing the main one froze every
        // other root command (and the UI) while a session was live.
        val shell = RootManager.newShell()
        try {
            // frida-inject 17.x has no host flag: it talks to its own local device
            // as root, spawning the target (-f) and loading the agent (-s).
            RootManager.execStreaming(
                shell,
                "$inject -f $pkg -s $scriptPath",
                onLine = onLine,
            )
            onLine("[=] inject session ended for $pkg")
        } finally {
            runCatching { shell.close() }
        }
    }

    /**
     * Kills the live frida-inject client, if any. pkill runs on the main shell,
     * which stays free because inject never touches it; the blocked stream on the
     * dedicated shell returns as soon as the process dies.
     *
     * SIGTERM first so a healthy session tears down cleanly (measured ~1s), then
     * SIGKILL after a grace period: frida-inject's graceful shutdown has been
     * observed to wedge for ~60s when the server was bounced mid-session, and
     * Stop must stay deterministic. Only the originally observed PIDs are killed,
     * so a session started right after Stop can't be hit by the escalation.
     */
    suspend fun stopInject(ctx: Context, onLine: (String) -> Unit) {
        val pattern = "^${BinaryManager.injectPath(ctx)} "
        val pids = RootManager.exec("pgrep -f '$pattern'").out.filter { it.isNotBlank() }
        if (pids.isEmpty()) {
            onLine("[=] no running inject client")
            return
        }
        val pidList = pids.joinToString(" ")
        RootManager.exec("kill -TERM $pidList")
        delay(1_500)
        RootManager.exec("kill -KILL $pidList 2>/dev/null")
        onLine("[*] inject client killed")
    }
}
