package com.athr.karaoketv.ui.components

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import com.athr.karaoketv.R
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * The clicks a remote is supposed to make.
 *
 * Focus and selection use the platform's own effects rather than bundled audio, so
 * they match every other app on the box and go quiet when the viewer turns system
 * sounds off. Voice search gets real tones instead: it is the one action with no
 * visible moving part, so without a sound nobody can tell whether the box heard
 * them or the microphone is dead.
 */
interface UiSounds {
    fun navigate()
    fun select()
    fun listenStart()
    fun listenDone()
    fun listenFailed()
}

val LocalUiSounds = staticCompositionLocalOf<UiSounds> { SilentSounds }

object SilentSounds : UiSounds {
    override fun navigate() = Unit
    override fun select() = Unit
    override fun listenStart() = Unit
    override fun listenDone() = Unit
    override fun listenFailed() = Unit
}

@Composable
fun rememberUiSounds(enabled: Boolean): UiSounds {
    val context = LocalContext.current
    val tones = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME) }.getOrNull()
    }
    // Our own clips rather than View.playSoundEffect: that route goes silent
    // whenever the box has system touch sounds turned off, and on a TV nobody
    // knows that setting exists — the app just feels dead.
    val pool = remember {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }
    val tickId = remember(pool) { pool.load(context, R.raw.nav_tick, 1) }
    val selectId = remember(pool) { pool.load(context, R.raw.nav_select, 1) }

    DisposableEffect(pool, tones) {
        onDispose {
            runCatching { pool.release() }
            runCatching { tones?.release() }
        }
    }

    return remember(enabled, pool, tickId, selectId, tones) {
        if (!enabled) {
            SilentSounds
        } else {
            object : UiSounds {
                private fun play(id: Int, volume: Float) {
                    if (id != 0) pool.play(id, volume, volume, 1, 0, 1f)
                }

                override fun navigate() = play(tickId, NAV_VOLUME)
                override fun select() = play(selectId, SELECT_VOLUME)

                override fun listenStart() {
                    tones?.startTone(ToneGenerator.TONE_PROP_BEEP, SHORT_TONE_MS)
                }

                override fun listenDone() {
                    tones?.startTone(ToneGenerator.TONE_PROP_ACK, SHORT_TONE_MS)
                }

                override fun listenFailed() {
                    tones?.startTone(ToneGenerator.TONE_PROP_NACK, SHORT_TONE_MS)
                }
            }
        }
    }
}

private const val NAV_VOLUME = 0.35f
private const val SELECT_VOLUME = 0.55f
private const val TONE_VOLUME = 60
private const val SHORT_TONE_MS = 120

/** Clicks once when focus arrives, never on the way out. */
@Composable
fun Modifier.navigationSound(): Modifier {
    val sounds = LocalUiSounds.current
    var wasFocused by remember { mutableStateOf(false) }
    return this.onFocusChanged {
        if (it.isFocused && !wasFocused) sounds.navigate()
        wasFocused = it.isFocused
    }
}

/** Wraps a click handler so selecting anything makes the same sound. */
@Composable
fun withSelectSound(onClick: () -> Unit): () -> Unit {
    val sounds = LocalUiSounds.current
    return { sounds.select(); onClick() }
}
