package rocks.pkb.plektra.inputmethod

import android.content.Context
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import rocks.pkb.plektra.SettingsManager

/**
 * Terminal mode (off by default), for the terminal apps in [TerminalApps]: a terminal tells input
 * methods it has no text field (TYPE_NULL), so the keyboard would leave Alt and SYM to the app.
 * Treated instead as a visible-password text field, the same thing Termux's own
 * "enforce-char-based-input" option asks for, the keyboard types its Alt and SYM symbols into
 * it, with no suggestions, corrections or automatic capitals. Ctrl goes to the terminal as a real
 * Ctrl, and the keys a terminal reads itself (Enter, Backspace, Tab, Esc, arrows) as they are.
 */
object TerminalMode {
    const val KEY_ENABLED = "terminal_mode_enabled"
    /** Its settings-search link. */
    const val LINK_ID = "modifiers.terminal_mode"

    const val INPUT_TYPE = InputType.TYPE_CLASS_TEXT or
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

    fun isEnabled(context: Context): Boolean =
        SettingsManager.getPreferences(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Whether [inputType] is a terminal's "no text field". */
    fun isTerminalView(inputType: Int): Boolean =
        inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_NULL

    /**
     * For a terminal app's field while terminal mode is on: rewrites it in place, as the keyboard
     * sees it. True when the field is the terminal's (rewritten now or before).
     */
    fun apply(context: Context, info: EditorInfo?): Boolean {
        if (info == null || !isEnabled(context) || !TerminalApps.isTerminal(info.packageName)) return false
        if (!isTerminalView(info.inputType)) return info.inputType == INPUT_TYPE
        info.inputType = INPUT_TYPE
        return true
    }

    /** Keys a terminal reads itself: sent as pressed unless Alt or SYM is typing a symbol. */
    fun isTerminalKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DEL,
        KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_PAGE_DOWN,
        KeyEvent.KEYCODE_MOVE_HOME, KeyEvent.KEYCODE_MOVE_END, KeyEvent.KEYCODE_INSERT -> true
        in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> true
        else -> false
    }
}

/**
 * The keys terminal mode sends on as they are: Ctrl combos as real Ctrl keys, and the
 * terminal's own keys. A key sent on has its release sent the same way.
 */
class TerminalKeys(private val connection: () -> InputConnection?) {
    // Pressed key -> (key sent, meta state sent)
    private val ctrlSent = mutableMapOf<Int, Pair<Int, Int>>()
    // Terminal keys (Enter, Backspace, arrows...) passed to the terminal as pressed
    private val rawDown = mutableSetOf<Int>()

    /**
     * Any Ctrl (held, tapped or latched; [ctrl]) sends the key as a real Ctrl combo, with Alt
     * and Shift if they are held too, so Ctrl+C and Ctrl+D reach the shell instead of the
     * keyboard's copy and cursor keys. True when it was sent.
     */
    fun sendCtrlCombo(event: KeyEvent, ctrl: Boolean, alt: Boolean, shift: Boolean): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val keyCode = event.keyCode
        if (KeyEvent.isModifierKey(keyCode) || keyCode == SYM_KEYCODE || keyCode == KeyEvent.KEYCODE_BACK) return false
        if (event.repeatCount > 0) {
            if (keyCode !in ctrlSent) return false
        } else {
            if (!ctrl) return false
            var meta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
            if (alt) meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
            if (shift) meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
            ctrlSent[keyCode] = keyCode to meta
        }
        send(event, KeyEvent.ACTION_DOWN)
        return true
    }

    /** A terminal key pressed as it is: the keyboard leaves it to the app. */
    fun passRaw(keyCode: Int) {
        rawDown += keyCode
    }

    /**
     * A key coming up: [Release.SENT] when its Ctrl combo's release was sent, [Release.RAW] when
     * it goes to the app as it is, or [Release.NONE].
     */
    fun onKeyUp(event: KeyEvent?): Release {
        val keyCode = event?.keyCode ?: return Release.NONE
        if (keyCode in ctrlSent) {
            send(event, KeyEvent.ACTION_UP)
            return Release.SENT
        }
        return if (rawDown.remove(keyCode)) Release.RAW else Release.NONE
    }

    enum class Release { NONE, SENT, RAW }

    fun reset() {
        ctrlSent.clear()
        rawDown.clear()
    }

    private fun send(source: KeyEvent, action: Int) {
        val (keyCode, meta) = (if (action == KeyEvent.ACTION_UP) ctrlSent.remove(source.keyCode) else ctrlSent[source.keyCode]) ?: return
        connection()?.sendKeyEvent(
            KeyEvent(
                source.downTime, source.eventTime, action, keyCode,
                if (action == KeyEvent.ACTION_DOWN) source.repeatCount else 0, meta,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }

    private companion object {
        const val SYM_KEYCODE = 63
    }
}
