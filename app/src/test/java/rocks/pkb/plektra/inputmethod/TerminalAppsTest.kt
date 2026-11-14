package rocks.pkb.plektra.inputmethod

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.inputmethod.statusbar.StatusBarButtonId
import rocks.pkb.plektra.inputmethod.statusbar.StatusBarButtonRegistry

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TerminalAppsTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        SettingsManager.setStatusBarSlotsLeft(context, listOf("microphone", "emoji"))
    }

    @After
    fun tearDown() {
        TerminalApps.onAppInFront(null)
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    private fun shown() = StatusBarButtonRegistry().getEnabledButtons(context).map { it.id }

    @Test
    fun terminalAppsGetNoMicrophoneButton() {
        TerminalApps.onAppInFront("com.whatsapp")
        assertTrue(StatusBarButtonId.Microphone in shown())

        TerminalApps.onAppInFront("com.termux")
        assertFalse(StatusBarButtonId.Microphone in shown())
        assertTrue("Other buttons stay", StatusBarButtonId.Emoji in shown())
    }

    @Test
    fun theKeyboardKnowsWhenATerminalIsInFront() {
        val controller = Robolectric.buildService(PhysicalKeyboardInputMethodService::class.java).create()
        try {
            val service = controller.get()
            service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT; packageName = "org.connectbot" }, false)
            assertTrue(TerminalApps.inTerminal)
            service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT; packageName = "com.example.notes" }, false)
            assertFalse(TerminalApps.inTerminal)
        } finally {
            controller.destroy()
        }
        assertEquals(false, TerminalApps.isTerminal(null))
    }
}
