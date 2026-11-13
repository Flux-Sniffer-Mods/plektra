package rocks.pkb.plektra

import android.content.Context
import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rocks.pkb.plektra.inputmethod.EmojiKey

/** The emoji key: which key it is (pressed to assign), what it opens, and tap for one emoji. */
@Composable
fun EmojiKeySetting() {
    val context = LocalContext.current
    var key by remember { mutableIntStateOf(EmojiKey.keyCode(context)) }
    var opensLayer by remember { mutableStateOf(EmojiKey.opensLayer(context)) }
    var sticky by remember { mutableStateOf(EmojiKey.stickyTap(context)) }
    var assigning by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.settingRow(EmojiKey.LINK_ID) { assigning = true }.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 52.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)) {
            Text(stringResource(R.string.emoji_picker_key_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                stringResource(R.string.emoji_picker_key_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.emoji_picker_key_current, emojiKeyLabel(context, key)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    if (key != KeyEvent.KEYCODE_UNKNOWN) {
        Surface(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 52.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)) {
                Text(stringResource(R.string.emoji_key_target_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !opensLayer,
                        onClick = { opensLayer = false; EmojiKey.setOpensLayer(context, false) },
                        label = { Text(stringResource(R.string.emoji_key_target_picker)) }
                    )
                    FilterChip(
                        selected = opensLayer,
                        onClick = { opensLayer = true; EmojiKey.setOpensLayer(context, true) },
                        label = { Text(stringResource(R.string.emoji_key_target_layer)) }
                    )
                }
            }
        }
        Surface(modifier = Modifier.settingRow(EmojiKey.STICKY_LINK_ID).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 52.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.emoji_sticky_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(R.string.emoji_sticky_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = sticky, onCheckedChange = { value -> sticky = value; EmojiKey.setStickyTap(context, value) })
            }
        }
    }

    if (assigning) {
        val focus = remember { FocusRequester() }
        var rejected by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { assigning = false },
            title = { Text(stringResource(R.string.emoji_picker_key_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            val native = event.nativeKeyEvent
                            // Back closes the dialog as usual
                            if (native.keyCode == KeyEvent.KEYCODE_BACK) return@onPreviewKeyEvent false
                            if (native.action == KeyEvent.ACTION_DOWN && native.repeatCount == 0) {
                                if (EmojiKey.isAllowed(native.keyCode, native.isPrintingKey)) {
                                    EmojiKey.setKeyCode(context, native.keyCode)
                                    key = native.keyCode
                                    assigning = false
                                } else {
                                    rejected = true
                                }
                            }
                            true
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.emoji_picker_key_description), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.emoji_picker_key_current, emojiKeyLabel(context, key)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(if (rejected) R.string.emoji_picker_key_rejected else R.string.emoji_picker_key_press_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (rejected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            },
            confirmButton = {
                TextButton(onClick = {
                    EmojiKey.setKeyCode(context, KeyEvent.KEYCODE_UNKNOWN)
                    key = KeyEvent.KEYCODE_UNKNOWN
                    assigning = false
                }) { Text(stringResource(R.string.emoji_picker_key_turn_off)) }
            },
            dismissButton = {
                TextButton(onClick = { assigning = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

internal fun emojiKeyLabel(context: Context, keyCode: Int): String = when (keyCode) {
    KeyEvent.KEYCODE_UNKNOWN -> context.getString(R.string.emoji_picker_key_off)
    KeyEvent.KEYCODE_SHIFT_RIGHT -> context.getString(R.string.emoji_picker_key_right_shift)
    KeyEvent.KEYCODE_SHIFT_LEFT -> context.getString(R.string.emoji_picker_key_left_shift)
    KeyEvent.KEYCODE_ALT_RIGHT -> context.getString(R.string.emoji_picker_key_right_alt)
    KeyEvent.KEYCODE_ALT_LEFT -> context.getString(R.string.emoji_picker_key_left_alt)
    KeyEvent.KEYCODE_CTRL_RIGHT -> context.getString(R.string.emoji_picker_key_right_ctrl)
    KeyEvent.KEYCODE_CTRL_LEFT -> context.getString(R.string.emoji_picker_key_left_ctrl)
    KeyEvent.KEYCODE_FUNCTION -> "Fn"
    else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").replace('_', ' ')
        .lowercase().replaceFirstChar { it.uppercase() }
}
