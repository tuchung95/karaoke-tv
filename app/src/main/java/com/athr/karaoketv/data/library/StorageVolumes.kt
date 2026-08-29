package com.athr.karaoketv.data.library

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import java.io.File

/**
 * Locates plugged-in drives.
 *
 * On Android 10 and older a USB volume under /storage/<UUID> is world-readable and
 * crawling it directly is much faster than SAF. From Android 11 the File API cannot
 * reach removable storage at all, whatever permissions are held, so on those boxes
 * picking a volume opens the system folder picker already pointed at that drive —
 * same two taps for the viewer, but through the only door the OS still leaves open.
 */
object StorageVolumes {

    /** Android 11 walled off the File API for removable storage; SAF is the only way in. */
    val directAccessSupported: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R

    data class Volume(val label: String, val path: String, val fileCountHint: Int)

    fun list(context: Context): List<Volume> {
        val found = LinkedHashMap<String, Volume>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sm = context.getSystemService(StorageManager::class.java)
            sm?.storageVolumes?.forEach { sv ->
                val dir = sv.directory ?: return@forEach
                if (!dir.canRead()) return@forEach
                val label = sv.getDescription(context)
                    ?: if (sv.isRemovable) "Ổ cắm ngoài" else "Bộ nhớ trong"
                found[dir.absolutePath] = Volume(label, dir.absolutePath, countTopLevel(dir))
            }
        }

        // /storage/<UUID> covers the older boxes that the API above misses.
        File("/storage").listFiles()?.forEach { child ->
            if (!child.isDirectory || !child.canRead()) return@forEach
            if (child.name == "self" || child.name == "emulated") return@forEach
            found.getOrPut(child.absolutePath) {
                Volume("Ổ cắm ngoài (${child.name})", child.absolutePath, countTopLevel(child))
            }
        }

        val primary = Environment.getExternalStorageDirectory()
        if (primary != null && primary.canRead()) {
            found.getOrPut(primary.absolutePath) {
                Volume("Bộ nhớ trong", primary.absolutePath, countTopLevel(primary))
            }
        }

        return found.values.sortedByDescending { it.fileCountHint }
    }

    private fun countTopLevel(dir: File): Int =
        runCatching { dir.listFiles()?.size ?: 0 }.getOrDefault(0)

    /**
     * Opens the system folder picker on a specific drive instead of wherever it
     * last was, so "the USB stick" is one confirmation away rather than a hunt
     * through a tree with a D-pad.
     */
    fun documentInitialUri(path: String): Uri? {
        val documentId = when {
            path.startsWith("/storage/emulated") -> "primary:"
            else -> "${path.substringAfterLast('/')}:"
        }
        return runCatching {
            DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, documentId)
        }.getOrNull()
    }

    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
}
