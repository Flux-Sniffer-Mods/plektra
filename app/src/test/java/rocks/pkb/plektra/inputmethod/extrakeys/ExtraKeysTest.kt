package rocks.pkb.plektra.inputmethod.extrakeys

import android.os.Looper
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.inputmethod.PhysicalKeyboardInputMethodService
import rocks.pkb.plektra.inputmethod.TerminalApps

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExtraKeysTest {
    private val sent = mutableListOf<KeyEvent>()
    private val typed = mutableListOf<String>()
    private val edits = mutableListOf<Int>()
    private var row: ExtraKeysRow? = null
    private var app = "com.example.notes"
    private var terminal = false

    private val connection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader, arrayOf(InputConnection::class.java)
    ) { _, method, args ->
        when (method.name) {
            "sendKeyEvent" -> sent += args!![0] as KeyEvent
            "commitText" -> typed += args!![0].toString()
            "performContextMenuAction" -> edits += args!![0] as Int
        }
        when (method.returnType) { java.lang.Boolean.TYPE -> true; java.lang.Integer.TYPE -> 0; else -> null }
    } as InputConnection

    private val controller = ExtraKeysController({ connection }, { app }, { terminal }) { row = it }

    @After
    fun tearDown() {
        TerminalApps.onAppInFront(null)
        SettingsManager.getPreferences(RuntimeEnvironment.getApplication()).edit().clear().commit()
    }

    private fun down(keyCode: Int, meta: Int = 0, repeat: Int = 0) =
        controller.onKeyDown(keyCode, KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, repeat, meta))

    private fun downs() = sent.filter { it.action == KeyEvent.ACTION_DOWN }

    @Test
    fun opensWithTheSetForTheApp() {
        controller.toggle()
        assertEquals(ExtraKeySets.TEXT, row!!.keys)
        controller.toggle()
        assertNull(row)

        terminal = true
        controller.toggle()
        assertEquals(ExtraKeySets.TERMINAL, row!!.keys)
    }

    @Test
    fun itsKeysSendKeysTypeAndEdit() {
        controller.toggle()
        controller.press(ExtraKey.LEFT)
        controller.press(ExtraKey.COPY)
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, downs().single().keyCode)
        assertEquals(listOf(android.R.id.copy), edits)

        terminal = true
        controller.refresh()
        controller.press(ExtraKey.SLASH)
        assertEquals(listOf("/"), typed)
    }

    @Test
    fun theTopRowPressesTheRowsKeysAndTheirReleaseIsTheRows() {
        controller.toggle()
        // Q is the first key (←), P the tenth (Paste)
        assertTrue(down(KeyEvent.KEYCODE_Q))
        assertTrue(down(KeyEvent.KEYCODE_P))
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, downs().single().keyCode)
        assertEquals(listOf(android.R.id.paste), edits)
        assertTrue(controller.onKeyUp(KeyEvent.KEYCODE_Q))
        assertFalse("Only once", controller.onKeyUp(KeyEvent.KEYCODE_Q))
        // A held arrow repeats
        down(KeyEvent.KEYCODE_Q)
        down(KeyEvent.KEYCODE_Q, repeat = 1)
        assertEquals(3, downs().size)
        // Other keys type as usual
        assertFalse(down(KeyEvent.KEYCODE_A))
    }

    @Test
    fun aLatchedCtrlGoesWithTheNextKey() {
        terminal = true
        controller.toggle()
        controller.press(ExtraKey.CTRL)
        assertEquals(setOf(ExtraKey.CTRL), row!!.latched)
        assertTrue(down(KeyEvent.KEYCODE_C))
        val ctrlC = downs().single()
        assertEquals(KeyEvent.KEYCODE_C, ctrlC.keyCode)
        assertTrue(ctrlC.isCtrlPressed)
        assertTrue("Used up", row!!.latched.isEmpty())
    }

    @Test
    fun backClosesItAndAnotherAppDoesToo() {
        controller.toggle()
        assertTrue(down(KeyEvent.KEYCODE_BACK))
        assertNull(row)
        assertFalse("Closed: Back is the app's again", down(KeyEvent.KEYCODE_BACK))

        controller.toggle()
        controller.onFieldStarted(app)
        assertTrue(controller.isOpen)
        controller.onFieldStarted("com.example.other")
        assertFalse(controller.isOpen)
    }

    @Test
    fun theKeyboardOpensItFromItsCommand() {
        val service = Robolectric.buildService(PhysicalKeyboardInputMethodService::class.java).create()
        try {
            val ime = service.get()
            listOf("mInputConnection", "mStartedInputConnection").forEach { name ->
                android.inputmethodservice.InputMethodService::class.java.getDeclaredField(name)
                    .apply { isAccessible = true }.set(ime, connection)
            }
            ime.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT; packageName = "com.example.notes" }, false)
            assertTrue(ExtraKeysToggle.toggle())
            shadowOf(Looper.getMainLooper()).idle()
            val field = PhysicalKeyboardInputMethodService::class.java.getDeclaredField("extraKeys").apply { isAccessible = true }
            assertTrue((field.get(ime) as ExtraKeysController).isOpen)
        } finally {
            service.destroy()
        }
        assertFalse("Gone with the keyboard", ExtraKeysToggle.toggle())
    }
}
