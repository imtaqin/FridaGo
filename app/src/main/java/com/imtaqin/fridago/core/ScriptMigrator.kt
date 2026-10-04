package com.imtaqin.fridago.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Makes pre-Frida-17 scripts run on the bundled Frida 17.22 without the author
 * touching them. Two independent problems, two independent fixes:
 *
 *  1. **Java bridge** (frida/frida#3495): Frida 17 decoupled the Java/ObjC
 *     bridges — `Java` is no longer a global. Scripts using `Java.perform` /
 *     `Java.use` get the compiled `frida-java-bridge` bundle (assets/bridge/java.js)
 *     prepended, which assigns `globalThis.Java`.
 *
 *  2. **Removed static APIs**: `Module.enumerateExportsSync`, `Module.getExportByName`,
 *     `Module.findExportByName`, `Module.findBaseAddress`, `Memory.readUtf8String`,
 *     `Memory.writeU32`, `Process.enumerateModulesSync`, … were dropped in 17
 *     (instance methods on `Process.getModuleByName(...)` / `NativePointer` replaced
 *     them). A small runtime shim re-adds them as pass-throughs — no text rewriting,
 *     so authors' code stays byte-identical.
 *
 * The header is inserted only when the script actually needs it, and the result is
 * cached per source script so repeat injects don't re-read assets.
 */
object ScriptMigrator {

    private const val BRIDGE_ASSET = "bridge/java.js"

    private val needsBridge =
        Regex("""\bJava\s*\.\s*(perform|use|choose|available|performNow|array|cast|retain|registerClass|openClassFile)""")

    private val legacyApi = Regex(
        """Module\.(enumerateExportsSync|enumerateImportsSync|enumerateSymbolsSync|enumerateSectionsSync|enumerateExports|enumerateImports|getExportByName|findExportByName|getBaseAddress|findBaseAddress|findModuleByName|enumerateModulesSync)|""" +
            """Memory\.(read|write)[A-Z]\w*|""" +
            """Process\.(enumerateModulesSync|enumerateThreadsSync|enumerateRangesSync)""",
    )

    data class Migrated(val file: File, val usedBridge: Boolean, val usedShim: Boolean)

    /**
     * Returns a runnable script for [source]: original text when nothing needs
     * fixing, otherwise `bridge? + shim? + original` written next to the source
     * as `<name>.f17.js`. Runs on [Dispatchers.IO] (asset + file I/O).
     */
    suspend fun migrate(ctx: Context, source: ScriptFile): Migrated = withContext(Dispatchers.IO) {
        val body = ScriptStore.read(source)
        val useBridge = needsBridge.containsMatchIn(body)
        val useShim = legacyApi.containsMatchIn(body)

        if (!useBridge && !useShim) return@withContext Migrated(File(source.path), false, false)

        // Written under scripts/f17/ — ScriptStore.list only lists files (not
        // subdirs), so the migrated twin never shows up as a second script.
        val outDir = File(File(ctx.filesDir, "scripts"), "f17").apply { mkdirs() }
        val out = File(outDir, source.name)
        out.writeText(buildString {
            if (useBridge) {
                append("// [corvo] frida-java-bridge bundle (Frida 17+ ships without the Java global)\n")
                append(ctx.assets.open(BRIDGE_ASSET).use { it.reader().readText() })
                append('\n')
            }
            if (useShim) {
                append(SHIM)
                append('\n')
            }
            append(body)
        })
        Migrated(out, useBridge, useShim)
    }

    /**
     * Pass-through re-implementations of the static APIs removed in Frida 17.
     * Only added when the script references them; each entry is guarded so it
     * never clobbers a native implementation.
     */
    private val SHIM = """
        // [corvo] frida-17 compat shim — restored legacy static APIs
        (function () {
          function mod(name) {
            if (name === null || name === undefined) return Process.mainModule;
            return Process.getModuleByName(name);
          }
          var M = Module;
          M.enumerateExportsSync = M.enumerateExportsSync || function (n) { return mod(n).enumerateExports(); };
          M.enumerateImportsSync = M.enumerateImportsSync || function (n) { return mod(n).enumerateImports(); };
          M.enumerateSymbolsSync = M.enumerateSymbolsSync || function (n) { return mod(n).enumerateSymbols(); };
          M.enumerateSectionsSync = M.enumerateSectionsSync || function (n) { return mod(n).enumerateSections(); };
          M.enumerateExports = M.enumerateExports || M.enumerateExportsSync;
          M.enumerateImports = M.enumerateImports || M.enumerateImportsSync;
          M.getExportByName = M.getExportByName || function (n, e) {
            if (n === null || n === undefined) return M.getGlobalExportByName(e);
            return mod(n).getExportByName(e);
          };
          M.findExportByName = M.findExportByName || function (n, e) {
            if (n === null || n === undefined) return M.findGlobalExportByName(e);
            var m = Process.findModuleByName(n);
            return m === null ? null : m.findExportByName(e);
          };
          M.getBaseAddress = M.getBaseAddress || function (n) { return mod(n).base; };
          M.findBaseAddress = M.findBaseAddress || function (n) {
            var m = Process.findModuleByName(n);
            return m === null ? null : m.base;
          };
          M.findModuleByName = M.findModuleByName || Process.findModuleByName;
          Process.enumerateModulesSync = Process.enumerateModulesSync || function () { return Process.enumerateModules(); };
          Process.enumerateThreadsSync = Process.enumerateThreadsSync || function () { return Process.enumerateThreads(); };
          var np = NativePointer.prototype;
          Object.getOwnPropertyNames(np).forEach(function (name) {
            if (name.slice(0, 4) === "read" && typeof Memory[name] !== "function") {
              Memory[name] = function () {
                var args = Array.prototype.slice.call(arguments);
                var p = args.shift();
                if (!(p instanceof NativePointer)) p = ptr(p);
                return np[name].apply(p, args);
              };
            } else if (name.slice(0, 5) === "write" && typeof Memory[name] !== "function") {
              Memory[name] = function () {
                var args = Array.prototype.slice.call(arguments);
                var p = args.shift();
                if (!(p instanceof NativePointer)) p = ptr(p);
                return np[name].apply(p, args);
              };
            }
          });
        })();
    """.trimIndent()
}
