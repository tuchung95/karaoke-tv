package com.athr.karaoketv.ui.youtube

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.athr.karaoketv.ui.components.TvButton
import com.athr.karaoketv.ui.theme.KaraokeColors
import kotlinx.coroutines.delay

/**
 * Plays a YouTube video inside the app through YouTube's own IFrame player.
 *
 * The official embed is the one route that plays here without going around
 * YouTube: the player is theirs, unmodified, and it serves its own ads. That is
 * also the honest limit of this screen — a Premium subscription lives in the
 * YouTube app's signed-in session, not in our WebView, so paying subscribers
 * still get ads here and are better off with "mở trong app YouTube".
 *
 * The remote is wired to the three keys a TV player owes its viewer: centre for
 * play/pause, left and right to seek. Everything else belongs to the browser
 * chrome we deliberately did not draw.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerScreen(
    title: String,
    onExit: () -> Unit,
    onOpenInYouTubeApp: (() -> Unit)?,
    modifier: Modifier = Modifier,
    videoId: String? = null,
    searchQuery: String? = null,
) {
    val focus = remember { FocusRequester() }
    val failureFocus = remember { FocusRequester() }
    var view by remember { mutableStateOf<WebView?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var ended by remember { mutableStateOf(false) }

    // Retried, the same way the video stage does it: a dropped focus request
    // leaves the remote pressing keys that go nowhere.
    LaunchedEffect(Unit) {
        repeat(15) {
            if (runCatching { focus.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(40)
        }
    }

    // A refused embed is a dead end unless the remote can reach the way out of it.
    LaunchedEffect(failure) {
        if (failure != null && onOpenInYouTubeApp != null) {
            repeat(15) {
                if (runCatching { failureFocus.requestFocus() }.isSuccess) return@LaunchedEffect
                delay(40)
            }
        }
    }

    // The end of a video is an ending too. Say so, then hand the room back to the
    // list rather than leaving them on a stopped player.
    LaunchedEffect(ended) {
        if (ended) {
            delay(3500)
            onExit()
        }
    }

    // This screen draws no controls, so the keys are announced once and then get
    // out of the way of the video.
    var hintVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(6000)
        hintVisible = false
    }

    DisposableEffect(Unit) {
        onDispose {
            // Stop the sound before the view goes away; a destroyed WebView that
            // was still playing can keep audio running for a beat.
            view?.loadUrl("about:blank")
            view?.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) {
                    return@onPreviewKeyEvent false
                }
                val web = view ?: return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        web.evaluateJavascript("toggle()", null)
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        web.evaluateJavascript("toggle()", null)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        web.evaluateJavascript("seekBy(-10)", null)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        web.evaluateJavascript("seekBy(10)", null)
                        true
                    }
                    // A search rarely puts the right rip first, so stepping through
                    // the results is the most-used control on this screen.
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        if (searchQuery == null) return@onPreviewKeyEvent false
                        web.evaluateJavascript("nextResult()", null)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        if (searchQuery == null) return@onPreviewKeyEvent false
                        web.evaluateJavascript("previousResult()", null)
                        true
                    }
                    else -> false
                }
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    val toMain = Handler(Looper.getMainLooper())
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    // Autoplay: nobody can tap a TV, and the viewer already chose
                    // this video on the previous screen.
                    settings.mediaPlaybackRequiresUserGesture = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    isFocusable = false
                    isFocusableInTouchMode = false
                    keepScreenOn = true
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onEnded() {
                                toMain.post { ended = true }
                            }

                            @JavascriptInterface
                            fun onError(code: String) {
                                toMain.post { failure = playerErrorMessage(code) }
                            }
                        },
                        "KaraokeTV",
                    )
                    // A real origin is required or the IFrame API refuses to load.
                    loadDataWithBaseURL(
                        "https://www.youtube.com",
                        playerHtml(videoId, searchQuery),
                        "text/html",
                        "utf-8",
                        null,
                    )
                    view = this
                }
            },
            onRelease = { it.destroy() },
        )

        if (failure != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KaraokeColors.Scrim)
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Không phát được bài này",
                    style = MaterialTheme.typography.headlineMedium,
                    color = KaraokeColors.OnSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = failure!!,
                    style = MaterialTheme.typography.titleMedium,
                    color = KaraokeColors.Muted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                if (onOpenInYouTubeApp != null) {
                    TvButton(
                        text = "Mở trong app YouTube",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        emphasised = true,
                        focusRequester = failureFocus,
                        onClick = onOpenInYouTubeApp,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    text = "Hoặc bấm BACK để chọn bài khác",
                    style = MaterialTheme.typography.titleMedium,
                    color = KaraokeColors.Accent,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (hintVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = if (searchQuery != null) {
                        "OK phát/dừng · ◀ ▶ tua 10 giây · ▲ ▼ đổi kết quả · BACK thoát"
                    } else {
                        "OK phát/dừng · ◀ ▶ tua 10 giây · BACK thoát"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = KaraokeColors.Muted,
                    textAlign = TextAlign.Center,
                )
            }
        } else if (ended) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KaraokeColors.Scrim)
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Hết bài",
                    style = MaterialTheme.typography.displaySmall,
                    color = KaraokeColors.Primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = KaraokeColors.Muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Error codes the IFrame player reports. 101 and 150 are the same thing: the
 * uploader disallowed embedding — worth naming, because the fix is to open it in
 * the YouTube app rather than to try again.
 */
