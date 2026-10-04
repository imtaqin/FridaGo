package com.imtaqin.fridago.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Where an imported agent script comes from. */
enum class ScriptSource(val label: String, val hint: String) {
    CodeShare("CodeShare", "user/project"),
    GitHub("GitHub", "https://github.com/…/agent.js"),
    Gist("Gist", "https://gist.github.com/user/<id>"),
}

/**
 * Downloads Frida agent scripts from public sources and returns the raw source
 * so [ScriptStore] can persist it. All network work runs on [Dispatchers.IO].
 *
 *  - [ScriptSource.CodeShare]: a `user/project` slug, resolved via the public
 *    `codeshare.frida.re` project API (the same endpoint `frida --codeshare` uses).
 *  - [ScriptSource.GitHub]: a github.com blob URL (auto-rewritten to raw) or a
 *    raw.githubusercontent.com URL.
 *  - [ScriptSource.Gist]: a gist URL/id (resolved via the GitHub Gists API) or a
 *    gist.githubusercontent.com raw URL.
 */
object ScriptFetcher {

    data class FetchedScript(val name: String, val body: String)

    /** One CodeShare search hit, parsed from the /search/ HTML. */
    data class CodeShareHit(
        val owner: String,
        val slug: String,
        val title: String,
        val description: String,
        val likes: String,
        val views: String,
    ) {
        val ref: String get() = "$owner/$slug"
    }

    private const val UA = "Corvo-Frida/1.0"
    private const val TIMEOUT_MS = 15_000

    suspend fun fetch(source: ScriptSource, input: String): FetchedScript =
        withContext(Dispatchers.IO) {
            val value = input.trim()
            require(value.isNotEmpty()) { "empty reference" }
            when (source) {
                ScriptSource.CodeShare -> codeshare(value)
                ScriptSource.GitHub -> github(value)
                ScriptSource.Gist -> gist(value)
            }
        }

    /**
     * Keyword search across CodeShare. Parses the public /search/ page — the
     * site has no JSON search API — into [CodeShareHit]s, capped at 30.
     */
    suspend fun searchCodeShare(query: String): List<CodeShareHit> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            require(q.isNotEmpty()) { "empty query" }
            val html = httpGet(
                "https://codeshare.frida.re/search/?query=" +
                    java.net.URLEncoder.encode(q, "UTF-8"),
            )
            ARTICLE.split(html).drop(1).mapNotNull { block ->
                val link = HIT_LINK.find(block) ?: return@mapNotNull null
                val (owner, slug, title) = link.destructured
                val desc = HIT_DESC.find(block)?.groupValues?.get(1)
                    ?.replace(Regex("<[^>]+>"), "")
                    ?.trim()
                    .orEmpty()
                CodeShareHit(
                    owner = owner,
                    slug = slug,
                    title = title.htmlDecode(),
                    description = desc.htmlDecode(),
                    likes = HIT_LIKES.find(block)?.groupValues?.get(1) ?: "0",
                    views = HIT_VIEWS.find(block)?.groupValues?.get(1) ?: "0",
                )
            }.take(30)
        }

    private val ARTICLE = Regex("<article>")
    private val HIT_LINK = Regex(
        """<h2><a href="https://codeshare\.frida\.re/@([^/"]+)/([^/"]+)/?[^"]*">([^<]+)</a></h2>""",
    )
    private val HIT_DESC = Regex("<p>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
    private val HIT_LIKES = Regex("""fa-thumbs-o-up[^>]*></i>\s*([\d.KM]+)""")
    private val HIT_VIEWS = Regex("""fa-eye[^>]*></i>\s*([\d.KM]+)""")

    private fun String.htmlDecode(): String = replace(Regex("&amp;"), "&")
        .replace(Regex("&lt;"), "<").replace(Regex("&gt;"), ">")
        .replace(Regex("&quot;"), "\"").replace(Regex("&#x27;|&#39;"), "'")

    // ---- sources ------------------------------------------------------------

    private fun codeshare(input: String): FetchedScript {
        val slug = input.removePrefix("@").trim('/')
        require(slug.count { it == '/' } == 1) { "expected 'user/project'" }
        val json = JSONObject(httpGet("https://codeshare.frida.re/api/project/$slug/"))
        val source = json.optString("source").ifBlank { null }
            ?: json.optJSONObject("project")?.optString("source")?.ifBlank { null }
            ?: throw IOException("no 'source' field in CodeShare response")
        return FetchedScript(ensureJs(slug.substringAfterLast('/')), source)
    }

    private fun github(input: String): FetchedScript {
        var url = input.substringBefore('#')
        if (url.contains("github.com/") && url.contains("/blob/")) {
            url = url.replace("https://github.com/", "https://raw.githubusercontent.com/")
                .replaceFirst("/blob/", "/")
        }
        val body = httpGet(url)
        val name = url.substringBefore('?').substringAfterLast('/').ifBlank { "github-agent" }
        return FetchedScript(ensureJs(name), body)
    }

    private fun gist(input: String): FetchedScript {
        val clean = input.substringBefore('#')
        if (clean.contains("gist.githubusercontent.com")) {
            val body = httpGet(clean.substringBefore('?'))
            val name = clean.substringBefore('?').substringAfterLast('/').ifBlank { "gist" }
            return FetchedScript(ensureJs(name), body)
        }
        val path = clean.substringBefore('?').trimEnd('/')
        val id = path.split('/').lastOrNull { it.matches(Regex("[0-9a-fA-F]{20,}")) }
            ?: path.substringAfterLast('/')
        require(id.isNotBlank()) { "could not find a gist id" }
        val json = JSONObject(
            httpGet(
                "https://api.github.com/gists/$id",
                mapOf("Accept" to "application/vnd.github+json"),
            )
        )
        val files = json.getJSONObject("files")
        val keys = files.keys().asSequence().toList()
        val key = keys.firstOrNull { it.endsWith(".js") }
            ?: keys.firstOrNull()
            ?: throw IOException("gist has no files")
        val content = files.getJSONObject(key).getString("content")
        return FetchedScript(ensureJs(key), content)
    }

    // ---- http ---------------------------------------------------------------

    private fun httpGet(urlStr: String, headers: Map<String, String> = emptyMap()): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", UA)
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("HTTP $code${text.take(160).prependIfNotBlank()}")
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun String.prependIfNotBlank() = if (isBlank()) "" else " — ${trim()}"

    private fun ensureJs(name: String): String {
        val base = name.trim().ifBlank { "agent" }
        return if (base.endsWith(".js")) base else "$base.js"
    }
}
