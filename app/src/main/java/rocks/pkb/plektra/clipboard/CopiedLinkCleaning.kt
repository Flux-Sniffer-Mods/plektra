package rocks.pkb.plektra.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import rocks.pkb.plektra.SettingsManager

/**
 * Clean copied links: a link copied while the keyboard is running loses its tracking on the
 * clipboard itself, so it is clean wherever it is pasted (the keyboard's clipboard history, an
 * app's own Paste, Ctrl+V). Off unless switched on.
 */
object CopiedLinkCleaning {
    const val KEY = "clean_copied_links"

    /** Longer copies are left alone: a link is rarely more, and text that long is not a link. */
    private const val MAX_LENGTH = 4_000

    fun isEnabled(context: Context): Boolean =
        SettingsManager.getPreferences(context).getBoolean(KEY, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY, enabled).apply()
    }

    /**
     * Replaces the clipboard's text with its cleaned links when the setting is on. Only a single
     * plain-text item is replaced, or formatted text that is nothing but a link; never a copy
     * marked sensitive (password managers), a file or an intent. Returns whether the clipboard
     * was replaced: the clean copy then arrives as a clipboard change of its own.
     */
    fun cleanClipboard(context: Context, clipboard: ClipboardManager): Boolean {
        if (!isEnabled(context)) return false
        val clip = runCatching { clipboard.primaryClip }.getOrNull() ?: return false
        val cleaned = cleanedText(clip) ?: return false
        return runCatching {
            clipboard.setPrimaryClip(ClipData.newPlainText(clip.description?.label, cleaned))
        }.isSuccess
    }

    /** [clip]'s text with its links cleaned, or null when the clip is to be left as it is. */
    internal fun cleanedText(clip: ClipData): String? {
        if (clip.itemCount != 1) return null
        val description = clip.description ?: return null
        // ClipDescription.EXTRA_IS_SENSITIVE, as ClipboardHistoryManager reads it on every Android version
        if (description.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true) return null
        val item = clip.getItemAt(0) ?: return null
        if (item.uri != null || item.intent != null) return null
        val text = item.text?.toString() ?: return null
        if (text.length > MAX_LENGTH || !text.contains("://")) return null
        val justALink = LinkCleaner.URL.matchEntire(text.trim()) != null
        val plain = description.mimeTypeCount == 1 &&
            description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) && item.htmlText == null
        if (!plain && !justALink) return null
        return LinkCleaner.clean(text).takeIf { it != text }
    }
}
