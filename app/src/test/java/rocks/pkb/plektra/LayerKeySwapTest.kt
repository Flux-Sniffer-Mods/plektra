package rocks.pkb.plektra

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.inputmethod.StatusBarController

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LayerKeySwapTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun twoMappedKeysSwap() {
        assertEquals(mapOf(1 to "b", 2 to "a", 3 to "c"), swapKeys(mapOf(1 to "a", 2 to "b", 3 to "c"), 1, 2))
    }

    @Test
    fun anEmptyKeyTakesTheOthersAndLeavesItEmpty() {
        assertEquals(mapOf(2 to "a"), swapKeys(mapOf(1 to "a"), 1, 2))
        assertEquals(mapOf(1 to "a"), swapKeys(mapOf(2 to "a"), 1, 2))
    }

    private fun keys(view: View): List<View> = when {
        view.hasOnClickListeners() -> listOf(view)
        view is ViewGroup -> (0 until view.childCount).flatMap { keys(view.getChildAt(it)) }
        else -> emptyList()
    }

    @Test
    fun theEditorsKeysCanBeHeldToDragOnlyWhenSwappingIsOffered() {
        val bar = StatusBarController(context, StatusBarController.Mode.CANDIDATES_ONLY)
        val mappings = mapOf(KeyEvent.KEYCODE_Q to "😀", KeyEvent.KEYCODE_W to "🐱")
        val plain = keys(bar.createCustomizableEmojiKeyboard(mappings, { _, _ -> }))
        assertTrue(plain.isNotEmpty())
        assertFalse(plain.any { it.isLongClickable })

        val swapping = keys(bar.createCustomizableEmojiKeyboard(mappings, { _, _ -> }, onSwap = { _, _ -> }))
        assertTrue(swapping.isNotEmpty())
        assertTrue(swapping.all { it.isLongClickable })

        // The keys that turn the layer's pages stay put
        val paged = keys(bar.createCustomizableEmojiKeyboard(
            mappings, { _, _ -> }, navigationKeys = mapOf(KeyEvent.KEYCODE_Q to "‹"), onSwap = { _, _ -> }
        ))
        assertEquals(swapping.size - 1, paged.count { it.isLongClickable })
    }
}
