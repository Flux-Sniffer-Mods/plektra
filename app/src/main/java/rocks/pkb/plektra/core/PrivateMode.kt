package rocks.pkb.plektra.core

import android.content.Context
import android.widget.Toast
import rocks.pkb.plektra.OfflineMode
import rocks.pkb.plektra.R
import rocks.pkb.plektra.SettingsManager

/**
 * Private mode, from a key or the quick launcher: incognito typing in every field and offline
 * mode together, at once. Turning it off puts offline mode back as it was before. Kept across
 * restarts, and left out of backups: it is a state, not a setting.
 */
object PrivateMode {
    const val KEY_ON = "private_mode"
    private const val KEY_OFFLINE_BEFORE = "private_mode_offline_before"

    fun isOn(context: Context): Boolean = SettingsManager.getPreferences(context).getBoolean(KEY_ON, false)

    /** Read when the app starts, so typing is incognito from the first field. */
    fun load(context: Context) {
        IncognitoTyping.privateMode = isOn(context)
    }

    /** Switches it over and says so; returns whether it is now on. */
    fun toggle(context: Context, announce: Boolean = true): Boolean {
        val prefs = SettingsManager.getPreferences(context)
        val on = !isOn(context)
        if (on) {
            prefs.edit()
                .putBoolean(KEY_ON, true)
                .putBoolean(KEY_OFFLINE_BEFORE, OfflineMode.isOn(context))
                .apply()
            OfflineMode.set(context, true)
        } else {
            val offlineBefore = prefs.getBoolean(KEY_OFFLINE_BEFORE, false)
            prefs.edit().putBoolean(KEY_ON, false).remove(KEY_OFFLINE_BEFORE).apply()
            OfflineMode.set(context, offlineBefore)
        }
        IncognitoTyping.privateMode = on
        if (announce) {
            Toast.makeText(
                context,
                if (on) R.string.private_mode_on else R.string.private_mode_off,
                Toast.LENGTH_LONG
            ).show()
        }
        return on
    }
}
