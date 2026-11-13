package rocks.pkb.plektra.core

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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SuggestionSwipeLearningTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val learning = SuggestionSwipeLearning

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        learning.resetForTest()
    }

    @After
    fun tearDown() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        learning.resetForTest()
    }

    @Test
    fun offUntilSwitchedOnAndSwipesAlwaysPickByDefault() {
        assertFalse(learning.enabled(context))
        learning.onPicked(context, now = 1_000L)
        learning.onDeleted(context, now = 1_500L)
        assertEquals(1f, learning.scale(context))
        assertTrue(learning.swipesPick(context, now = 3_600_000L))
    }

    @Test
    fun aPickUndoneAtOnceMakesSwipesNeedMore() {
        learning.setEnabled(context, true)
        learning.onPicked(context, now = 1_000L)
        learning.onDeleted(context, now = 2_000L)
        assertEquals(1.08f, learning.scale(context), 0.001f)

        // A Backspace long after a pick says nothing about the pick
        learning.onPicked(context, now = 10_000L)
        learning.onDeleted(context, now = 20_000L)
        assertEquals(1.08f, learning.scale(context), 0.001f)
    }

    @Test
    fun aSwipeThatFellShortBeforeAPickMakesSwipesNeedLess() {
        learning.setEnabled(context, true)
        learning.onMissed(reached = 70f, needed = 100f, now = 1_000L)
        learning.onPicked(context, now = 2_000L)
        assertEquals(0.95f, learning.scale(context), 0.001f)

        // A swipe that went nowhere near isn't a pick that fell short
        learning.onMissed(reached = 20f, needed = 100f, now = 5_000L)
        learning.onPicked(context, now = 5_500L)
        assertEquals(0.95f, learning.scale(context), 0.001f)
    }

    @Test
    fun theScaleStaysWithinBoundsAndCanBeForgotten() {
        learning.setEnabled(context, true)
        var now = 0L
        repeat(40) {
            now += 10_000L
            learning.onPicked(context, now)
            learning.onDeleted(context, now + 500L)
        }
        assertEquals(1.8f, learning.scale(context), 0.001f)
        learning.resetLearned(context)
        assertEquals(1f, learning.scale(context))
        learning.setEnabled(context, false)
        assertEquals(1f, learning.scale(context))
    }

    @Test
    fun afterAPauseInTypingASwipeScrolls() {
        learning.setIdleSeconds(context, 10)
        learning.onTyped(now = 1_000L)
        assertTrue(learning.swipesPick(context, now = 9_000L))
        assertFalse(learning.swipesPick(context, now = 12_000L))
        learning.onTyped(now = 12_500L)
        assertTrue(learning.swipesPick(context, now = 13_000L))
    }
}
