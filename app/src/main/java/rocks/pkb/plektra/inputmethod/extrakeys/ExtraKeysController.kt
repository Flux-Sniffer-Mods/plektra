package rocks.pkb.plektra.inputmethod.extrakeys

import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/**
 * The extra keys row's state: open or not, for which app, which of its modifiers are latched,
 * and what its keys and the physical top-row letters do while it's open.
 */
class ExtraKeysController(
    private val connection: () -> InputConnection?,
    private val packageName: () -> String?,
    /** A terminal app: the row shows the terminal set. */
    private val inTerminal: () -> Boolean,
    /** Shows [ExtraKeysRow], or puts the bar back with null; then redraws the bar. */
    private val render: (ExtraKeysRow?) -> Unit
) {
    var isOpen = false
        private set
    private var openFor: String? = null
    private var shown: List<ExtraKey> = emptyList()
    private val latched = mutableSetOf<ExtraKey>()
    // Physical keys pressed while the row was open, whose release is the row's too
    private val keysDown = mutableSetOf<Int>()

    fun toggle() = setOpen(!isOpen)

    fun setOpen(open: Boolean) {
        if (open && connection() == null) return
        isOpen = open
        openFor = if (open) packageName() else null
        if (!open) latched.clear()
        refresh()
    }

    /** A new field: the row stays open in the same app, and closes in another. */
    fun onFieldStarted(packageName: String?) {
        if (!isOpen) return
        if (packageName != openFor) setOpen(false) else refresh()
    }

    fun refresh() {
        if (!isOpen) {
            shown = emptyList()
            render(null)
            return
        }
        shown = if (inTerminal()) ExtraKeySets.TERMINAL else ExtraKeySets.TEXT
        render(ExtraKeysRow(shown, latched.toSet(), ::press) { setOpen(false) })
    }

    private fun meta(): Int {
        var meta = 0
        if (ExtraKey.CTRL in latched) meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (ExtraKey.ALT in latched) meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        return meta
    }

    /** A key on the row, tapped or pressed through its top-row letter. */
    fun press(key: ExtraKey) {
        val ic = connection() ?: return
        if (key.isModifier) {
            if (!latched.remove(key)) latched += key
            refresh()
            return
        }
        when {
            key.editAction != 0 -> ic.performContextMenuAction(key.editAction)
            key.text != null -> ic.commitText(key.text, 1)
            else -> send(ic, key.keyCode, meta())
        }
        if (latched.isNotEmpty()) {
            latched.clear()
            refresh()
        }
    }

    private fun send(ic: InputConnection, keyCode: Int, meta: Int) {
        val now = SystemClock.uptimeMillis()
        val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
    }

    /**
     * A physical key going down while the row is open: Back closes it; the top row of letters
     * presses its keys (Q the first, P the tenth); with the row's Ctrl or Alt latched, the next
     * key is that key with the modifier, so Ctrl then C is Ctrl+C. True when the row took it.
     */
    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!isOpen || event == null) return false
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.repeatCount == 0) setOpen(false)
            return true
        }
        if (KeyEvent.isModifierKey(keyCode) || keyCode == KeyEvent.KEYCODE_SYM || keyCode == SYM_KEYCODE) return false
        val ic = connection() ?: return false
        if (event.repeatCount > 0) {
            if (keyCode !in keysDown) return false
            // Arrows and Delete repeat while held; the rest press once
            val key = shown.getOrNull(ExtraKeySets.indexForPhysicalKey(keyCode))
            if (key != null && key.keyCode != KeyEvent.KEYCODE_UNKNOWN && !key.isModifier && latched.isEmpty()) {
                send(ic, key.keyCode, 0)
            }
            return true
        }
        if (latched.isNotEmpty()) {
            send(ic, keyCode, meta() or (event.metaState and KeyEvent.META_SHIFT_MASK))
            latched.clear()
            refresh()
            keysDown += keyCode
            return true
        }
        val key = shown.getOrNull(ExtraKeySets.indexForPhysicalKey(keyCode)) ?: return false
        press(key)
        keysDown += keyCode
        return true
    }

    /** A physical key coming up: true when its press was the row's. */
    fun onKeyUp(keyCode: Int): Boolean = keysDown.remove(keyCode)

    private companion object {
        const val SYM_KEYCODE = 63
    }
}
