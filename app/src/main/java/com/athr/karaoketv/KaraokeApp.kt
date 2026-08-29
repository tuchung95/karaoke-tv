package com.athr.karaoketv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import com.athr.karaoketv.data.prefs.AppPrefs
import com.athr.karaoketv.data.repo.LibraryRepository
import com.athr.karaoketv.player.PlayerController

/**
 * The player and the library index outlive any single screen — a song keeps
 * playing while people browse for the next one — so both live here.
 */
class KaraokeApp : Application(), ImageLoaderFactory {

    val prefs: AppPrefs by lazy { AppPrefs(this) }
    val repository: LibraryRepository by lazy { LibraryRepository(this, prefs) }
    val playerController: PlayerController by lazy { PlayerController(this, repository, prefs) }

    /**
     * Thumbnails are frames pulled out of the karaoke videos themselves — there are
     * no cover images on a drive full of rips. Decoding a frame is expensive, so the
     * results are cached on disk and survive restarts.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("thumbnails"))
                .maxSizeBytes(THUMBNAIL_CACHE_BYTES)
                .build()
        }
        .build()

    private companion object {
        const val THUMBNAIL_CACHE_BYTES = 200L * 1024 * 1024
    }
}
