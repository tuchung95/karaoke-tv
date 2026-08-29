package com.athr.karaoketv.data.youtube

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/**
 * Hands a search off to the YouTube app installed on the box.
 *
 * Playback deliberately stays in YouTube's own player rather than being pulled
 * into this app's ExoPlayer. That is what YouTube's terms require, and it is also
 * the only way a Premium subscription keeps working: ad-free is enforced by the
 * official player against the signed-in account, so a stream played anywhere else
 * would carry ads even for a paying subscriber.
 *
 * The trade-off is real and worth knowing: songs opened this way sit outside our
 * transport, so pitch shift, vocal-channel removal and the queue do not apply to
 * them. Those belong to files on the drive.
 */
object YouTubeLauncher {

    /** Ordered by preference: the TV client first, then the phone/tablet ones. */
    private val KNOWN_PACKAGES = listOf(
        "com.google.android.youtube.tv",
        "com.google.android.youtube.tvunplugged",
        "com.google.android.apps.youtube.unplugged",
        "com.google.android.youtube",
    )

    fun installedPackage(context: Context): String? {
        val pm = context.packageManager
        return KNOWN_PACKAGES.firstOrNull { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        }
    }

    fun isAvailable(context: Context): Boolean = installedPackage(context) != null

    /**
     * Opens YouTube on the results for [query]. Lets the system pick a handler when
     * no known package is installed, and reports false when nothing can open it.
     */
    fun openSearch(context: Context, query: String, appendKaraoke: Boolean): Boolean {
        val terms = buildQuery(query, appendKaraoke)
        if (terms.isBlank()) return false

        val uri = Uri.parse(
            "https://www.youtube.com/results?search_query=" +
                URLEncoder.encode(terms, "UTF-8")
        )
        val target = installedPackage(context)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (target != null) setPackage(target)
        }
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    /** Most rips are titled "<tên bài> karaoke", so the word is worth adding by default. */
    private fun buildQuery(query: String, appendKaraoke: Boolean): String {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return ""
        if (!appendKaraoke) return trimmed
        return if (trimmed.contains("karaoke", ignoreCase = true)) trimmed else "$trimmed karaoke"
    }
}
