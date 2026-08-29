package com.athr.karaoketv.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.ui.theme.KaraokeColors

/**
 * A grid keyboard rather than a QWERTY one: on a D-pad the fastest layout is the
 * one where every key is the same distance apart and the alphabet runs in the
 * order people already know. Diacritics are deliberately absent — the index is
 * folded, so "gan nhu la" finds "Gần Như Là".
 */
private val LETTER_ROWS = listOf(
    "ABCDEFGH",
    "IJKLMNOP",
    "QRSTUVWX",
    "YZ012345",
    "6789",
)

@Composable
fun OnScreenKeyboard(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    firstKeyFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LETTER_ROWS.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { keyIndex, ch ->
                    KeyCap(
                        label = ch.toString(),
                        onClick = { onKey(ch.toString()) },
                        focusRequester = if (rowIndex == 0 && keyIndex == 0) firstKeyFocus else null,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WideKey("Dấu cách", onClick = { onKey(" ") }, widthDp = 130)
            WideKey("Xóa", onClick = onBackspace, widthDp = 84)
            WideKey("Xóa hết", onClick = onClear, widthDp = 100)
        }
    }
}

@Composable
private fun KeyCap(
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
) = KeyButton(label, onClick, width = 40, focusRequester = focusRequester)

@Composable
private fun WideKey(label: String, onClick: () -> Unit, widthDp: Int) =
    KeyButton(label, onClick, width = widthDp, focusRequester = null)

@Composable
private fun KeyButton(
    label: String,
    onClick: () -> Unit,
    width: Int,
    focusRequester: FocusRequester?,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(width = width.dp, height = 40.dp)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.colors(
            containerColor = KaraokeColors.Surface,
            contentColor = KaraokeColors.Muted,
            focusedContainerColor = KaraokeColors.OnSurface,
            focusedContentColor = KaraokeColors.Background,
        ),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
    }
}
