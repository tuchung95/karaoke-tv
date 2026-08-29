package com.athr.karaoketv.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index("titleKey"),
        Index("titleCompact"),
        Index("titleInitials"),
        Index("artistKey"),
        Index("songNumber"),
        Index("category"),
        Index("collection"),
        Index("scanStamp"),
    ],
)
data class SongEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val fileName: String,

    val title: String,
    /** Folded, space-separated: "gan nhu la". */
    val titleKey: String,
    /** Folded, spaces removed: "gannhula" — matches typing without the space key. */
    val titleCompact: String,
    /** First letters: "gnl". */
    val titleInitials: String,

    val artist: String?,
    val artistKey: String?,
    val artistCompact: String?,

    /** Traditional karaoke book number, when the filename carries one. */
    val songNumber: String?,
    /** "Nam", "Nữ", "Tông thấp"… parsed from the filename. */
    val tone: String?,

    /** Folder path relative to the library root, e.g. "Nhac Tre/Den Vau". */
    val relPath: String,
    /** Top-level folder under the root — used as the genre/category shelf. */
    val category: String?,
    /** Immediate parent folder — used as the sub-shelf. */
    val collection: String?,

    val sizeBytes: Long,
    val lastModified: Long,
    /** 0 until the song has been played once and ExoPlayer reported a duration. */
    val durationMs: Long = 0L,

    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val favorite: Boolean = false,
    val addedAt: Long = 0L,

    /** Stamp of the scan that last saw this file; older rows are pruned after a scan. */
    val scanStamp: Long = 0L,
)
