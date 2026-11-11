package rocks.pkb.plektra.core.suggestions

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FrequentWordLearnerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private fun prefs() = context.getSharedPreferences("frequent_words_test", Context.MODE_PRIVATE)

    @After
    fun tearDown() {
        prefs().edit().clear().commit()
    }

    @Test
    fun aWordTypedThreeTimesIsLearned() {
        val learner = FrequentWordLearner(prefs())
        assertNull(learner.countUse("zorbly"))
        assertNull(learner.countUse("zorbly"))
        assertEquals("zorbly", learner.countUse("zorbly"))
        // Counting starts again afterwards
        assertNull(learner.countUse("zorbly"))
    }

    @Test
    fun countsAreKeptBetweenSessions() {
        FrequentWordLearner(prefs()).apply { countUse("Quibbet"); countUse("quibbet") }
        assertEquals("quibbet", FrequentWordLearner(prefs()).countUse("Quibbet"))
    }

    @Test
    fun aNameAlwaysCapitalisedStaysCapitalised() {
        val learner = FrequentWordLearner(prefs())
        repeat(2) { learner.countUse("Nikkita") }
        assertEquals("Nikkita", learner.countUse("Nikkita"))
    }

    @Test
    fun onlyRealWordsCount() {
        listOf("ok", "abc123", "hello!", "-dash", "a'").forEach { assertFalse(it, FrequentWordLearner.isLearnable(it)) }
        listOf("zorbly", "can't", "well-known", "caffè").forEach { assertTrue(it, FrequentWordLearner.isLearnable(it)) }
    }

    @Test
    fun onlyTheMostRecentlySeenWordsAreTracked() {
        val learner = FrequentWordLearner(prefs(), threshold = 3, maxTracked = 2)
        learner.countUse("alphaword", now = 1L)
        learner.countUse("betaword", now = 2L)
        learner.countUse("gammaword", now = 3L)
        // The oldest was dropped: two more uses don't reach three
        learner.countUse("alphaword", now = 4L)
        assertNull(learner.countUse("alphaword", now = 5L))
        assertNull(learner.countUse("gammaword", now = 6L))
        assertEquals("gammaword", learner.countUse("gammaword", now = 7L))
    }
}
