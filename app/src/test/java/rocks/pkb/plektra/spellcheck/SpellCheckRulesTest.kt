package rocks.pkb.plektra.spellcheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpellCheckRulesTest {
    @Test
    fun ordinaryWordsAreChecked() {
        listOf("hello", "Helo", "don't", "caffè", "end.").forEach { assertTrue(it, SpellCheckRules.shouldCheck(it)) }
    }

    @Test
    fun codesLinksAndNamesAreLeftAlone() {
        listOf("a", "B2B", "NASA", "@pastiera", "#tbt", "example.com", "http://x", "snake_case", "42", "--")
            .forEach { assertFalse(it, SpellCheckRules.shouldCheck(it)) }
    }

    @Test
    fun apostropheWordsAreCheckedAsWhatTheyreMadeFrom() {
        fun forms(word: String) = SpellCheckRules.apostropheForms(word)
        assertTrue("will" in forms("won't"))
        assertTrue("will" in forms("Won’t"))
        assertTrue("can" in forms("can't"))
        assertTrue("could" in forms("couldn't"))
        assertTrue("homme" in forms("l'homme"))
        assertTrue("anno" in forms("dell'anno"))
        assertFalse("re" in forms("thye're"))
    }

    @Test
    fun everyDictionaryIsALanguageAndTheManifestListsThemAll() {
        val bundled = java.io.File("src/main/assets/common/dictionaries_serialized").list().orEmpty()
            .map { it.substringBefore('_') }.toSet()
        assertTrue(bundled.isNotEmpty())
        // Plain JVM test: the catalog's file names read without Android's JSON
        val catalog = java.io.File("src/main/assets/common/dictionary_catalog.json").readText()
        val offered = Regex("\"filename\"\\s*:\\s*\"([a-z]+)_").findAll(catalog).map { it.groupValues[1] }.toSet()
        assertEquals(offered + bundled, SpellCheckRules.LANGUAGES)
        // The languages Android offers the spell checker for are the same
        val xml = java.io.File("src/main/res/xml/spellchecker.xml").readText()
        val listed = Regex("subtypeLocale=\"([a-z]+)\"").findAll(xml).map { it.groupValues[1] }.toSet()
        assertEquals(SpellCheckRules.LANGUAGES, listed)
    }
}
