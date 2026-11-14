package rocks.pkb.plektra.shortcuts

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import rocks.pkb.plektra.SettingsManager

/**
 * Apps' shortcuts on keys (off by default): in any app, Ctrl + Alt + 1 to 4 open the app's own
 * launcher shortcuts (the ones its icon shows on a long press), in its order, and Ctrl + , its
 * settings screen. On a keyboard without a number row, the keys whose Alt character is 1 to 4.
 */
object AppActionKeys {
    const val KEY_ENABLED = "app_action_keys"
    /** Its settings-search link. */
    const val LINK_ID = "quick_launcher.app_action_keys"
    private const val TAG = "PlektraAppActionKeys"

    fun isEnabled(context: Context): Boolean =
        SettingsManager.getPreferences(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    sealed class Target {
        /** The app's launcher shortcut at [index] (0 to 3). */
        data class Shortcut(val index: Int) : Target()
        data object Settings : Target()
    }

    /** What a key with [ctrl] and [alt] held opens; [altChar] is the key's Alt character, if any. */
    fun targetFor(keyCode: Int, ctrl: Boolean, alt: Boolean, altChar: Char?): Target? {
        if (!ctrl) return null
        if (alt) {
            val digit = when (keyCode) {
                in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_4 -> keyCode - KeyEvent.KEYCODE_1 + 1
                else -> altChar?.takeIf { it in '1'..'4' }?.digitToInt()
            } ?: return null
            return Target.Shortcut(digit - 1)
        }
        return if (keyCode == KeyEvent.KEYCODE_COMMA) Target.Settings else null
    }

    /** Opens [target] of [packageName]; false when the app has none there (the key goes on). */
    fun open(context: Context, packageName: String, target: Target): Boolean {
        val actions = AppActionDiscovery.discover(context, packageName)
        val action = when (target) {
            is Target.Shortcut -> actions.launcherShortcuts.getOrNull(target.index)
            Target.Settings -> actions.settings
        } ?: return false
        val intent = AppActionDiscovery.intentFor(context, packageName, action) ?: return false
        return try {
            context.startActivity(intent)
            true
        } catch (error: Exception) {
            Log.w(TAG, "$packageName: app shortcut not opened", error)
            false
        }
    }
}
