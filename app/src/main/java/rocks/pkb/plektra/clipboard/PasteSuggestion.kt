package rocks.pkb.plektra.clipboard

/**
 * Paste suggestion: text copied within the last minute is offered as a chip in the next text
 * field. A copy marked sensitive (a password manager's) is offered only in password fields,
 * and there the chip never shows the text.
 */
object PasteSuggestion {
    const val WINDOW_MS = 60_000L
    private const val MAX_LABEL_LENGTH = 24

    /** The chip in a password field: masked, always the same length so it gives nothing away. */
    const val MASKED_LABEL = "⎘ ••••••••"

    /** The chip to offer for [copy] in a new field, or null when there is nothing to offer. */
    fun chipFor(
        copy: RecentCopyTracker.RecentCopy?,
        nowMs: Long,
        passwordField: Boolean,
        inPasswordFields: Boolean
    ): String? {
        if (copy == null || nowMs - copy.timestamp > WINDOW_MS || nowMs < copy.timestamp) return null
        if (passwordField) return if (inPasswordFields) MASKED_LABEL else null
        // A password manager's copy only goes into password fields
        return if (copy.sensitive) null else label(copy.text)
    }

    /** The copied text on one short line, after ⎘ (a text symbol, not an emoji). */
    fun label(text: String): String {
        val oneLine = text.trim().replace(Regex("\\s+"), " ")
        val shown = if (oneLine.length > MAX_LABEL_LENGTH) {
            oneLine.substring(0, cutBefore(oneLine, MAX_LABEL_LENGTH - 1)).trimEnd() + "…"
        } else oneLine
        return "⎘ $shown"
    }

    /** Where to cut [text] at or before [index] without splitting a character (an emoji, an accent). */
    private fun cutBefore(text: String, index: Int): Int {
        val characters = java.text.BreakIterator.getCharacterInstance().apply { setText(text) }
        return if (characters.isBoundary(index)) index else characters.preceding(index).coerceAtLeast(0)
    }
}
