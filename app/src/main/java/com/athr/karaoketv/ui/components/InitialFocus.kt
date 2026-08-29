package com.athr.karaoketv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
        repeat(ATTEMPTS) {
            if (runCatching { focusRequester.requestFocus() }.isSuccess) {
                return@LaunchedEffect
            }
            delay(RETRY_DELAY_MS)
        }
    }
}

private const val ATTEMPTS = 15
private const val RETRY_DELAY_MS = 40L
