package rocks.pkb.plektra.core

import android.view.inputmethod.EditorInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.core.suggestions.UserDictionaryStore
import rocks.pkb.plektra.data.emoji.RecentEmojiManager
import rocks.pkb.plektra.data.symbols.Kaomoji
import rocks.pkb.plektra.data.symbols.SymbolSearch

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IncognitoTypingTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val privateField = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or EditorInfo.IME_ACTION_SEND

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        RecentEmojiManager.clearRecentEmojis(context)
        IncognitoTyping.active = false
    }

    @After
    fun tearDown() {
        IncognitoTyping.active = false
        RecentEmojiManager.clearRecentEmojis(context)
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    @Test
    fun byDefaultOnlyFieldsThatAskAreIncognito() {
        assertFalse(IncognitoTyping.isAlways(context))
        assertTrue(IncognitoTyping.followsApps(context))
        assertTrue(IncognitoTyping.isIncognitoField(context, privateField))
        assertFalse(IncognitoTyping.isIncognitoField(context, EditorInfo.IME_ACTION_SEND))
    }

    @Test
    fun alwaysCoversEveryFieldAndFollowingAppsCanBeTurnedOff() {
        IncognitoTyping.setFollowsApps(context, false)
        assertFalse(IncognitoTyping.isIncognitoField(context, privateField))

        IncognitoTyping.setAlways(context, true)
        assertTrue(IncognitoTyping.isIncognitoField(context, 0))
        assertTrue(IncognitoTyping.isIncognitoField(context, privateField))
    }

    @Test
    fun incognitoLeavesRecentEmojiSymbolsAndKaomojiAsTheyWere() {
        assertTrue(RecentEmojiManager.addRecentEmoji(context, "🙂"))
        SymbolSearch.recents.add(context, "→")
        Kaomoji.recents.add(context, "(^_^)")

        IncognitoTyping.active = true
        assertFalse(RecentEmojiManager.addRecentEmoji(context, "🎉"))
        SymbolSearch.recents.add(context, "×")
        Kaomoji.recents.add(context, "(T_T)")

        assertEquals(listOf("🙂"), RecentEmojiManager.getRecentEmojis(context))
        assertEquals("→", SymbolSearch.recents.get(context).first())
        assertFalse("×" in SymbolSearch.recents.get(context))
        assertEquals("(^_^)", Kaomoji.recents.get(context).first())
        assertFalse("(T_T)" in Kaomoji.recents.get(context))

        IncognitoTyping.active = false
        assertTrue(RecentEmojiManager.addRecentEmoji(context, "🎉"))
        assertEquals(listOf("🎉", "🙂"), RecentEmojiManager.getRecentEmojis(context))
    }

    @Test
    fun incognitoDoesNotCountUsesOfDictionaryWords() {
        // The store's entries are shared by every instance, so this test looks only at its own word
        val word = "Qwertzuiop"
        fun entry(store: UserDictionaryStore) = store.getSnapshot().first { it.word == word }
        val store = UserDictionaryStore()
        store.addWord(context, word)
        val before = entry(store)

        IncognitoTyping.active = true
        store.markUsed(context, word)
        assertEquals(before, entry(store))
        assertEquals(before.frequency, UserDictionaryStore().also { it.loadUserEntries(context) }.let(::entry).frequency)

        IncognitoTyping.active = false
        store.markUsed(context, word)
        assertEquals(before.frequency + 1, entry(store).frequency)
        store.removeWord(context, word)
    }
}
