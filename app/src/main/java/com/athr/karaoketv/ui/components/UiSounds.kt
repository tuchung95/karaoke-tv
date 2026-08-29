package com.athr.karaoketv.ui.components

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.athr.karaoketv.R
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
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
    // Listener attached before the clips are loaded, all in one block: registering
    // it afterwards misses samples that finished decoding in the meantime, and then
    // nothing is ever marked ready and every click is a silent no-op.
    val clips = remember {
        val ready = mutableStateListOf<Int>()
        val soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                // USAGE_MEDIA, not SONIFICATION: on a TV box the sonification
                // stream is often left silent, while the music stream is the one
                // the room has turned up.
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) ready.add(sampleId)
        }
        Clips(
            pool = soundPool,
            tick = soundPool.load(context, R.raw.nav_tick, 1),
            select = soundPool.load(context, R.raw.nav_select, 1),
            ready = ready,
        )
    }
    val pool = clips.pool

    DisposableEffect(clips, tones) {
        onDispose {
            runCatching { clips.pool.release() }
            runCatching { tones?.release() }
        }
    }

    return remember(enabled, clips, tones) {
        if (!enabled) {
            SilentSounds
        } else {
            object : UiSounds {
                private fun play(id: Int, volume: Float) {
                    if (id != 0 && id in clips.ready) {
                        pool.play(id, volume, volume, 1, 0, 1f)
                    }
                }

                override fun navigate() = play(clips.tick, NAV_VOLUME)
                override fun select() = play(clips.select, SELECT_VOLUME)

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

private const val NAV_VOLUME = 0.6f
private const val SELECT_VOLUME = 0.9f
private const val TONE_VOLUME = 60
private const val SHORT_TONE_MS = 120

/**
 * An interaction source that clicks when focus lands on whatever it is given to.
 *
 * Focus has to be observed through the component's own interaction source rather
 * than a `Modifier.onFocusChanged` wrapped around it: the design system's focus
 * target sits above the caller's modifier in the chain, so an observer placed
 * there only ever sees "not focused" and the sound never fires.
 */
@Composable
fun rememberSoundedInteractionSource(): MutableInteractionSource {
    val sounds = LocalUiSounds.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) sounds.navigate() }
    return source
}

/** Wraps a click handler so selecting anything makes the same sound. */
@Composable
fun withSelectSound(onClick: () -> Unit): () -> Unit {
    val sounds = LocalUiSounds.current
    return { sounds.select(); onClick() }
}

private class Clips(
    val pool: SoundPool,
    val tick: Int,
    val select: Int,
    val ready: List<Int>,
)
