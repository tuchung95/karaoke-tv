package com.athr.karaoketv.ui.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class VoiceSearchHandle(
    val available: Boolean,
    val listening: Boolean,
    val partial: String,
    val error: String?,
    val start: () -> Unit,
    val stop: () -> Unit,
)

/**
 * Voice search over the karaoke index. Recognition runs in-app rather than through
 * the system's full-screen dialog so the search results keep updating live behind
 * it — asking for "gần như là" and watching the list settle is much less jarring
 * on a TV than being thrown into another activity.
 */
@Composable
fun rememberVoiceSearch(
    onPartial: (String) -> Unit,
    onResult: (String) -> Unit,
): VoiceSearchHandle {
    val context = LocalContext.current
    val currentOnPartial by rememberUpdatedState(onPartial)
    val currentOnResult by rememberUpdatedState(onResult)

    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    var listening by remember { mutableStateOf(false) }
    var partial by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingStart by remember { mutableStateOf(false) }

    val recognizer = remember(available) {
        if (available) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                error = null
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                listening = false
            }

            override fun onError(code: Int) {
                listening = false
                error = describeError(code)
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val best = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (best.isNotBlank()) {
                    partial = best
                    currentOnResult(best)
                }
            }

            override fun onPartialResults(results: Bundle?) {
                val best = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (best.isNotBlank()) {
                    partial = best
                    currentOnPartial(best)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose { recognizer?.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted && pendingStart) {
            pendingStart = false
            recognizer?.startListening(recognizerIntent(context))
        } else if (!granted) {
            pendingStart = false
            error = "Cần cấp quyền micro để tìm bằng giọng nói"
        }
    }

    return VoiceSearchHandle(
        available = available,
        listening = listening,
        partial = partial,
        error = error,
        start = {
            error = null
            partial = ""
            when {
                recognizer == null -> error = "Thiết bị này không hỗ trợ tìm bằng giọng nói"
                !hasPermission -> {
                    pendingStart = true
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                else -> recognizer.startListening(recognizerIntent(context))
            }
        },
        stop = {
            recognizer?.stopListening()
            listening = false
        },
    )
}

private fun recognizerIntent(context: Context) =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
    }

private fun describeError(code: Int): String = when (code) {
    SpeechRecognizer.ERROR_NO_MATCH -> "Không nghe rõ, thử lại nhé"
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Không nghe thấy gì"
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
        "Nhận dạng giọng nói cần mạng"
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Chưa có quyền micro"
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Đang bận, thử lại"
    else -> "Không nhận dạng được giọng nói"
}
