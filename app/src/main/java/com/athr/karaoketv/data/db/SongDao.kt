package com.athr.karaoketv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    /** Carries user state across a rescan so play counts and favourites survive. */
    @Query(
        "SELECT id, playCount, lastPlayedAt, favorite, durationMs, addedAt, swapped FROM songs"
    )
    suspend fun loadUserState(): List<UserState>

    @Query("DELETE FROM songs WHERE scanStamp != :stamp")
    suspend fun pruneOlderThan(stamp: Long): Int

    @Query("DELETE FROM songs")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM songs")
    fun countFlow(): Flow<Int>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun byId(id: String): SongEntity?

    /**
     * One ranked pass over the index. [q] is the folded query with spaces,
     * [compact] the same without spaces, [raw] the untouched keystrokes (for
     * matching a song number exactly).
     */
    @Query(
        """
        SELECT * FROM songs
        WHERE titleKey LIKE '%' || :q || '%'
           OR titleCompact LIKE '%' || :compact || '%'
           OR titleInitials LIKE :compact || '%'
           OR artistKey LIKE '%' || :q || '%'
           OR artistCompact LIKE '%' || :compact || '%'
           OR songNumber LIKE :raw || '%'
        ORDER BY
          CASE
            WHEN songNumber = :raw THEN 0
            WHEN titleKey = :q THEN 1
            WHEN titleCompact LIKE :compact || '%' THEN 2
            WHEN titleInitials = :compact THEN 3
            WHEN songNumber LIKE :raw || '%' THEN 4
            WHEN titleInitials LIKE :compact || '%' THEN 5
            WHEN titleCompact LIKE '%' || :compact || '%' THEN 6
            WHEN artistCompact LIKE '%' || :compact || '%' THEN 7
            ELSE 8
          END,
          playCount DESC,
          title COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun search(q: String, compact: String, raw: String, limit: Int = 120): List<SongEntity>

    @Query("SELECT * FROM songs WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun recentlyPlayed(limit: Int = 40): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE favorite = 1 ORDER BY title COLLATE NOCASE ASC LIMIT :limit")
    fun favorites(limit: Int = 200): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedAt DESC LIMIT :limit")
    fun mostPlayed(limit: Int = 40): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY addedAt DESC, title COLLATE NOCASE ASC LIMIT :limit")
    fun recentlyAdded(limit: Int = 40): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY RANDOM() LIMIT :limit")
    suspend fun random(limit: Int): List<SongEntity>

    @Query(
        """
        SELECT category AS name, COUNT(*) AS songCount FROM songs
        WHERE category IS NOT NULL AND category != ''
        GROUP BY category ORDER BY songCount DESC, name COLLATE NOCASE ASC
        """
    )
    fun categories(): Flow<List<LibraryGroup>>

    @Query(
        """
        SELECT artist AS name, COUNT(*) AS songCount FROM songs
        WHERE artist IS NOT NULL AND artist != ''
        GROUP BY artist ORDER BY songCount DESC, name COLLATE NOCASE ASC
        """
    )
    fun artists(): Flow<List<LibraryGroup>>

    @Query(
        """
        SELECT collection AS name, COUNT(*) AS songCount FROM songs
        WHERE category = :category AND collection IS NOT NULL AND collection != ''
        GROUP BY collection ORDER BY name COLLATE NOCASE ASC
        """
    )
    suspend fun collectionsIn(category: String): List<LibraryGroup>

    @Query("SELECT * FROM songs WHERE category = :category ORDER BY title COLLATE NOCASE ASC")
    suspend fun songsInCategory(category: String): List<SongEntity>

    /** One row per folder that directly holds songs, with how many it holds. */
    @Query(
        """
        SELECT relPath AS name, COUNT(*) AS songCount FROM songs
        GROUP BY relPath
        """
    )
    suspend fun folderCounts(): List<LibraryGroup>

    @Query("SELECT * FROM songs WHERE relPath = :path ORDER BY title COLLATE NOCASE ASC")
    suspend fun songsDirectlyIn(path: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE collection = :collection ORDER BY title COLLATE NOCASE ASC")
    suspend fun songsInCollection(collection: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY title COLLATE NOCASE ASC")
    suspend fun songsByArtist(artist: String): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC LIMIT :limit OFFSET :offset")
    suspend fun allSongs(limit: Int, offset: Int): List<SongEntity>

    @Query(
        """
        UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :at
        WHERE id = :id
        """
    )
    suspend fun markPlayed(id: String, at: Long)

    @Query(
        """
        UPDATE songs SET
            title = :title, titleKey = :titleKey, titleCompact = :titleCompact,
            titleInitials = :titleInitials,
            artist = :artist, artistKey = :artistKey, artistCompact = :artistCompact,
            swapped = :swapped
        WHERE id = :id
        """
    )
    suspend fun updateNaming(
        id: String,
        title: String,
        titleKey: String,
        titleCompact: String,
        titleInitials: String,
        artist: String?,
        artistKey: String?,
        artistCompact: String?,
        swapped: Boolean,
    )

    @Query("UPDATE songs SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE songs SET durationMs = :durationMs WHERE id = :id AND durationMs = 0")
    suspend fun setDurationIfUnknown(id: String, durationMs: Long)

    data class UserState(
        val id: String,
        val playCount: Int,
        val lastPlayedAt: Long,
        val favorite: Boolean,
        val durationMs: Long,
        val addedAt: Long,
        val swapped: Boolean,
    )
}
