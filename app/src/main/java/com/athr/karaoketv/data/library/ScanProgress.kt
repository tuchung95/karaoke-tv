package com.athr.karaoketv.data.library

sealed interface ScanProgress {
    data class Working(val filesFound: Int, val currentFolder: String) : ScanProgress
    data class Finished(
        val totalSongs: Int,
        val removed: Int,
        val elapsedMs: Long,
    ) : ScanProgress
    data class Failed(val message: String) : ScanProgress
}
