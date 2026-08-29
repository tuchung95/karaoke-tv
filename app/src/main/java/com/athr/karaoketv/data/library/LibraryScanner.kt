package com.athr.karaoketv.data.library

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.athr.karaoketv.data.db.SongDao
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.util.TextNormalizer
import java.io.File
import java.security.MessageDigest
import java.util.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Crawls the drive and rebuilds the song index. A full pass over a few thousand
 * files takes seconds because it queries the document provider in bulk rather
 * than going through DocumentFile, which costs one IPC per node.
 */
class LibraryScanner(
    private val context: Context,
    private val dao: SongDao,
) {

    fun scan(source: LibrarySource): Flow<ScanProgress> = flow {
        val startedAt = System.currentTimeMillis()
        val stamp = startedAt
        try {
            // Play counts, favourites and probed durations must survive a rescan.
            val prior = dao.loadUserState().associateBy { it.id }

            var found = 0
            val batch = ArrayList<SongEntity>(BATCH_SIZE)

            suspend fun flush() {
                if (batch.isEmpty()) return
                dao.upsertAll(batch)
                batch.clear()
            }

            val onFile: suspend (RawFile) -> Unit = { raw ->
                batch += toEntity(raw, prior[idFor(raw)], stamp)
                found++
                if (batch.size >= BATCH_SIZE) flush()
            }
            val onFolder: suspend (String) -> Unit = { folder ->
                emit(ScanProgress.Working(found, folder))
            }

            when (source) {
                is LibrarySource.DocumentTree -> walkTree(source.treeUri, onFolder, onFile)
                is LibrarySource.DirectPath -> walkPath(File(source.path), onFolder, onFile)
                is LibrarySource.MediaLibrary -> walkMediaStore(onFolder, onFile)
            }
            flush()

            val removed = dao.pruneOlderThan(stamp)
            emit(
                ScanProgress.Finished(
                    totalSongs = found,
                    removed = removed,
                    elapsedMs = System.currentTimeMillis() - startedAt,
                )
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "lost access to library", e)
            emit(ScanProgress.Failed("Mất quyền đọc ổ cứng. Hãy chọn lại thư mục."))
        } catch (e: Exception) {
            Log.e(TAG, "scan failed", e)
            emit(ScanProgress.Failed(e.message ?: "Quét thất bại"))
        }
    }.flowOn(Dispatchers.IO)

    // ---- walkers ----------------------------------------------------------

    private suspend fun walkTree(
        treeUri: Uri,
        onFolder: suspend (String) -> Unit,
        onFile: suspend (RawFile) -> Unit,
    ) {
        val resolver = context.contentResolver
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )

        val pending = ArrayDeque<Pair<String, String>>()
        pending += DocumentsContract.getTreeDocumentId(treeUri) to ""

        while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val (docId, relPath) = pending.removeFirst()
            onFolder(if (relPath.isEmpty()) "/" else relPath)

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            resolver.query(childrenUri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val childId = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2)
                    val size = if (c.isNull(3)) 0L else c.getLong(3)
                    val modified = if (c.isNull(4)) 0L else c.getLong(4)

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (MediaFiles.isSkippableFolder(name)) continue
                        pending += childId to joinPath(relPath, name)
                    } else if (MediaFiles.isVideo(name)) {
                        onFile(
                            RawFile(
                                uri = DocumentsContract
                                    .buildDocumentUriUsingTree(treeUri, childId).toString(),
                                fileName = name,
                                relPath = relPath,
                                sizeBytes = size,
                                lastModified = modified,
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Reads the system's video index instead of the filesystem.
     *
     * MediaStore already knows every video on every mounted volume, USB included,
     * and reaching it needs only the ordinary read-video permission — no folder
     * picker, no all-files access, neither of which some TV boxes have at all.
     */
    private suspend fun walkMediaStore(
        onFolder: suspend (String) -> Unit,
        onFile: suspend (RawFile) -> Unit,
    ) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.RELATIVE_PATH,
        )
        val cursor = context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.RELATIVE_PATH} ASC",
        ) ?: throw SecurityException("Không đọc được thư viện video của máy")

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val pathCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
            var lastFolder = ""

            while (it.moveToNext()) {
                val name = it.getString(nameCol) ?: continue
                if (!MediaFiles.isVideo(name)) continue
                val relPath = (it.getString(pathCol) ?: "").trim('/')
                if (relPath != lastFolder) {
                    lastFolder = relPath
                    onFolder(relPath.ifBlank { "Bộ nhớ máy" })
                }
                onFile(
                    RawFile(
                        uri = ContentUris.withAppendedId(collection, it.getLong(idCol)).toString(),
                        fileName = name,
                        relPath = relPath,
                        sizeBytes = it.getLong(sizeCol),
                        lastModified = it.getLong(dateCol) * 1000L,
                    )
                )
            }
        }
    }

    private suspend fun walkPath(
        root: File,
        onFolder: suspend (String) -> Unit,
        onFile: suspend (RawFile) -> Unit,
    ) {
        if (!root.isDirectory || root.listFiles() == null) {
            throw SecurityException("Không đọc được ${root.path}")
        }
        val rootPath = root.absolutePath

        val pending = ArrayDeque<File>()
        pending += root

        while (pending.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val dir = pending.removeFirst()
            val relPath = dir.absolutePath.removePrefix(rootPath).trim('/')
            onFolder(if (relPath.isEmpty()) "/" else relPath)

            val children = dir.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    if (MediaFiles.isSkippableFolder(child.name)) continue
                    pending += child
                } else if (MediaFiles.isVideo(child.name)) {
                    onFile(
                        RawFile(
                            uri = Uri.fromFile(child).toString(),
                            fileName = child.name,
                            relPath = relPath,
                            sizeBytes = child.length(),
                            lastModified = child.lastModified(),
                        )
                    )
                }
            }
        }
    }

    // ---- mapping ----------------------------------------------------------

    private fun toEntity(
        raw: RawFile,
        prior: SongDao.UserState?,
        stamp: Long,
    ): SongEntity {
        val segments = raw.relPath.split('/').filter { it.isNotBlank() }
        val parsed = FilenameParser.parse(raw.fileName, parentFolder = segments.lastOrNull())
        // A correction the viewer made by hand outranks whatever the filename says.
        val corrected = if (prior?.swapped == true && parsed.artist != null) {
            parsed.copy(title = parsed.artist, artist = parsed.title)
        } else {
            parsed
        }
        val title = corrected.title.ifBlank { raw.fileName.substringBeforeLast('.', raw.fileName) }
        val titleKey = TextNormalizer.key(title)
        val artistKey = corrected.artist?.let { TextNormalizer.key(it) }

        return SongEntity(
            id = idFor(raw),
            uri = raw.uri,
            fileName = raw.fileName,
            title = title,
            titleKey = titleKey,
            titleCompact = titleKey.replace(" ", ""),
            titleInitials = TextNormalizer.initials(title),
            artist = corrected.artist,
            artistKey = artistKey,
            artistCompact = artistKey?.replace(" ", ""),
            songNumber = corrected.songNumber,
            tone = corrected.tone,
            relPath = raw.relPath,
            category = segments.firstOrNull(),
            collection = segments.lastOrNull(),
            sizeBytes = raw.sizeBytes,
            lastModified = raw.lastModified,
            durationMs = prior?.durationMs ?: 0L,
            playCount = prior?.playCount ?: 0,
            lastPlayedAt = prior?.lastPlayedAt ?: 0L,
            favorite = prior?.favorite ?: false,
            swapped = prior?.swapped ?: false,
            addedAt = prior?.addedAt?.takeIf { it > 0L } ?: stamp,
            scanStamp = stamp,
        )
    }

    /**
     * Identity is path + name + size, never the URI: re-granting the drive mints
     * fresh document URIs, and losing everyone's favourites over that would be a
     * bad surprise.
     */
    private fun idFor(raw: RawFile): String =
        sha1("${raw.relPath}/${raw.fileName}:${raw.sizeBytes}")

    private fun sha1(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun joinPath(parent: String, child: String) =
        if (parent.isEmpty()) child else "$parent/$child"

    private companion object {
        const val TAG = "LibraryScanner"
        const val BATCH_SIZE = 300
    }
}
