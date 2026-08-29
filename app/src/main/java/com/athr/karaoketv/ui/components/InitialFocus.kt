package com.athr.karaoketv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

/**
 * Takes focus as soon as the target node is attached.
 *
 * A single `requestFocus()` on first composition is not reliable: the node is
 * often not attached yet on that frame and the call throws. Swallowing that leaves
 * a TV app with no focus anywhere, which means the remote does nothing at all and
 * the screen looks frozen — there is no cursor to fall back on. So keep trying for
 * a short while, and stop as soon as it lands.
 */
@Composable
fun RequestInitialFocus(focusRequester: FocusRequester, key: Any? = Unit) {
    LaunchedEffect(key) {
        // Wait for the first frame: before it, the node is not attached and the
        // request throws.
        withFrameNanos { }
        // Then claim focus a few times over the next moment. One successful call
        // is not enough — Compose may hand initial focus to some other child just
        // after, which is how the home row ended up focusing a middle button and
        // scrolling itself sideways on launch.
        repeat(ATTEMPTS) { attempt ->
            runCatching { focusRequester.requestFocus() }
            if (attempt < ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
    }
}

private const val ATTEMPTS = 6
// Half a second total: long enough to win the race against Compose's own
// initial assignment on a slow cold start, short enough that it cannot yank
// focus back out from under someone already pressing keys.
private const val RETRY_DELAY_MS = 100L
