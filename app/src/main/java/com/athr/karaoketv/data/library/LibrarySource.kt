package com.athr.karaoketv.data.library

import android.net.Uri

/** Where the karaoke files live. */
sealed interface LibrarySource {
    /** A document tree the user granted with the system folder picker. */
    data class DocumentTree(val treeUri: Uri) : LibrarySource

    /** A plain filesystem path, e.g. /storage/A1B2-C3D4/Karaoke. */
    data class DirectPath(val path: String) : LibrarySource
}
