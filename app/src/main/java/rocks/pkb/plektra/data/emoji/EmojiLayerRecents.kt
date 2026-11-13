package rocks.pkb.plektra.data.emoji

import android.content.Context
import android.view.KeyEvent
import rocks.pkb.plektra.SettingsManager

/**
 * Recent emoji first on the emoji layer (option, off by default): the emoji you used last take
 * the layer's first keys, and its own emoji move along by as many.
 *
 * "Used" counts every emoji you type, from the picker or from the layer itself, kept in a list of
 * its own, so an emoji already on the layer that you use a lot moves to the front instead of
 * showing twice. Only the classic layer: the emoji pages have their own Recents.
 */
object EmojiLayerRecents {
    const val KEY_ENABLED = "emoji_layer_recents_first"
    const val KEY_COUNT = "emoji_layer_recents_count"
    const val KEY_USED = "emoji_layer_used"
    /** Its settings-search link. */
    const val LINK_ID = "sym.emoji_layer_recents_first"
    private const val MAX_USED = 40

    /** The layer's keys in reading order: the recent emoji start on Q. */
    private val LAYER_KEYS = listOf(
        KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_T,
        KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_P,
        KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G,
        KeyEvent.KEYCODE_H, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
        KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_B,
        KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_M
    )

    fun enabled(context: Context): Boolean =
        SettingsManager.getPreferences(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** How many keys the recent emoji take (1 to 10, 5 by default). */
    fun count(context: Context): Int =
        SettingsManager.getPreferences(context).getInt(KEY_COUNT, 5).coerceIn(1, 10)

    fun setCount(context: Context, count: Int) {
        SettingsManager.getPreferences(context).edit().putInt(KEY_COUNT, count.coerceIn(1, 10)).apply()
    }

    /** Every emoji used, newest first. */
    fun used(context: Context): List<String> =
        SettingsManager.getPreferences(context).getString(KEY_USED, null)
            ?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()

    /** An emoji was typed. Nothing is kept while the option is off. */
    fun markUsed(context: Context, emoji: String) {
        if (emoji.isBlank() || !enabled(context)) return
        val updated = (listOf(emoji) + used(context).filter { it != emoji }).take(MAX_USED)
        SettingsManager.getPreferences(context).edit().putString(KEY_USED, updated.joinToString("\n")).apply()
    }

    /** The layer's [mapped] emoji, with the recent ones first while the option is on. */
    fun layer(context: Context, mapped: Map<Int, String>): Map<Int, String> =
        if (enabled(context)) arrange(mapped, used(context), count(context)) else mapped

    /**
     * The layer's keys with the recent emoji first: the keys that have an emoji, in reading order,
     * get the [recent] ones, then the layer's own [mapped] emoji in their key order, without any
     * already shown. Keys without an emoji stay empty, so the layer keeps its shape.
     */
    fun arrange(mapped: Map<Int, String>, recent: List<String>, count: Int): Map<Int, String> {
        val keys = LAYER_KEYS.filter { it in mapped } + mapped.keys.filter { it !in LAYER_KEYS }.sorted()
        val front = recent.distinct().take(count)
        val rest = keys.mapNotNull { mapped[it] }.filter { it !in front }
        return keys.zip(front + rest).toMap()
    }
}
