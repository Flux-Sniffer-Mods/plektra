package rocks.pkb.plektra.inputmethod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddLastWordTest {
    @Test
    fun theWordBeforeTheCursorIsFound() {
        assertEquals("zorbly", lastWordIn("I like zorbly"))
        assertEquals("zorbly", lastWordIn("I like zorbly, "))
        assertEquals("can't", lastWordIn("we can't"))
        assertEquals("well-known", lastWordIn("a well-known"))
        assertEquals("caffè", lastWordIn("un caffè."))
    }

    @Test
    fun quotesAndDashesAroundItAreLeftOut() {
        assertEquals("quoted", lastWordIn("say 'quoted'"))
        assertEquals("word", lastWordIn("—word—"))
    }

    @Test
    fun numbersAndNothingAreNotWords() {
        assertNull(lastWordIn(""))
        assertNull(lastWordIn("   "))
        assertNull(lastWordIn("call 112"))
        assertNull(lastWordIn("!?"))
    }
}
