package rocks.pkb.plektra.inputmethod

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
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
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.backup.BackupPreferenceContract
import rocks.pkb.plektra.backup.PreferenceValueType

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TerminalModeTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var controller: ServiceController<PhysicalKeyboardInputMethodService>
    private lateinit var service: PhysicalKeyboardInputMethodService
    private val sent = mutableListOf<KeyEvent>()
    private val typed = mutableListOf<String>()
    private val connection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader, arrayOf(InputConnection::class.java)
    ) { _, method, args ->
        when (method.name) {
            "sendKeyEvent" -> sent += args!![0] as KeyEvent
            "commitText" -> typed += args!![0].toString()
        }
        when (method.returnType) { java.lang.Boolean.TYPE -> true; java.lang.Integer.TYPE -> 0; else -> null }
    } as InputConnection

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        controller = Robolectric.buildService(PhysicalKeyboardInputMethodService::class.java).create()
        service = controller.get()
        listOf("mInputConnection", "mStartedInputConnection").forEach { name ->
            android.inputmethodservice.InputMethodService::class.java.getDeclaredField(name)
                .apply { isAccessible = true }.set(service, connection)
        }
    }

    @After
    fun tearDown() {
        controller.destroy()
        TerminalApps.onAppInFront(null)
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    private fun termux() = EditorInfo().apply { inputType = InputType.TYPE_NULL; packageName = "com.termux" }

    private fun start(info: EditorInfo) {
        android.inputmethodservice.InputMethodService::class.java.getDeclaredField("mInputEditorInfo")
            .apply { isAccessible = true }.set(service, info)
        service.onStartInput(info, false)
    }

    @Test
    fun offByDefaultLeavingTheTerminalAsItIs() {
        val info = termux()
        start(info)
        assertEquals(InputType.TYPE_NULL, info.inputType)
        assertEquals(PreferenceValueType.BOOLEAN, BackupPreferenceContract.expectedExportType("plektra_prefs", TerminalMode.KEY_ENABLED))
    }

    @Test
    fun onATerminalIsTypedIntoAsATextFieldWithoutSuggestions() {
        TerminalMode.setEnabled(context, true)
        val info = termux()
        start(info)
        assertEquals(TerminalMode.INPUT_TYPE, info.inputType)

        // Another app's "no text field" is left alone
        val other = EditorInfo().apply { inputType = InputType.TYPE_NULL; packageName = "com.example.game" }
        start(other)
        assertEquals(InputType.TYPE_NULL, other.inputType)
    }

    @Test
    fun ctrlReachesTheTerminalAsARealCtrl() {
        TerminalMode.setEnabled(context, true)
        start(termux())
        val ctrlMeta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        assertTrue(service.onKeyDown(KeyEvent.KEYCODE_C, KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C, 0, ctrlMeta)))
        assertTrue(service.onKeyUp(KeyEvent.KEYCODE_C, KeyEvent(0, 30, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_C, 0, ctrlMeta)))
        assertEquals(listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP), sent.map { it.action })
        assertTrue(sent.all { it.keyCode == KeyEvent.KEYCODE_C && it.isCtrlPressed })
        assertTrue("Nothing typed", typed.isEmpty())
    }

    @Test
    fun theTerminalsOwnKeysGoAsTheyAre() {
        TerminalMode.setEnabled(context, true)
        start(termux())
        service.onKeyDown(KeyEvent.KEYCODE_ENTER, KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, 0))
        service.onKeyUp(KeyEvent.KEYCODE_ENTER, KeyEvent(0, 30, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0, 0))
        assertTrue("Not sent as a Ctrl combo", sent.none { it.isCtrlPressed })
        assertFalse(typed.contains("\n"))
    }
}
