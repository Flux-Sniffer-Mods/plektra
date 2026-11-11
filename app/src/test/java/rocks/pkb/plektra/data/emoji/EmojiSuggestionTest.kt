package rocks.pkb.plektra.data.emoji

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.inputmethod.suggestions.EmojiSuggestion

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "en")
class EmojiSuggestionTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        // A font that draws every emoji, so the index holds them all
        TestEmojiFont.install(context)
        EmojiSearchRepository.clearCache()
    }

    private fun index() = runBlocking { EmojiSearchRepository.getSearchIndex(context) }

    @Test
    fun aNameMatchComesFirstAndTheIndexIsThenAtHandWithoutWaiting() {
        val index = index()
        assertEquals("🍕", EmojiSearchRepository.suggestionFor(index, "pizza"))
        assertEquals("🚀", EmojiSearchRepository.suggestionFor(index, "Rocket"))
        assertNotNull(EmojiSearchRepository.cachedSearchIndex(context))
    }

    @Test
    fun aKeywordGivesTheFirstMatchingEmojiInPickerOrder() {
        val happy = EmojiSearchRepository.suggestionFor(index(), "happy")
        assertNotNull(happy)
        assertTrue(EmojiSuggestion.isEmoji(happy!!))
    }

    @Test
    fun genericShortAndUnknownWordsGetNothing() {
        val index = index()
        listOf("time", "up", "button", "the", "a", "zzzqx").forEach {
            assertNull(it, EmojiSearchRepository.suggestionFor(index, it))
        }
    }

    @Test
    fun emojiAreToldApartFromWords() {
        assertTrue(EmojiSuggestion.isEmoji("😊"))
        assertTrue(EmojiSuggestion.isEmoji("❤️"))
        assertFalse(EmojiSuggestion.isEmoji("pizza"))
        assertFalse(EmojiSuggestion.isEmoji("1️⃣"))
        assertFalse(EmojiSuggestion.isEmoji("—"))
    }

    @Test
    fun theEmojiGoesAfterTheWordWithASpaceEachSide() {
        val typed = StringBuilder("I love pizza")
        val connection = object : android.view.inputmethod.BaseInputConnection(android.view.View(context), true) {
            override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence = typed.takeLast(n).toString()
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean { typed.append(text); return true }
        }

        assertTrue(EmojiSuggestion.commitAfterWord(connection, "🍕"))

        assertEquals("I love pizza 🍕 ", typed.toString())
    }
}
