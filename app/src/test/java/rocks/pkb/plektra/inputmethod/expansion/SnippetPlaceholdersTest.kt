package rocks.pkb.plektra.inputmethod.expansion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale

class SnippetPlaceholdersTest {
    // Wednesday 4 March 2026, 09:05 in the JVM's time zone
    private val now = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.MARCH, 4, 9, 5)
    }.time

    @Test
    fun isoDateAndDayFollowTheirOwnFormats() {
        assertEquals(
            "Sent 2026-03-04 (Wednesday)",
            SnippetPlaceholders.fill("Sent {isodate} ({day})", now, Locale.UK)
        )
        assertEquals("Mittwoch", SnippetPlaceholders.fill("{day}", now, Locale.GERMANY))
    }

    @Test
    fun dateAndTimeAreWrittenTheWayTheLocaleWritesThem() {
        assertEquals(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.UK).format(now), SnippetPlaceholders.fill("{date}", now, Locale.UK))
        assertEquals(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.UK).format(now), SnippetPlaceholders.fill("{time}", now, Locale.UK))
        assertEquals(
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).format(now),
            SnippetPlaceholders.fill("{datetime}", now, Locale.US)
        )
    }

    @Test
    fun clipboardIsReadOnceAndOnlyWhenTheSnippetAsksForIt() {
        var reads = 0
        val clipboard = { reads++; "link" }

        assertEquals("a link, link", SnippetPlaceholders.fill("a {clipboard}, {clipboard}", now, Locale.UK, clipboard))
        SnippetPlaceholders.fill("No placeholders", now, Locale.UK, clipboard)
        SnippetPlaceholders.fill("{isodate}", now, Locale.UK, clipboard)

        assertEquals(1, reads)
    }

    @Test
    fun clipboardTextIsInsertedAsItIs() {
        // Replacement syntax and placeholders inside the clipboard text are not interpreted
        assertEquals("\$1 \\ {date}", SnippetPlaceholders.fill("{clipboard}", now, Locale.UK) { "\$1 \\ {date}" })
        assertEquals("[]", SnippetPlaceholders.fill("[{clipboard}]", now, Locale.UK) { null })
    }

    @Test
    fun otherBracesStayAsWritten() {
        val text = "{name}, {} and {DATE}"
        assertFalse(SnippetPlaceholders.hasPlaceholders(text))
        assertEquals(text, SnippetPlaceholders.fill(text, now, Locale.UK))
        assertTrue(SnippetPlaceholders.hasPlaceholders("Today: {date}"))
    }
}
