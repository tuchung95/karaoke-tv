package com.athr.karaoketv.ui

/** Where a song list came from, so the list screen can load and title itself. */
sealed interface SongListSource {
    data class Category(val name: String) : SongListSource
    data class Artist(val name: String) : SongListSource
    data object Favorites : SongListSource
    data object MostPlayed : SongListSource
    data object All : SongListSource
}

sealed interface Screen {
    data object Home : Screen
    data object Search : Screen
    data object Categories : Screen
    data object Artists : Screen
    data object Queue : Screen
    data object Settings : Screen
    data object HomeLayout : Screen

    /** A node in the drive's own folder tree. Empty path is the library root. */
    data class Folder(val path: String, val title: String) : Screen
    data class SongList(val title: String, val source: SongListSource) : Screen
}
