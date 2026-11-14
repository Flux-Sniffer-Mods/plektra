package rocks.pkb.plektra.inputmethod.expansion

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dynamic parts of a snippet, filled in when it expands: {date}, {time}, {datetime}, {isodate},
 * {day} and {clipboard}. Anything else in braces stays as written.
 */
object SnippetPlaceholders {
    private val PLACEHOLDER = Regex("""\{(date|time|datetime|isodate|day|clipboard)\}""")

    fun hasPlaceholders(text: String): Boolean = PLACEHOLDER.containsMatchIn(text)

    /**
     * [text] with its placeholders replaced: dates and times as [locale] writes them at [now],
     * {isodate} as yyyy-MM-dd, and {clipboard} with [clipboard]'s text (empty when there is none).
     * The clipboard is read only when the text asks for it.
     */
    fun fill(
        text: String,
        now: Date = Date(),
        locale: Locale = Locale.getDefault(),
        clipboard: () -> String? = { null }
    ): String {
        if (!hasPlaceholders(text)) return text
        val clip by lazy(LazyThreadSafetyMode.NONE) { clipboard().orEmpty() }
        return PLACEHOLDER.replace(text) { match ->
            when (match.groupValues[1]) {
                "date" -> DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(now)
                "time" -> DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(now)
                "datetime" -> DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(now)
                "isodate" -> SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(now)
                "day" -> SimpleDateFormat("EEEE", locale).format(now)
                else -> clip
            }
        }
    }
}
