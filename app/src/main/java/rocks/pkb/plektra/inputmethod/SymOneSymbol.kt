package rocks.pkb.plektra.inputmethod

import android.content.Context
import android.view.KeyEvent
import android.view.ViewConfiguration
import rocks.pkb.plektra.SettingsManager

/**
 * Tap SYM for one symbol (off by default): a tap on SYM makes the next key type its symbol,
 * as if SYM were held, without opening the symbols; a second tap opens them and Back cancels.
 */
class SymOneSymbol {
    companion object {
        const val KEY_ENABLED = "sym_sticky_tap"
        /** Its settings-search link. */
        const val LINK_ID = "modifiers.sym_sticky"

        fun isEnabled(context: Context): Boolean =
            SettingsManager.getPreferences(context).getBoolean(KEY_ENABLED, false)

        fun setEnabled(context: Context, enabled: Boolean) {
            SettingsManager.getPreferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        }

        /** SYM held past the long-press time without choosing a symbol: it just lets go. */
        fun heldTooLong(event: KeyEvent?): Boolean =
            event != null && event.eventTime - event.downTime >= ViewConfiguration.getLongPressTimeout()
    }

    /** Waiting for the key whose symbol to type. */
    var armed: Boolean = false
        private set

    /**
     * SYM tapped alone. Returns true when the tap was taken (the next key types its symbol), or
     * false when SYM should do what it always does (open or close the symbols).
     */
    fun onSymTapped(context: Context, symPage: Int): Boolean {
        if (!armed && symPage == 0 && isEnabled(context)) {
            armed = true
            return true
        }
        armed = false
        return false
    }

    /** The waiting is over: a key was pressed, Back cancelled it, or the field was left. */
    fun disarm(): Boolean {
        val was = armed
        armed = false
        return was
    }
}
