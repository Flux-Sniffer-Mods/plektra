package rocks.pkb.plektra.inputmethod

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import rocks.pkb.plektra.SettingsManager

/**
 * Search bars wait for typing: when an app opens on its search bar, already focused, Plektra
 * doesn't bring up its bar or keyboard by itself; the first key, a tap on the field or the app
 * asking Android to show the keyboard does. Off unless switched on.
 */
object SearchBarWait {
    const val KEY = "search_bar_waits_for_typing"

    fun isEnabled(context: Context): Boolean = SettingsManager.getPreferences(context).getBoolean(KEY, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY, enabled).apply()
    }

    /** A search bar: a filter field, or a text field whose action is Search. */
    fun isSearchBar(info: EditorInfo?): Boolean {
        info ?: return false
        if (info.inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        if (info.inputType and InputType.TYPE_MASK_VARIATION == InputType.TYPE_TEXT_VARIATION_FILTER) return true
        return info.imeOptions and EditorInfo.IME_MASK_ACTION == EditorInfo.IME_ACTION_SEARCH
    }
}
