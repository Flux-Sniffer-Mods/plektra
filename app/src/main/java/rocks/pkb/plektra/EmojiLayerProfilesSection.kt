package rocks.pkb.plektra

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import rocks.pkb.plektra.data.mappings.EmojiLayerProfile
import rocks.pkb.plektra.data.mappings.EmojiLayerProfiles
import rocks.pkb.plektra.data.mappings.KeyMappingLoader

/**
 * Emoji layer profiles: apply a situation's emoji to the emoji layer, keep your own, or let the
 * layer follow the app you're in. [onApplied] gets the emoji layer as it now is.
 */
@Composable
fun EmojiLayerProfilesSection(onApplied: (Map<Int, String>) -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    val active = remember(revision) { EmojiLayerProfiles.activeId(context) }
    val custom = remember(revision) { EmojiLayerProfiles.custom(context) }
    var switchByApp by remember { mutableStateOf(EmojiLayerProfiles.switchByApp(context)) }
    var managing by remember { mutableStateOf<EmojiLayerProfile?>(null) }
    var naming by remember { mutableStateOf<EmojiLayerProfile?>(null) }
    var savingCurrent by remember { mutableStateOf(false) }

    fun currentLayer(): Map<Int, String> =
        SettingsManager.getSymMappings(context).takeIf { it.isNotEmpty() }
            ?: KeyMappingLoader.loadSymKeyMappings(context.assets)

    fun apply(profile: EmojiLayerProfile, name: String) {
        EmojiLayerProfiles.apply(context, profile)
        revision++
        onApplied(profile.mappings)
        Toast.makeText(context, context.getString(R.string.emoji_profiles_applied, name), Toast.LENGTH_SHORT).show()
    }

    ProfileRow(
        title = stringResource(R.string.emoji_profiles_title),
        description = stringResource(R.string.emoji_profiles_description),
        linkId = EmojiLayerProfiles.LINK_ID,
        onClick = { expanded = !expanded }
    )
    if (!expanded) return

    Note(stringResource(R.string.emoji_profiles_intro))
    Surface(modifier = Modifier.settingRow(EmojiLayerProfiles.SWITCH_BY_APP_LINK_ID).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 52.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.emoji_profiles_switch_by_app_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(R.string.emoji_profiles_switch_by_app_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = switchByApp, onCheckedChange = {
                switchByApp = it
                EmojiLayerProfiles.setSwitchByApp(context, it)
            })
        }
    }

    Label(stringResource(R.string.emoji_profiles_situations))
    EmojiLayerProfiles.BUILT_IN.forEach { profile ->
        val name = builtInName(profile)
        ProfileRow(title = titleOf(name, profile.id == active), description = preview(profile), onClick = { apply(profile, name) })
    }

    Label(stringResource(R.string.emoji_profiles_yours))
    ProfileRow(
        title = stringResource(R.string.emoji_profiles_save_current_title),
        description = stringResource(R.string.emoji_profiles_save_current_description),
        onClick = { savingCurrent = true }
    )
    custom.forEach { profile ->
        ProfileRow(title = titleOf(profile.name, profile.id == active), description = preview(profile), onClick = { managing = profile })
    }

    if (savingCurrent) {
        NameDialog(
            initial = context.getString(R.string.emoji_profiles_new_name, custom.size + 1),
            onDismiss = { savingCurrent = false },
            onSave = { name ->
                val created = EmojiLayerProfiles.createCustom(context, name, currentLayer())
                EmojiLayerProfiles.apply(context, created)
                savingCurrent = false
                revision++
            }
        )
    }
    naming?.let { profile ->
        NameDialog(
            initial = profile.name,
            onDismiss = { naming = null },
            onSave = { name ->
                EmojiLayerProfiles.saveCustom(context, profile.copy(name = name))
                naming = null
                revision++
            }
        )
    }
    managing?.let { profile ->
        AlertDialog(
            onDismissRequest = { managing = null },
            title = { Text(profile.name) },
            text = {
                Column {
                    Text(preview(profile))
                    listOf(
                        R.string.emoji_profiles_apply to { apply(profile, profile.name) },
                        R.string.emoji_profiles_update to {
                            EmojiLayerProfiles.saveCustom(context, profile.copy(mappings = currentLayer()))
                            revision++
                        },
                        R.string.emoji_profiles_rename to { naming = profile },
                        R.string.emoji_profiles_delete to {
                            EmojiLayerProfiles.deleteCustom(context, profile.id)
                            revision++
                        }
                    ).forEach { (label, action) ->
                        TextButton(onClick = { managing = null; action() }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(label))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { managing = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun ProfileRow(title: String, description: String, linkId: String? = null, onClick: () -> Unit) {
    Surface(modifier = Modifier.settingRow(linkId, onClick).fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 52.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 52.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 52.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.emoji_profiles_name_title)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true) },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun builtInName(profile: EmojiLayerProfile): String = when (profile.id) {
    "everyday" -> stringResource(R.string.emoji_profile_everyday)
    "chatting" -> stringResource(R.string.emoji_profile_chatting)
    "work" -> stringResource(R.string.emoji_profile_work)
    "social" -> stringResource(R.string.emoji_profile_social)
    "love" -> stringResource(R.string.emoji_profile_love)
    "celebrations" -> stringResource(R.string.emoji_profile_celebrations)
    "food" -> stringResource(R.string.emoji_profile_food)
    "travel" -> stringResource(R.string.emoji_profile_travel)
    "gaming" -> stringResource(R.string.emoji_profile_gaming)
    else -> profile.name
}

@Composable
private fun titleOf(name: String, inUse: Boolean): String =
    if (inUse) "$name · " + stringResource(R.string.emoji_profiles_in_use) else name

/** The top two rows, as they sit on the keys. */
private fun preview(profile: EmojiLayerProfile): String =
    EmojiLayerProfiles.KEYS.take(19).mapNotNull { profile.mappings[it] }.joinToString(" ")
