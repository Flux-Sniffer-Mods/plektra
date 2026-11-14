package rocks.pkb.plektra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rocks.pkb.plektra.data.emoji.EmojiLayerRecents

/** Recent emoji first on the classic emoji layer, and how many keys they take. */
@Composable
fun EmojiLayerRecentsSetting() {
    val context = LocalContext.current
    var on by remember { mutableStateOf(EmojiLayerRecents.enabled(context)) }
    var count by remember { mutableIntStateOf(EmojiLayerRecents.count(context)) }
    Surface(modifier = Modifier.settingRow(EmojiLayerRecents.LINK_ID).fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 52.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.emoji_layer_recents_first_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(R.string.emoji_layer_recents_first_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = on, onCheckedChange = { value ->
                    on = value
                    EmojiLayerRecents.setEnabled(context, value)
                })
            }
            if (on) {
                Text(
                    stringResource(R.string.emoji_layer_recents_count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Slider(
                    value = count.toFloat(),
                    onValueChange = { value ->
                        count = value.toInt()
                        EmojiLayerRecents.setCount(context, count)
                    },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }
        }
    }
}
