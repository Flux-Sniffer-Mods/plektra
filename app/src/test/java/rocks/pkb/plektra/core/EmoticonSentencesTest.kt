package rocks.pkb.plektra.core

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import rocks.pkb.plektra.inputmethod.AutoCapitalizeHelper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EmoticonSentencesTest {
    @After
    fun tearDown() {
        EmoticonSentences.enabled = true
    }

    @Test
    fun emoticonsEndSentences() {
        listOf("Great :)", "ok ;-)", "haha :D", ":P", "sad :'(", "love you <3", "yay ^_^", "lol XD").forEach {
            assertTrue(it, EmoticonSentences.endsWithEmoticon(it))
        }
        listOf("(see above)", "chapter 8)", "http://x", "Note:D", "hello", "a :)b").forEach {
            assertFalse(it, EmoticonSentences.endsWithEmoticon(it))
        }
        EmoticonSentences.enabled = false
        assertFalse(EmoticonSentences.endsWithEmoticon("Great :)"))
    }

    @Test
    fun anEmoticonEndsASentenceForCapitalsAndDoubleSpace() {
        // A capital for the next word once a space follows
        assertTrue(AutoCapitalizeHelper.hasSentenceEndingPunctuation("Great :) ", requireWhitespaceAfter = true))
        assertFalse(AutoCapitalizeHelper.hasSentenceEndingPunctuation("Great :)", requireWhitespaceAfter = true))
        // Double space checks without the space: no full stop goes after it
        assertTrue(AutoCapitalizeHelper.hasSentenceEndingPunctuation("Great :)", requireWhitespaceAfter = false))
        // A bracket closing words is not one
        assertFalse(AutoCapitalizeHelper.hasSentenceEndingPunctuation("(see above) ", requireWhitespaceAfter = true))

        EmoticonSentences.enabled = false
        assertFalse(AutoCapitalizeHelper.hasSentenceEndingPunctuation("Great :) ", requireWhitespaceAfter = true))
    }
}
