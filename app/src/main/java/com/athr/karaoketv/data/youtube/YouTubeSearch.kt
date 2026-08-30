package com.athr.karaoketv.data.youtube

import com.athr.karaoketv.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** One YouTube result, trimmed to what a card on a TV actually shows. */
data class YouTubeVideo(
    val id: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String,
)

/**
 * Searches YouTube through the official Data API so results can be picked inside
 * this app, with the D-pad grid people already know, instead of dropping the room
 * into YouTube's own search page mid-party.
 *
 * Two filters matter more than anything else here. `videoEmbeddable` and
 * `videoSyndicated` keep out the videos the embedded player is not allowed to
 * play — without them a card looks identical to every other card and only reveals
 * itself as a dead end after someone has chosen it and the room is waiting.
 */
object YouTubeSearch {

    private const val ENDPOINT = "https://www.googleapis.com/youtube/v3/search"

    val configured: Boolean get() = BuildConfig.YOUTUBE_API_KEY.isNotBlank()

    suspend fun search(query: String, appendKaraoke: Boolean): Result<List<YouTubeVideo>> =
        withContext(Dispatchers.IO) {
            val terms = buildQuery(query, appendKaraoke)
            if (terms.isBlank()) return@withContext Result.success(emptyList())
            if (!configured) {
                return@withContext Result.failure(
                    IllegalStateException("Chưa có API key YouTube")
                )
            }

            val url = URL(
                ENDPOINT +
                    "?part=snippet" +
                    "&type=video" +
                    "&maxResults=24" +
                    "&videoEmbeddable=true" +
                    "&videoSyndicated=true" +
                    "&relevanceLanguage=vi" +
                    "&regionCode=VN" +
                    "&q=" + URLEncoder.encode(terms, "UTF-8") +
                    "&key=" + URLEncoder.encode(BuildConfig.YOUTUBE_API_KEY, "UTF-8")
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    return@withContext Result.failure(IllegalStateException(messageFor(code)))
                }
                val json = JSONObject(connection.inputStream.bufferedReader().readText())
                val items = json.optJSONArray("items")
                    ?: return@withContext Result.success(emptyList())

                val videos = buildList {
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
                        if (id.isBlank()) continue
                        val snippet = item.optJSONObject("snippet") ?: continue
                        add(
                            YouTubeVideo(
                                id = id,
                                title = unescape(snippet.optString("title")),
                                channel = unescape(snippet.optString("channelTitle")),
                                thumbnailUrl = snippet.optJSONObject("thumbnails")
                                    ?.optJSONObject("high")
                                    ?.optString("url")
                                    .orEmpty(),
                            )
                        )
                    }
                }
                Result.success(videos)
            } catch (e: Exception) {
                Result.failure(IllegalStateException("Không tìm được — kiểm tra mạng của box"))
            } finally {
                connection.disconnect()
            }
        }

    /**
     * The daily quota is the failure people will actually hit, and "403" tells a
     * room full of guests nothing. Say what ran out and what still works.
     */
    private fun messageFor(code: Int): String = when (code) {
        403 -> "Hết lượt tìm YouTube hôm nay. Vẫn mở được bằng app YouTube."
        400 -> "API key YouTube không dùng được. Kiểm tra youtubeApiKey trong local.properties."
        else -> "YouTube không trả lời (lỗi $code)."
    }

    /** Titles come back with HTML entities in them; a karaoke title full of &amp; reads badly. */
    private fun unescape(text: String): String = text
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    /** Shared so the keyless player route searches for exactly the same words. */
    fun buildQuery(query: String, appendKaraoke: Boolean): String {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return ""
        if (!appendKaraoke) return trimmed
        return if (trimmed.contains("karaoke", ignoreCase = true)) trimmed else "$trimmed karaoke"
    }
}
