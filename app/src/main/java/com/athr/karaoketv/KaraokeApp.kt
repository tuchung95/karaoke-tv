package com.athr.karaoketv

import android.app.Application
import com.athr.karaoketv.data.prefs.AppPrefs
import com.athr.karaoketv.data.repo.LibraryRepository
import com.athr.karaoketv.player.PlayerController

/**
 * The player and the library index outlive any single screen — a song keeps
 * playing while people browse for the next one — so both live here.
 */
class KaraokeApp : Application() {

    val prefs: AppPrefs by lazy { AppPrefs(this) }
    val repository: LibraryRepository by lazy { LibraryRepository(this, prefs) }
    val playerController: PlayerController by lazy { PlayerController(this, repository, prefs) }
}
