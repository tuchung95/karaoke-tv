package com.athr.karaoketv.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * Ripped karaoke VCDs and DVDs almost always put the guide vocal on one channel
 * and the backing track on the other. Duplicating a single channel across both
 * outputs is what the "bỏ giọng ca sĩ" button on a real karaoke deck does, and
 * it is the only vocal removal that actually works on those discs.
 *
 * Only 16-bit stereo PCM is handled; anything else is reported as unsupported and
 * the sink bypasses this stage. The processor stays active even in [Mode.STEREO]
 * so the mode can be flipped mid-song without reconfiguring the audio pipeline.
 */
@OptIn(UnstableApi::class)
class ChannelMixProcessor : BaseAudioProcessor() {

    enum class Mode { STEREO, LEFT_ONLY, RIGHT_ONLY, MONO }

    @Volatile
    var mode: Mode = Mode.STEREO

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat,
    ): AudioProcessor.AudioFormat {
        val usable = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT &&
            inputAudioFormat.channelCount == 2
        return if (usable) inputAudioFormat else AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val frames = (limit - position) / BYTES_PER_FRAME
        val output = replaceOutputBuffer(frames * BYTES_PER_FRAME)

        val currentMode = mode
        var i = position
        while (i + BYTES_PER_FRAME <= limit) {
            val left = sampleAt(inputBuffer, i)
            val right = sampleAt(inputBuffer, i + 2)
            when (currentMode) {
                Mode.STEREO -> {
                    putSample(output, left)
                    putSample(output, right)
                }
                Mode.LEFT_ONLY -> {
                    putSample(output, left)
                    putSample(output, left)
                }
                Mode.RIGHT_ONLY -> {
                    putSample(output, right)
                    putSample(output, right)
                }
                Mode.MONO -> {
                    val mixed = ((left + right) / 2)
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    putSample(output, mixed)
                    putSample(output, mixed)
                }
            }
            i += BYTES_PER_FRAME
        }

        inputBuffer.position(limit)
        output.flip()
    }

    private fun sampleAt(buffer: ByteBuffer, index: Int): Int {
        val low = buffer.get(index).toInt() and 0xFF
        val high = buffer.get(index + 1).toInt()
        return ((high shl 8) or low).toShort().toInt()
    }

    private fun putSample(buffer: ByteBuffer, value: Int) {
        buffer.put((value and 0xFF).toByte())
        buffer.put(((value shr 8) and 0xFF).toByte())
    }

    private companion object {
        const val BYTES_PER_FRAME = 4 // 2 channels * 16-bit
    }
}
