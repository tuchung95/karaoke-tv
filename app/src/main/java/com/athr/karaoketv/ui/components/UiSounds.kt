package com.athr.karaoketv.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.SoundEffectConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

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
    val view = LocalView.current
    val tones = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME) }.getOrNull()
    }
    DisposableEffect(tones) {
        onDispose { runCatching { tones?.release() } }
    }
    return remember(enabled, view, tones) {
        if (!enabled) {
            SilentSounds
        } else {
            object : UiSounds {
                override fun navigate() {
                    view.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT)
                }

                override fun select() {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                }

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
