package com.imtaqin.fridago.core

import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin coroutine-friendly wrapper over libsu. All calls run on [Dispatchers.IO]
 * because the underlying `su` pipe blocks.
 */
object RootManager {

    /** @return true if a root shell is available on this device. */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        Shell.getShell().isRoot
    }

    /**
     * Re-probes root even if an earlier check cached a non-root shell (the user
     * may have granted su after the first prompt). Drops the stale shell so the
     * next [Shell.getShell] rebuilds it and re-requests su.
     */
    suspend fun isRootAvailableFresh(): Boolean = withContext(Dispatchers.IO) {
        val cached = Shell.getCachedShell()
        if (cached != null && !cached.isRoot) cached.close()
        runCatching { Shell.getShell().isRoot }.getOrDefault(false)
    }

    /** Runs [cmd] to completion and returns the combined result. */
    suspend fun exec(vararg cmd: String): Shell.Result = withContext(Dispatchers.IO) {
        Shell.cmd(*cmd).exec()
    }

    /**
     * Builds an independent root shell. Long-lived commands (frida-inject runs
     * for the whole session) must use their own shell: libsu serializes jobs per
     * shell, so parking one on the main shell blocks every later exec — that was
     * the inject hang.
     */
    suspend fun newShell(): Shell = withContext(Dispatchers.IO) {
        Shell.Builder.create()
            .setFlags(Shell.FLAG_MOUNT_MASTER)
            .setTimeout(20)
            .build()
    }

    /**
     * Runs [cmd] on [shell], streaming every stdout/stderr line to [onLine] as it
     * arrives. Blocks until the command exits, so call this from a background
     * coroutine.
     */
    suspend fun execStreaming(
        shell: Shell,
        vararg cmd: String,
        onLine: (String) -> Unit,
    ): Shell.Result = withContext(Dispatchers.IO) {
        val sink = object : CallbackList<String>() {
            override fun onAddElement(line: String) = onLine(line)
        }
        shell.newJob().add(*cmd).to(sink, sink).exec()
    }
}
