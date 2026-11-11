package rocks.pkb.plektra.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.util.Log

/**
 * The last text copied while the keyboard runs, for the paste suggestion. It listens to the
 * clipboard on its own, whether or not clipboard history is on, and keeps the copy in memory
 * only: a password manager's copy never reaches the clipboard history from here.
 */
class RecentCopyTracker(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis
) : ClipboardManager.OnPrimaryClipChangedListener {
    /** [sensitive]: marked so by the app it came from (a password manager). */
    data class RecentCopy(val timestamp: Long, val text: String, val sensitive: Boolean = false) {
        // Never the text itself in logs
        override fun toString() = "RecentCopy(timestamp=$timestamp, sensitive=$sensitive)"
    }

    private val clipboard = context.getSystemService(ClipboardManager::class.java)

    @Volatile
    var recentCopy: RecentCopy? = null
        private set

    fun start() {
        clipboard?.addPrimaryClipChangedListener(this)
    }

    fun stop() {
        clipboard?.removePrimaryClipChangedListener(this)
        recentCopy = null
    }

    /** Forget the copy once it has been offered and used or dismissed: it is offered once. */
    fun consume() {
        recentCopy = null
    }

    override fun onPrimaryClipChanged() {
        recentCopy = null
        try {
            val clip = clipboard?.primaryClip ?: return
            if (clip.itemCount == 0 || clip.description?.hasMimeType("text/*") == false) return
            val text = clip.getItemAt(0)?.coerceToText(context)?.toString()
            if (text.isNullOrBlank()) return
            val sensitive = clip.description?.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true
            recentCopy = RecentCopy(clock(), text, sensitive)
        } catch (e: RuntimeException) {
            // A clip that can't be read costs the suggestion, never the keyboard
            Log.w(TAG, "Copy not read for the paste suggestion: $e")
        }
    }

    private companion object {
        const val TAG = "RecentCopyTracker"
    }
}
