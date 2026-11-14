package rocks.pkb.plektra.core

import android.content.Context
import android.view.inputmethod.EditorInfo
import rocks.pkb.plektra.SettingsManager

/**
 * Incognito typing: while the field being typed in is incognito, nothing is learned from it
 * (next-word predictions and sentence starts, how often dictionary words are used, recent emoji,
 * symbols and kaomoji). What was learned before is still offered. Either every field, or only
 * fields whose app asks keyboards not to learn (private browser tabs, some messengers), which is
 * the default.
 */
object IncognitoTyping {
    const val KEY_ALWAYS = "incognito_always"
    const val KEY_FOLLOW_APPS = "incognito_follow_apps"

    /** Whether the field being typed in is incognito. Set by the input method as each field starts. */
    @Volatile
    var active: Boolean = false

    fun isAlways(context: Context): Boolean =
        SettingsManager.getPreferences(context).getBoolean(KEY_ALWAYS, false)

    fun setAlways(context: Context, enabled: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY_ALWAYS, enabled).apply()
    }

    fun followsApps(context: Context): Boolean =
        SettingsManager.getPreferences(context).getBoolean(KEY_FOLLOW_APPS, true)

    fun setFollowsApps(context: Context, enabled: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY_FOLLOW_APPS, enabled).apply()
    }

    /** Whether typing in a field with these IME options is incognito. */
    fun isIncognitoField(context: Context, imeOptions: Int): Boolean =
        isAlways(context) ||
            (followsApps(context) && imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0)
}
package rocks.pkb.plektra.core

import android.content.Context
import android.view.inputmethod.EditorInfo
import rocks.pkb.plektra.SettingsManager

/**
 * Incognito typing: while the field being typed in is incognito, nothing is learned from it
 * (next-word predictions and sentence starts, how often dictionary words are used, recent emoji,
 * symbols and kaomoji). What was learned before is still offered. Either every field, or only
 * fields whose app asks keyboards not to learn (private browser tabs, some messengers), which is
 * the default.
 */
object IncognitoTyping {
    const val KEY_ALWAYS = "incognito_always"
    const val KEY_FOLLOW_APPS = "incognito_follow_apps"

    @Volatile
    private var fieldIncognito: Boolean = false

    /** Private mode is on (see [PrivateMode]): every field is incognito, whatever it started as. */
    @Volatile
    var privateMode: Boolean = false

    /**
     * Whether the field being typed in is incognito. Set by the input method as each field
     * starts; private mode makes it so in every field, from the moment it is turned on.
     */
    var active: Boolean
        get() = fieldIncognito || privateMode
        set(value) {
            fieldIncognito = value
        }

    fun isAlways(context: Context): Boolean =
        SettingsManager.getPreferences(context).getBoolean(KEY_ALWAYS, false)

    fun setAlways(context: Context, enabled: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY_ALWAYS, enabled).apply()
    }

    fun followsApps(context: Context): Boolean =
        SettingsManager.getPreferences(context).getBoolean(KEY_FOLLOW_APPS, true)

    fun setFollowsApps(context: Context, enabled: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY_FOLLOW_APPS, enabled).apply()
    }

    /** Whether typing in a field with these IME options is incognito. */
    fun isIncognitoField(context: Context, imeOptions: Int): Boolean =
        isAlways(context) ||
            (followsApps(context) && imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0)
}
