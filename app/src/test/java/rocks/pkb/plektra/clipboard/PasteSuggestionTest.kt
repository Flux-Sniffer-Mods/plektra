package rocks.pkb.plektra.clipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PasteSuggestionTest {
    @Test
    fun shortTextIsShownWhole() {
        assertEquals("⎘ hello world", PasteSuggestion.label("  hello\n world "))
    }

    @Test
    fun longTextIsCutOnOneLine() {
        val label = PasteSuggestion.label("https://example.com/a/very/long/path/that/goes/on")
        assertEquals("⎘ https://example.com/a/v…", label)
    }

    @Test
    fun aLongLabelIsNeverCutInsideAnEmoji() {
        assertEquals("\u2398 " + "a".repeat(22) + "…", PasteSuggestion.label("a".repeat(22) + "😀😀"))
        assertEquals("\u2398 " + "a".repeat(19) + "🇬🇧…", PasteSuggestion.label("a".repeat(19) + "🇬🇧🇬🇧"))
        assertEquals("\u2398 " + "a".repeat(21) + "…", PasteSuggestion.label("a".repeat(21) + "🇬🇧🇬🇧"))
    }

    @Test
    fun thePasswordFieldLabelGivesNothingAway() {
        assertEquals("\u2398 \u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022", PasteSuggestion.MASKED_LABEL)
    }

    @Test
    fun aRecentCopyIsOfferedForAMinuteAndAPasswordManagersOnlyMaskedInPasswordFields() {
        val copy = RecentCopyTracker.RecentCopy(timestamp = 1_000L, text = "hello")
        assertEquals("⎘ hello", PasteSuggestion.chipFor(copy, 60_000L, passwordField = false, inPasswordFields = true))
        assertNull(PasteSuggestion.chipFor(copy, 62_000L, passwordField = false, inPasswordFields = true))
        assertNull(PasteSuggestion.chipFor(null, 1_000L, passwordField = false, inPasswordFields = true))

        val secret = copy.copy(sensitive = true)
        assertNull(PasteSuggestion.chipFor(secret, 2_000L, passwordField = false, inPasswordFields = true))
        assertEquals(PasteSuggestion.MASKED_LABEL, PasteSuggestion.chipFor(secret, 2_000L, passwordField = true, inPasswordFields = true))
        assertEquals(PasteSuggestion.MASKED_LABEL, PasteSuggestion.chipFor(copy, 2_000L, passwordField = true, inPasswordFields = true))
        assertNull(PasteSuggestion.chipFor(secret, 2_000L, passwordField = true, inPasswordFields = false))
    }
}
