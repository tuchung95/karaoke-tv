package com.athr.karaoketv.data.library

import android.net.Uri

/** Where the karaoke files live. */
sealed interface LibrarySource {
    /** A document tree the user granted with the system folder picker. */
    data class DocumentTree(val treeUri: Uri) : LibrarySource

    /** A plain filesystem path, e.g. /storage/A1B2-C3D4/Karaoke. */
    data class DirectPath(val path: String) : LibrarySource

    /**
     * Everything the system's own media index knows about, across every mounted
     * volume including USB.
     *
     * The last resort that is not really a last resort: plenty of TV boxes ship
     * neither a folder picker nor an all-files permission screen, and on those the
     * other two sources are simply unreachable. This one only needs the ordinary
     * read-video permission, which every Android has a dialog for.
     */
    data object MediaLibrary : LibrarySource
}
