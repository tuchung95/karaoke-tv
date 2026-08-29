package com.athr.karaoketv.data.db

/** A browse shelf: a folder, a collection or an artist, with how many songs it holds. */
data class LibraryGroup(
    val name: String,
    val songCount: Int,
)
