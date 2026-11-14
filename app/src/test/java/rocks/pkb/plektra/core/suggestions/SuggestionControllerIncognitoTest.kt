package rocks.pkb.plektra.core.suggestions

import android.view.KeyEvent
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SuggestionControllerIncognitoTest {
    private lateinit var store: UserNGramStore
    private lateinit var controller: SuggestionController
    private val snapshots = mutableListOf<List<SuggestionResult>>()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        store = UserNGramStore(context).apply { clearAll() }
        val repository = FakeDictionaryRepository().apply { isReady = true }
        controller = SuggestionController(
            context = context,
            assets = context.assets,
            settingsProvider = { SuggestionSettings(suggestionsEnabled = true, maxSuggestions = 3) },
            onSuggestionsUpdated = { snapshots.add(it) },
            currentLocale = Locale.GERMANY,
            dictionaryRepositoryFactory = { _, _, _, _, _ -> repository },
            nextWordPredictorOverride = NextWordPredictor(store)
        )
    }

    @After
    fun tearDown() {
        controller.flushNextWordLearningForTests()
        controller.destroy()
        store.clearAll()
        store.close()
    }

    @Test
    fun incognitoLearnsNoNextWordsButStillOffersWhatWasLearned() {
        type("ich bin ")
        controller.flushNextWordLearningForTests()
        assertEquals(listOf("bin"), store.predict("de-DE", "ich", limit = 3).map { it.word })

        controller.incognito = true
        type("du bist ich ")
        controller.flushNextWordLearningForTests()
        assertTrue(store.predict("de-DE", "du", limit = 3).isEmpty())
        assertEquals(listOf("bin"), store.predict("de-DE", "ich", limit = 3).map { it.word })
        assertEquals("bin", snapshots.last().first().candidate)

        controller.incognito = false
        type("du bist ")
        controller.flushNextWordLearningForTests()
        assertEquals(listOf("bist"), store.predict("de-DE", "du", limit = 3).map { it.word })
    }

    private fun type(text: String) {
        text.forEach { ch ->
            if (ch == ' ') {
                controller.onBoundaryKey(KeyEvent.KEYCODE_SPACE, null, null)
            } else {
                controller.onCharacterCommitted(ch.toString(), null)
            }
        }
    }
}
