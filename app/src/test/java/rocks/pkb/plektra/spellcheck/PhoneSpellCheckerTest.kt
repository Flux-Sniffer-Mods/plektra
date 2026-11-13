package rocks.pkb.plektra.spellcheck

import android.os.Looper
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.core.suggestions.FakeDictionaryRepository
import rocks.pkb.plektra.core.suggestions.SuggestionController
import rocks.pkb.plektra.core.suggestions.SuggestionResult
import rocks.pkb.plektra.core.suggestions.SuggestionSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PhoneSpellCheckerTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val controllers = mutableListOf<SuggestionController>()

    @After
    fun tearDown() {
        controllers.forEach(SuggestionController::destroy)
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    private fun answer(attributes: Int, vararg suggestions: String) = arrayOf(
        SentenceSuggestionsInfo(arrayOf(SuggestionsInfo(attributes, arrayOf(*suggestions))), intArrayOf(0), intArrayOf(4))
    )

    @Test
    fun offUnlessSwitchedOn() {
        assertFalse(PhoneSpellChecker.isEnabled(context))
        PhoneSpellChecker.setEnabled(context, true)
        assertTrue(PhoneSpellChecker.isEnabled(context))
    }

    @Test
    fun correctionsOnlyForAWordThatLooksMisspelt() {
        assertEquals(listOf("hello", "help"), PhoneSpellChecker.corrections(answer(SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO, "hello", "help")))
        assertNull(PhoneSpellChecker.corrections(answer(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY)))
        assertNull(PhoneSpellChecker.corrections(null))
    }

    @Test
    fun theirCorrectionsComeSecondInTheBar() {
        val repository = FakeDictionaryRepository().apply {
            isReady = true
            addTestEntry("held", 200)
        }
        val shown = mutableListOf<List<SuggestionResult>>()
        val asked = mutableListOf<String>()
        val controller = SuggestionController(
            context = context,
            assets = context.assets,
            settingsProvider = { SuggestionSettings(suggestionsEnabled = true, maxSuggestions = 3) },
            onSuggestionsUpdated = { shown += it },
            currentLocale = Locale.ENGLISH,
            dictionaryRepositoryFactory = { _, _, _, _, _ -> repository },
            externalSuggestions = { word, _, onResult -> asked += word; onResult(listOf("hello", "held")) }
        ).also { controllers += it }

        "helo".forEach { controller.onCharacterCommitted(it.toString(), null) }
        // The dictionary's suggestions come from a background job, then the spell checker's
        val deadline = System.currentTimeMillis() + 10_000L
        while (shown.lastOrNull()?.any { it.candidate == "hello" } != true && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50))
        }

        assertEquals(listOf("helo"), asked.distinct())
        val bar = shown.last().map { it.candidate }
        // The dictionary's own first, theirs next, nothing twice
        assertEquals(listOf("held", "hello"), bar.take(2))
        assertEquals(1, bar.count { it == "held" })
    }
}
