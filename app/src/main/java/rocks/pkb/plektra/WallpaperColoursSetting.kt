package rocks.pkb.plektra

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Colours from the wallpaper (Android 12 and later), for every keyboard theme. */
@Composable
fun WallpaperColoursSetting(onChanged: () -> Unit = {}) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val context = LocalContext.current
    var on by remember { mutableStateOf(WallpaperKeyboardColours.isEnabled(context)) }
    Row(
        modifier = Modifier.fillMaxWidth().settingRow(WallpaperKeyboardColours.LINK_ID),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.keyboard_theme_wallpaper_colours_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.keyboard_theme_wallpaper_colours_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = on, onCheckedChange = {
            on = it
            WallpaperKeyboardColours.setEnabled(context, it)
            onChanged()
        })
    }
}
