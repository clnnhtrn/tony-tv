package app.tony.data

import android.content.Context
import app.tony.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Downloads Tony's lists from the public GitHub repo.
 * Safety (CLAUDE.md 5a): list files are untrusted input. HTTPS only, size-capped, parsed strictly
 * into known shapes (unknown fields ignored), and the last good copy is kept on disk so a bad or
 * missing download never empties the TV.
 */
class Repository(private val context: Context) {
    val http: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http"), 30L * 1024 * 1024))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = false; coerceInputValues = true }
    private val lastGood = File(context.filesDir, "lists").apply { mkdirs() }

    private suspend fun <T> load(path: String, strategy: DeserializationStrategy<T>, maxBytes: Long): Result<T> =
        withContext(Dispatchers.IO) {
            require(!path.contains("..") && !path.startsWith("/")) { "bad path" }
            val url = BuildConfig.DATA_BASE + path
            require(url.startsWith("https://")) { "lists must come over HTTPS" }
            val cacheFile = File(lastGood, path.replace('/', '_'))
            val fresh = runCatching {
                http.newCall(Request.Builder().url(url).header("User-Agent", "Tony/1.0").build()).execute().use { resp ->
                    check(resp.isSuccessful) { "HTTP ${resp.code}" }
                    val body = resp.body
                    val len = body.contentLength()
                    check(len < 0 || len <= maxBytes) { "too large" }
                    val bytes = body.byteStream().readNBytesCapped(maxBytes)
                    val text = bytes.toString(Charsets.UTF_8)
                    val parsed = json.decodeFromString(strategy, text)   // validate before saving
                    cacheFile.writeText(text)
                    parsed
                }
            }
            if (fresh.isSuccess) fresh
            else runCatching { json.decodeFromString(strategy, cacheFile.readText()) }
                .recoverCatching { throw fresh.exceptionOrNull() ?: it }
        }

    suspend fun index() = load("dist/index.json", Index.serializer(), 2_000_000)
    suspend fun channels(file: String) = load("dist/$file", ChannelFile.serializer(), 8_000_000)

    companion object {
        fun logoUrl(c: Channel): String? = c.logo?.takeIf { !it.contains("..") }?.let { BuildConfig.DATA_BASE + it }
        /** Stills are refreshed every few hours: the time-slot query makes the TV fetch the new frame
         *  (and steps around CDN-cached 404s from before a still existed). */
        fun stillUrl(c: Channel): String =
            BuildConfig.STILLS_BASE + "stills/" + c.id + ".jpg?t=" + (System.currentTimeMillis() / (3L * 3600 * 1000))
    }
}

private fun java.io.InputStream.readNBytesCapped(max: Long): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(16 * 1024)
    var total = 0L
    while (true) {
        val n = read(buf)
        if (n < 0) break
        total += n
        check(total <= max) { "too large" }
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}
