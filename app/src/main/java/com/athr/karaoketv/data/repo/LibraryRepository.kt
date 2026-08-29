package com.athr.karaoketv.data.repo

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.athr.karaoketv.data.db.KaraokeDatabase
import com.athr.karaoketv.data.db.LibraryGroup
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.data.library.LibraryScanner
import com.athr.karaoketv.data.library.LibrarySource
import com.athr.karaoketv.data.library.ScanProgress
import com.athr.karaoketv.data.prefs.AppPrefs
import com.athr.karaoketv.util.TextNormalizer
import kotlinx.coroutines.flow.Flow

class LibraryRepository(
    private val context: Context,
    val prefs: AppPrefs,
) {

    private val dao = KaraokeDatabase.get(context).songDao()
    private val scanner = LibraryScanner(context, dao)

    val songCount: Flow<Int> = dao.countFlow()
    val recentlyPlayed: Flow<List<SongEntity>> = dao.recentlyPlayed()
    val favorites: Flow<List<SongEntity>> = dao.favorites()
    val mostPlayed: Flow<List<SongEntity>> = dao.mostPlayed()
    val recentlyAdded: Flow<List<SongEntity>> = dao.recentlyAdded()
    val categories: Flow<List<LibraryGroup>> = dao.categories()
    val artists: Flow<List<LibraryGroup>> = dao.artists()

    suspend fun search(text: String): List<SongEntity> {
        val q = TextNormalizer.query(text)
        if (q.isBlank()) return emptyList()
        return dao.search(q = q, compact = q.replace(" ", ""), raw = text.trim())
    }

    suspend fun songsInCategory(name: String) = dao.songsInCategory(name)
    suspend fun songsInCollection(name: String) = dao.songsInCollection(name)
    suspend fun songsByArtist(name: String) = dao.songsByArtist(name)
    suspend fun collectionsIn(category: String) = dao.collectionsIn(category)
    suspend fun allSongs(limit: Int, offset: Int) = dao.allSongs(limit, offset)
    suspend fun random(limit: Int) = dao.random(limit)
    suspend fun byId(id: String) = dao.byId(id)

    suspend fun markPlayed(id: String) = dao.markPlayed(id, System.currentTimeMillis())
    /** A folder directly under some path, and how many songs sit anywhere beneath it. */
    data class FolderChild(val name: String, val path: String, val songCount: Int)

    /**
     * Walks the folder tree the way the drive is actually laid out.
     *
     * Built from the stored relative paths rather than a table of folders: a rip
     * collection's shape is whatever someone made on the drive, and it changes
     * every time they plug it into a laptop.
     */
    suspend fun folderChildren(path: String): List<FolderChild> {
        val counts = dao.folderCounts()
        val prefix = if (path.isEmpty()) "" else "$path/"
        val direct = linkedMapOf<String, Int>()
        for (row in counts) {
            val full = row.name
            if (full == path) continue
            if (prefix.isNotEmpty() && !full.startsWith(prefix)) continue
            val name = full.removePrefix(prefix).substringBefore('/')
            if (name.isBlank()) continue
            direct[name] = (direct[name] ?: 0) + row.songCount
        }
        return direct.entries
            .sortedBy { it.key.lowercase() }
            .map { (name, count) ->
                FolderChild(name, if (path.isEmpty()) name else "$path/$name", count)
            }
    }

    suspend fun songsDirectlyIn(path: String) = dao.songsDirectlyIn(path)

    suspend fun setFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite)

    /** Swaps a song's title and artist when the filename put them the other way round. */
    suspend fun swapTitleAndArtist(song: SongEntity) {
        val newTitle = song.artist?.takeIf { it.isNotBlank() } ?: return
        val newArtist = song.title
        val titleKey = TextNormalizer.key(newTitle)
        val artistKey = TextNormalizer.key(newArtist)
        dao.updateNaming(
            id = song.id,
            title = newTitle,
            titleKey = titleKey,
            titleCompact = titleKey.replace(" ", ""),
            titleInitials = TextNormalizer.initials(newTitle),
            artist = newArtist,
            artistKey = artistKey,
            artistCompact = artistKey.replace(" ", ""),
            swapped = !song.swapped,
        )
    }
    suspend fun rememberDuration(id: String, durationMs: Long) {
        if (durationMs > 0L) dao.setDurationIfUnknown(id, durationMs)
    }

    suspend fun clearLibrary() = dao.clear()

    // ---- source ------------------------------------------------------------

    fun currentSource(): LibrarySource? {
        val stored = prefs.libraryUri ?: return null
        return when (prefs.librarySourceKind) {
            AppPrefs.SOURCE_DIRECT -> LibrarySource.DirectPath(stored)
            AppPrefs.SOURCE_MEDIA -> LibrarySource.MediaLibrary
            else -> LibrarySource.DocumentTree(Uri.parse(stored))
        }
    }

    /** The system video index, for boxes with no picker and no permission screen. */
    fun useMediaLibrary(label: String) {
        prefs.libraryUri = AppPrefs.SOURCE_MEDIA
        prefs.librarySourceKind = AppPrefs.SOURCE_MEDIA
        prefs.libraryLabel = label
    }

    /** Persists the tree grant so the library survives a reboot of the box. */
    fun useDocumentTree(treeUri: Uri, label: String) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        prefs.libraryUri = treeUri.toString()
        prefs.librarySourceKind = AppPrefs.SOURCE_SAF
        prefs.libraryLabel = label
    }

    fun useDirectPath(path: String, label: String) {
        prefs.libraryUri = path
        prefs.librarySourceKind = AppPrefs.SOURCE_DIRECT
        prefs.libraryLabel = label
    }

    fun scan(source: LibrarySource): Flow<ScanProgress> = scanner.scan(source)

    fun markScanned() {
        prefs.lastScanAt = System.currentTimeMillis()
    }
}
