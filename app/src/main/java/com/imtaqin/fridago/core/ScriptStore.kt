package com.imtaqin.fridago.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A Frida agent script on disk. */
data class ScriptFile(val name: String, val path: String)

/**
 * Manages `.js` agent scripts under `filesDir/scripts`. On first run it seeds
 * the bundled example from assets so the user has something to inject immediately.
 */
object ScriptStore {

    private fun dir(ctx: Context): File =
        File(ctx.filesDir, "scripts").apply { mkdirs() }

    suspend fun seedDefaults(ctx: Context) = withContext(Dispatchers.IO) {
        val example = File(dir(ctx), "example.js")
        if (!example.exists()) {
            runCatching {
                ctx.assets.open("scripts/example.js").use { input ->
                    example.outputStream().use { input.copyTo(it) }
                }
            }
        }
    }

    suspend fun list(ctx: Context): List<ScriptFile> = withContext(Dispatchers.IO) {
        dir(ctx).listFiles { f -> f.isFile && f.extension == "js" }
            ?.sortedBy { it.name.lowercase() }
            ?.map { ScriptFile(it.name, it.absolutePath) }
            ?: emptyList()
    }

    suspend fun read(script: ScriptFile): String = withContext(Dispatchers.IO) {
        File(script.path).takeIf { it.exists() }?.readText().orEmpty()
    }

    /**
     * Creates or overwrites a script, returning its handle. The name is reduced to a
     * bare file name so a value like `../../evil` can't write outside the scripts dir.
     */
    suspend fun save(ctx: Context, name: String, body: String): ScriptFile =
        withContext(Dispatchers.IO) {
            val base = File(name.trim()).name          // strips any path components
                .ifBlank { "agent" }
            val safe = if (base.endsWith(".js")) base else "$base.js"
            val file = File(dir(ctx), safe)
            file.writeText(body)
            ScriptFile(file.name, file.absolutePath)
        }

    suspend fun delete(script: ScriptFile) = withContext(Dispatchers.IO) {
        File(script.path).delete()
    }
}