private fun playerErrorMessage(code: String): String = when (code.trim()) {
    "101", "150" -> "Chủ kênh không cho phát bài này ngoài YouTube."
    "152" -> "YouTube không nhận trình duyệt của box này — thường là do WebView đã cũ. " +
        "Cập nhật Android System WebView trong CH Play, hoặc mở bằng app YouTube."
    "100" -> "Video này đã bị gỡ khỏi YouTube."
    "5" -> "Box này không phát được video đó."
    "2" -> "Mã video không hợp lệ."
    else -> "YouTube báo lỗi $code."
}

/** Escapes a query for the single-quoted JS string it is dropped into. */
private fun jsString(value: String): String = value
    .replace("\\", "\\\\")
    .replace("'", "\\'")
    .replace("\n", " ")

/**
 * Fed either one video or a whole search. The search form is what makes this
 * screen work with no Data API key at all: the embedded player runs the query
 * itself and holds the results as a playlist.
 */
private fun playerHtml(videoId: String?, searchQuery: String?): String {
    val source = if (searchQuery != null) {
        "listType: 'search', list: '${jsString(searchQuery)}'"
    } else {
        "videoId: '${jsString(videoId.orEmpty())}'"
    }
    return """
<!DOCTYPE html>
<html>
  <head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
      html, body { margin: 0; padding: 0; height: 100%; background: #000; overflow: hidden; }
      #player { width: 100%; height: 100%; }
    </style>
  </head>
  <body>
    <div id="player"></div>
    <script src="https://www.youtube.com/iframe_api"></script>
    <script>
      var player;
      function onYouTubeIframeAPIReady() {
        player = new YT.Player('player', {
          playerVars: {
            $source,
            autoplay: 1, controls: 0, rel: 0, playsinline: 1, modestbranding: 1,
            enablejsapi: 1, origin: 'https://www.youtube.com'
          },
          events: {
            onReady: function (e) { e.target.playVideo(); },
            onStateChange: function (e) {
              if (e.data === YT.PlayerState.ENDED) { KaraokeTV.onEnded(); }
            },
            onError: function (e) { KaraokeTV.onError(String(e.data)); }
          }
        });
      }
      function toggle() {
        if (!player || !player.getPlayerState) return;
        if (player.getPlayerState() === 1) { player.pauseVideo(); } else { player.playVideo(); }
      }
      function seekBy(delta) {
        if (!player || !player.getCurrentTime) return;
        player.seekTo(Math.max(0, player.getCurrentTime() + delta), true);
      }
      function nextResult() { if (player && player.nextVideo) player.nextVideo(); }
      function previousResult() { if (player && player.previousVideo) player.previousVideo(); }
    </script>
  </body>
</html>
"""
}
