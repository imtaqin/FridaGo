package com.imtaqin.fridago.core

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ensures the frida binaries exist in the app's private files dir.
 *
 * If the APK bundled `assets/bin/<abi>/<name>` that is used directly; otherwise
 * the binary is fetched from the official GitHub release (`.xz` asset) and
 * decompressed in-process. Downloads land in `<name>.part` and are renamed last
 * so an interrupted fetch never leaves a half-written binary behind.
 */
object BinaryManager {

    const val FRIDA_SERVER = "frida-server"
    const val FRIDA_INJECT = "frida-inject"

    /** Pinned release — server and inject must be exactly the same version. */
    const val FRIDA_VERSION = "17.22.0"

    private const val RELEASES =
        "https://github.com/frida/frida/releases/download/$FRIDA_VERSION"

    /** The device ABI we will try to load binaries for, e.g. "arm64-v8a". */
    val primaryAbi: String
        get() = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

    /** Frida release assets are named by CPU arch, not Android ABI. */
    private val fridaArch: String
        get() = when (primaryAbi) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a", "armeabi" -> "arm"
            "x86_64" -> "x86_64"
            "x86" -> "x86"
            else -> "arm64"
        }

    private fun targetFile(ctx: Context, name: String) = File(ctx.filesDir, name)

    fun serverPath(ctx: Context): String = targetFile(ctx, FRIDA_SERVER).absolutePath
    fun injectPath(ctx: Context): String = targetFile(ctx, FRIDA_INJECT).absolutePath

    fun isDeployed(ctx: Context): Boolean =
        targetFile(ctx, FRIDA_SERVER).exists() && targetFile(ctx, FRIDA_INJECT).exists()

    /** True if the APK actually shipped a binary for this device's ABI. */
    fun isBundled(ctx: Context, name: String): Boolean = runCatching {
        ctx.assets.open("bin/$primaryAbi/$name").use { it.close() }
        true
    }.getOrDefault(false)

    /**
     * Brings both binaries on disk, downloading whichever are missing.
     * No-op when already deployed, so repeated Start taps are free.
     * Every line of progress goes to [onLine] (the app log).
     */
    suspend fun ensureBinaries(ctx: Context, onLine: (String) -> Unit): Unit =
        withContext(Dispatchers.IO) {
            if (isDeployed(ctx)) {
                onLine("[=] frida $FRIDA_VERSION binaries already on device")
                return@withContext
            }
            for (name in listOf(FRIDA_SERVER, FRIDA_INJECT)) {
                if (targetFile(ctx, name).exists()) continue
                if (isBundled(ctx, name)) {
                    onLine("[*] extracting bundled $name")
                    ctx.assets.open("bin/$primaryAbi/$name").use { input ->
                        targetFile(ctx, name).outputStream().use { input.copyTo(it) }
                    }
                } else {
                    download(ctx, name, onLine)
                }
                val out = targetFile(ctx, name)
                out.setExecutable(true, false)
                // Private app storage is noexec on some ROMs; chmod via root is the reliable path.
                RootManager.exec("chmod 755 ${out.absolutePath}")
                onLine("[+] $name ready (${out.length() / 1024 / 1024} MB)")
            }
        }

    private fun download(ctx: Context, name: String, onLine: (String) -> Unit) {
        val asset = "$name-$FRIDA_VERSION-android-$fridaArch.xz"
        onLine("[*] downloading $asset (~18 MB)")
        val part = File(ctx.filesDir, "$name.part")
        val conn = (URL("$RELEASES/$asset").openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Corvo-Frida")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code for $asset")
            conn.inputStream.use { input ->
                XZInputStream(input).use { xz ->
                    part.outputStream().use { out -> xz.copyTo(out) }
                }
            }
        } finally {
            conn.disconnect()
        }
        val out = targetFile(ctx, name)
        if (!part.renameTo(out)) throw IOException("could not move $asset into place")
    }
}
