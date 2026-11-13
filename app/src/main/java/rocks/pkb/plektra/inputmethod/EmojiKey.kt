package rocks.pkb.plektra.inputmethod

import android.content.Context
import android.view.KeyEvent
import android.view.ViewConfiguration
import rocks.pkb.plektra.SettingsManager

/**
 * The emoji key (off until a key is chosen): a key of the user's choice that, in a text field,
 * opens or closes the emoji picker or the emoji layer. A modifier key (Right Shift, say) does it
 * when released, so it still works in chords; held, the next key types its emoji from the emoji
 * layer. With "tap for one emoji", a tap makes the next key do the same and a second tap opens.
 */
class EmojiKey {
    companion object {
        const val KEY_CODE = "emoji_picker_key"
        const val KEY_OPENS_LAYER = "emoji_key_opens_layer"
        const val KEY_STICKY_TAP = "emoji_sticky_tap"
        /** Its settings-search link. */
        const val LINK_ID = "sym.emoji_key"
        const val STICKY_LINK_ID = "sym.emoji_sticky"

        /** Keys that type text or are needed while typing can't be the emoji key. */
        private val DENYLIST = setOf(
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_SYM
        )

        fun isAllowed(keyCode: Int, isPrintingKey: Boolean = false): Boolean =
            keyCode > KeyEvent.KEYCODE_UNKNOWN &&
                !isPrintingKey &&
                keyCode !in DENYLIST &&
                keyCode !in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 &&
                keyCode !in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z

        /** The chosen key, or KEYCODE_UNKNOWN while there is none (the default). */
        fun keyCode(context: Context): Int {
            val key = SettingsManager.getPreferences(context).getInt(KEY_CODE, KeyEvent.KEYCODE_UNKNOWN)
            return if (key == KeyEvent.KEYCODE_UNKNOWN || isAllowed(key)) key else KeyEvent.KEYCODE_UNKNOWN
        }

        /** Stores [keyCode] (KEYCODE_UNKNOWN turns it off); false when it isn't allowed. */
        fun setKeyCode(context: Context, keyCode: Int): Boolean {
            if (keyCode != KeyEvent.KEYCODE_UNKNOWN && !isAllowed(keyCode)) return false
            SettingsManager.getPreferences(context).edit().putInt(KEY_CODE, keyCode).apply()
            return true
        }

        fun opensLayer(context: Context): Boolean =
            SettingsManager.getPreferences(context).getBoolean(KEY_OPENS_LAYER, false)

        fun setOpensLayer(context: Context, layer: Boolean) {
            SettingsManager.getPreferences(context).edit().putBoolean(KEY_OPENS_LAYER, layer).apply()
        }

        fun stickyTap(context: Context): Boolean =
            SettingsManager.getPreferences(context).getBoolean(KEY_STICKY_TAP, false)

        fun setStickyTap(context: Context, enabled: Boolean) {
            SettingsManager.getPreferences(context).edit().putBoolean(KEY_STICKY_TAP, enabled).apply()
        }

        private fun heldTooLong(event: KeyEvent?): Boolean =
            event != null && event.eventTime - event.downTime >= ViewConfiguration.getLongPressTimeout()
    }

    /** The emoji key held down (its key code), or KEYCODE_UNKNOWN. */
    var heldKey: Int = KeyEvent.KEYCODE_UNKNOWN
        private set
    /** Another key was pressed while the emoji key was held. */
    private var chorded = false
    /** Tapped for one emoji: waiting for the key. */
    var armed: Boolean = false
        private set

    /** What a press of the emoji key does now. */
    enum class Press { NONE, OPEN }

    /**
     * The emoji key went down. A key that isn't a modifier opens at once ([Press.OPEN]); a
     * modifier waits for its release. [withCtrlOrAlt]: part of a chord from the start.
     */
    fun onKeyDown(keyCode: Int, event: KeyEvent?, withCtrlOrAlt: Boolean): Press {
        val first = (event?.repeatCount ?: 0) == 0
        heldKey = keyCode
        if (!first) return Press.NONE
        if (KeyEvent.isModifierKey(keyCode)) {
            chorded = withCtrlOrAlt
            return Press.NONE
        }
        armed = false
        return Press.OPEN
    }

    /** Another key went down while the emoji key was held: it is a chord now. */
    fun onOtherKeyDown() {
        if (heldKey != KeyEvent.KEYCODE_UNKNOWN) chorded = true
    }

    /**
     * Whether the key going down now types its emoji from the layer: the emoji key (a modifier)
     * held without Ctrl or Alt, or tapped for one emoji. The tap is used up either way.
     */
    fun typesEmoji(withCtrlOrAlt: Boolean): Boolean {
        val held = heldKey != KeyEvent.KEYCODE_UNKNOWN && KeyEvent.isModifierKey(heldKey) && !withCtrlOrAlt
        val wasArmed = armed
        armed = false
        return held || wasArmed
    }

    /**
     * The emoji key came up. Returns [Press.OPEN] when a modifier's tap opens the screen; a tap
     * arms "one emoji" instead when that's on and nothing is open ([screenOpen]).
     */
    fun onKeyUp(context: Context, keyCode: Int, event: KeyEvent?, screenOpen: Boolean): Press {
        if (keyCode != heldKey) return Press.NONE
        heldKey = KeyEvent.KEYCODE_UNKNOWN
        val wasChorded = chorded
        chorded = false
        // Held past a tap without choosing anything: the emoji key just lets go
        if (!KeyEvent.isModifierKey(keyCode) || wasChorded || heldTooLong(event)) return Press.NONE
        if (!armed && !screenOpen && stickyTap(context)) {
            armed = true
            return Press.NONE
        }
        armed = false
        return Press.OPEN
    }

    /** Whether the emoji key is down (its release is the emoji key's). */
    fun isHeld(keyCode: Int): Boolean = heldKey != KeyEvent.KEYCODE_UNKNOWN && keyCode == heldKey

    /** Ends a waiting tap (Back, or leaving the field); true when one was waiting. */
    fun disarm(): Boolean {
        val was = armed
        armed = false
        return was
    }

    fun reset() {
        heldKey = KeyEvent.KEYCODE_UNKNOWN
        chorded = false
        armed = false
    }
}
