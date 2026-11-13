package rocks.pkb.plektra.spellcheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rocks.pkb.plektra.R
import rocks.pkb.plektra.settingRow

/** The switch for the phone's spell checker's corrections in the suggestion bar. */
@Composable
fun PhoneSpellCheckerSetting(enabled: Boolean) {
    val context = LocalContext.current
    var on by remember { mutableStateOf(PhoneSpellChecker.isEnabled(context)) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .settingRow(PhoneSpellChecker.LINK_ID) {
                if (enabled) {
                    on = !on
                    PhoneSpellChecker.setEnabled(context, on)
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.phone_spell_checker_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.phone_spell_checker_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = on,
                onCheckedChange = { value -> on = value; PhoneSpellChecker.setEnabled(context, value) },
                enabled = enabled
            )
        }
    }
}
