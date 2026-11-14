package rocks.pkb.plektra.spellcheck

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.core.suggestions.FakeDictionaryRepository
import rocks.pkb.plektra.core.suggestions.SuggestionController
import rocks.pkb.plektra.core.suggestions.SuggestionSettings

/** The keyboard's loaded dictionary, as the spell checker asks it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KeyboardSpellCheckTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val repository = FakeDictionaryRepository().apply {
        isReady = true
        addTestEntry("hello", 200)
        addTestEntry("help", 150)
    }
    private val controller = SuggestionController(
        context = context,
        assets = context.assets,
        settingsProvider = { SuggestionSettings() },
        onSuggestionsUpdated = {},
        currentLocale = Locale.ENGLISH,
        dictionaryRepositoryFactory = { _, _, _, _, _ -> repository }
    )

    @After
    fun tearDown() = controller.destroy()

    @Test
    fun aKnownWordIsKnownAndAMisspeltOneGetsSuggestions() {
        assertEquals(SuggestionController.SpellCheckResult(true, emptyList()), controller.spellCheck("en", "hello", 3))
        val misspelt = controller.spellCheck("en", "helo", 3)!!
        assertEquals(false, misspelt.known)
        assertTrue(misspelt.suggestions.toString(), "hello" in misspelt.suggestions)
    }

    @Test
    fun anotherLanguageIsLeftToTheSpellCheckersOwnDictionary() {
        assertNull(controller.spellCheck("de", "hello", 3))
        repository.isReady = false
        assertNull(controller.spellCheck("en", "hello", 3))
    }
}
