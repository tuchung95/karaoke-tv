package com.athr.karaoketv.player

import com.athr.karaoketv.data.db.SongEntity

/**
 * One entry in the sing-along queue. The uid is per-entry, not per-song, so the
 * same track can sit in the list twice without the UI confusing them.
 */
data class QueueItem(
    val uid: Long,
    val song: SongEntity,
)
