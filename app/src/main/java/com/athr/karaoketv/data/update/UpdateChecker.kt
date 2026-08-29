package com.athr.karaoketv.data.update

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Looks up the newest release on GitHub.
 *
 * The app is sideloaded onto a TV box, so there is no store to push updates. A
 * release published with the APK attached is the whole distribution channel, and
 * this is the other half of it.
 */
object UpdateChecker {

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/tuchung95/karaoke-tv/releases/latest"

    data class Release(
        val versionName: String,
        val notes: String,
        val apkUrl: String,
        val sizeBytes: Long,
    )

    suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val json = JSONObject(connection.inputStream.bufferedReader().readText())
            val assets = json.optJSONArray("assets") ?: return@withContext null

            var apkUrl: String? = null
            var size = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    size = asset.optLong("size")
                    break
                }
            }
            val url = apkUrl?.takeIf { it.isNotBlank() } ?: return@withContext null

            Release(
                versionName = json.optString("tag_name").removePrefix("v").trim(),
                notes = json.optString("body").trim(),
                apkUrl = url,
                sizeBytes = size,
            )
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Compares dotted versions numerically, so 1.10 correctly beats 1.9 — a string
     * comparison would get that backwards and silently stop offering updates.
     */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
