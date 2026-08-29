package com.athr.karaoketv.data.library

/** A media file found on the drive, before it is parsed into a song. */
data class RawFile(
    val uri: String,
    val fileName: String,
    /** Folder path relative to the library root, "" for files sitting at the root. */
    val relPath: String,
    val sizeBytes: Long,
    val lastModified: Long,
)

object MediaFiles {

    /**
     * Containers a TV box's hardware decoder realistically handles. VOB/DAT are
     * included because ripped karaoke DVDs are still everywhere.
     */
    val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "m4v", "mov", "ts", "m2ts", "mts",
        "wmv", "mpg", "mpeg", "flv", "webm", "vob", "dat", "3gp", "ogv",
    )

    fun isVideo(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

    /** Skip system and metadata folders so a scan does not crawl junk. */
    fun isSkippableFolder(name: String): Boolean {
        if (name.startsWith(".")) return true
        return name.equals("System Volume Information", ignoreCase = true) ||
            name.equals("\$RECYCLE.BIN", ignoreCase = true) ||
            name.equals("LOST.DIR", ignoreCase = true) ||
            name.equals("Android", ignoreCase = true)
    }
}
