package com.athr.karaoketv.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

class AppPrefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("karaoke_prefs", Context.MODE_PRIVATE)

    var libraryUri: String?
        get() = sp.getString(KEY_LIBRARY_URI, null)
        set(value) = sp.edit { putString(KEY_LIBRARY_URI, value) }

    /** "saf" for a granted document tree, "direct" for a plain filesystem path. */
    var librarySourceKind: String
        get() = sp.getString(KEY_SOURCE_KIND, SOURCE_SAF) ?: SOURCE_SAF
        set(value) = sp.edit { putString(KEY_SOURCE_KIND, value) }

    var libraryLabel: String
        get() = sp.getString(KEY_LIBRARY_LABEL, "") ?: ""
        set(value) = sp.edit { putString(KEY_LIBRARY_LABEL, value) }

    var lastScanAt: Long
        get() = sp.getLong(KEY_LAST_SCAN, 0L)
        set(value) = sp.edit { putLong(KEY_LAST_SCAN, value) }

    /** Auto-advance to the next queued song when one finishes. */
    var autoNext: Boolean
        get() = sp.getBoolean(KEY_AUTO_NEXT, true)
        set(value) = sp.edit { putBoolean(KEY_AUTO_NEXT, value) }

    /** Flash "Bài tiếp theo: …" over the video when a song is about to end. */
    var showNextUpBanner: Boolean
        get() = sp.getBoolean(KEY_NEXT_BANNER, true)
        set(value) = sp.edit { putBoolean(KEY_NEXT_BANNER, value) }

    /** 0 = fit, 1 = fill (crop), 2 = stretch. Old 4:3 rips need this on a 16:9 TV. */
    var videoScaleMode: Int
        get() = sp.getInt(KEY_SCALE_MODE, 0)
        set(value) = sp.edit { putInt(KEY_SCALE_MODE, value) }

    /** Semitones of pitch shift carried between songs, -6..+6. */
    var pitchSemitones: Int
        get() = sp.getInt(KEY_PITCH, 0)
        set(value) = sp.edit { putInt(KEY_PITCH, value.coerceIn(-6, 6)) }

    /** Append "karaoke" to a YouTube search, since that is how the rips are titled. */
    var appendKaraokeToYouTube: Boolean
        get() = sp.getBoolean(KEY_YT_KEYWORD, true)
        set(value) = sp.edit { putBoolean(KEY_YT_KEYWORD, value) }

    /**
     * Home shelves in display order. Unknown keys are dropped and shelves added in
     * a later version are appended, so an old saved order never hides a new row.
     */
    var homeShelfOrder: List<HomeShelf>
        get() {
            val saved = sp.getString(KEY_SHELF_ORDER, null)
                ?.split(',')
                ?.mapNotNull { HomeShelf.fromKey(it.trim()) }
                .orEmpty()
            return saved + HomeShelf.DEFAULT_ORDER.filterNot { it in saved }
        }
        set(value) = sp.edit { putString(KEY_SHELF_ORDER, value.joinToString(",") { it.key }) }

    var hiddenShelves: Set<HomeShelf>
        get() = sp.getStringSet(KEY_SHELF_HIDDEN, emptySet())
            .orEmpty()
            .mapNotNull { HomeShelf.fromKey(it) }
            .toSet()
        set(value) = sp.edit { putStringSet(KEY_SHELF_HIDDEN, value.map { it.key }.toSet()) }

    fun setShelfVisible(shelf: HomeShelf, visible: Boolean) {
        hiddenShelves = if (visible) hiddenShelves - shelf else hiddenShelves + shelf
    }

    fun moveShelf(shelf: HomeShelf, delta: Int) {
        val order = homeShelfOrder.toMutableList()
        val from = order.indexOf(shelf)
        if (from < 0) return
        val to = (from + delta).coerceIn(0, order.lastIndex)
        if (to == from) return
        order.add(to, order.removeAt(from))
        homeShelfOrder = order
    }

    fun resetHomeLayout() {
        homeShelfOrder = HomeShelf.DEFAULT_ORDER
        hiddenShelves = emptySet()
    }

    val libraryConfigured: Boolean get() = !libraryUri.isNullOrBlank()

    fun libraryUriFlow(): Flow<String?> = keyFlow(KEY_LIBRARY_URI).map { libraryUri }

    private fun keyFlow(key: String): Flow<Unit> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) trySend(Unit)
        }
        sp.registerOnSharedPreferenceChangeListener(listener)
        trySend(Unit)
        awaitClose { sp.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        const val SOURCE_SAF = "saf"
        const val SOURCE_DIRECT = "direct"
        const val SOURCE_MEDIA = "media"

        private const val KEY_LIBRARY_URI = "library_uri"
        private const val KEY_SOURCE_KIND = "library_source_kind"
        private const val KEY_LIBRARY_LABEL = "library_label"
        private const val KEY_LAST_SCAN = "last_scan_at"
        private const val KEY_AUTO_NEXT = "auto_next"
        private const val KEY_NEXT_BANNER = "next_up_banner"
        private const val KEY_SCALE_MODE = "video_scale_mode"
        private const val KEY_PITCH = "pitch_semitones"
        private const val KEY_YT_KEYWORD = "youtube_append_karaoke"
        private const val KEY_SHELF_ORDER = "home_shelf_order"
        private const val KEY_SHELF_HIDDEN = "home_shelf_hidden"
    }
}
