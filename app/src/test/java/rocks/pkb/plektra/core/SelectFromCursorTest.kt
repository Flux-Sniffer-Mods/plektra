package rocks.pkb.plektra.core

import android.view.KeyEvent
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.inputmethod.TextSelectionHelper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SelectFromCursorTest {
    private val context = RuntimeEnvironment.getApplication()
    private val modifiers = ModifierStateController(300L)
    private val navMode = NavModeController(context, modifiers)
    private val sent = mutableListOf<KeyEvent>()
    private val recording = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java)
    ) { _, method, args ->
        when (method.name) {
            "sendKeyEvent" -> { sent += args!![0] as KeyEvent; true }
            else -> if (method.returnType == Boolean::class.javaPrimitiveType) false else null
        }
    } as InputConnection

    @After
    fun tearDown() {
        navMode.exitNavMode()
        SelectFromCursor.stop()
    }

    @Test
    fun startingTurnsNavModeOnAndItsMovesCarryShift() {
        navMode.startSelectingFromCursor()

        assertTrue(navMode.isNavModeActive())
        assertTrue(SelectFromCursor.active)
        assertTrue(navMode.executeMapping("keycode", "DPAD_LEFT", null, recording))
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT), sent.map { it.keyCode })
        assertTrue(sent.all { it.isShiftPressed })

        // Keys that don't move the cursor are sent as they are
        sent.clear()
        navMode.executeMapping("keycode", "TAB", null, recording)
        assertTrue(sent.none { it.isShiftPressed })
    }

    @Test
    fun leavingNavModeEndsTheSelecting() {
        navMode.startSelectingFromCursor()
        navMode.exitNavMode()

        assertFalse(SelectFromCursor.active)
        navMode.executeMapping("keycode", "DPAD_LEFT", null, recording)
        assertTrue(sent.none { it.isShiftPressed })
    }

    @Test
    fun navModeEndingAnyOtherWayEndsItToo() {
        navMode.startSelectingFromCursor()

        // A Ctrl tap or a field change clears the Nav Mode latch without exitNavMode
        modifiers.clearCtrlState()

        assertFalse(SelectFromCursor.active)
    }

    @Test
    fun cursorMovesExtendTheSelectionWhileSelecting() {
        val inputConnection = mock(InputConnection::class.java)
        `when`(inputConnection.getExtractedText(any(), anyInt())).thenReturn(ExtractedText().apply {
            text = "hello world"
            selectionStart = 5
            selectionEnd = 5
        })
        navMode.startSelectingFromCursor()

        assertTrue(TextSelectionHelper.moveCursorLeft(inputConnection))

        verify(inputConnection).setSelection(4, 5)
    }
}
