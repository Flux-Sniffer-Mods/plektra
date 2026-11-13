package rocks.pkb.plektra.inputmethod

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
import rocks.pkb.plektra.core.InputContextState
import rocks.pkb.plektra.core.SymLayoutController

/** Tap SYM for one symbol, on the real keys. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SymOneSymbolTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var controller: ServiceController<PhysicalKeyboardInputMethodService>
    private lateinit var service: PhysicalKeyboardInputMethodService
    private var text = ""
    private var clock = 10_000L
    private val sym = 63

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        controller = Robolectric.buildService(PhysicalKeyboardInputMethodService::class.java).create()
        service = controller.get()
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            packageName = "rocks.pkb.plektra.test"
        }
        val connection = connection()
        setField("mInputConnection", connection)
        setField("mStartedInputConnection", connection)
        setField("mInputEditorInfo", editorInfo)
        setField("isInputViewActive", true)
        setField("inputContextState", InputContextState.fromEditorInfo(editorInfo))
    }

    @After
    fun tearDown() {
        controller.destroy()
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    private val layers get() = getField<SymLayoutController>("symLayoutController")

    private fun symbolOfQ(): String {
        val symbol = layers.resolveChordSymbol(KeyEvent.KEYCODE_Q, false)
        assertNotNull("Q has a symbol on the SYM layer", symbol)
        return symbol!!
    }

    @Test
    fun offByDefaultAndBackedUp() {
        assertFalse(SymOneSymbol.isEnabled(context))
        assertEquals(PreferenceValueType.BOOLEAN, BackupPreferenceContract.expectedExportType("plektra_prefs", SymOneSymbol.KEY_ENABLED))
        // Off: a tap on SYM opens the symbols, as before
        press(sym)
        assertTrue(layers.isSymActive())
    }

    @Test
    fun aTapMakesTheNextKeyTypeItsSymbol() {
        SymOneSymbol.setEnabled(context, true)
        val symbol = symbolOfQ()
        press(sym)
        assertFalse("The symbols stay closed", layers.isSymActive())
        press(KeyEvent.KEYCODE_Q)
        press(KeyEvent.KEYCODE_Q)
        assertEquals("Only the next key", symbol + "q", text)
    }

    @Test
    fun aSecondTapOpensTheSymbols() {
        SymOneSymbol.setEnabled(context, true)
        press(sym)
        press(sym)
        assertTrue(layers.isSymActive())
        assertEquals("", text)
    }

    @Test
    fun backCancelsIt() {
        SymOneSymbol.setEnabled(context, true)
        press(sym)
        assertTrue(service.onKeyDown(KeyEvent.KEYCODE_BACK, KeyEvent(clock, clock, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0, 0)))
        service.onKeyUp(KeyEvent.KEYCODE_BACK, KeyEvent(clock, clock + 30L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0, 0))
        clock += 200L
        press(KeyEvent.KEYCODE_Q)
        assertEquals("q", text)
    }

    @Test
    fun heldWithoutChoosingItJustLetsGo() {
        SymOneSymbol.setEnabled(context, true)
        press(sym, holdMs = 2_000L)
        assertFalse(layers.isSymActive())
        press(KeyEvent.KEYCODE_Q)
        assertEquals("q", text)
    }

    @Test
    fun holdingSymWorksAsBefore() {
        SymOneSymbol.setEnabled(context, true)
        val symbol = symbolOfQ()
        val start = clock
        service.onKeyDown(sym, KeyEvent(start, start, KeyEvent.ACTION_DOWN, sym, 0, 0))
        press(KeyEvent.KEYCODE_Q)
        service.onKeyUp(sym, KeyEvent(start, clock, KeyEvent.ACTION_UP, sym, 0, 0))
        clock += 200L
        press(KeyEvent.KEYCODE_Q)
        assertEquals(symbol + "q", text)
        assertFalse(layers.isSymActive())
    }

    private fun press(keyCode: Int, holdMs: Long = 30L) {
        val start = clock
        clock += 200L + holdMs
        service.onKeyDown(keyCode, KeyEvent(start, start, KeyEvent.ACTION_DOWN, keyCode, 0, 0))
        service.onKeyUp(keyCode, KeyEvent(start, start + holdMs, KeyEvent.ACTION_UP, keyCode, 0, 0))
    }

    private fun connection(): InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java)
    ) { _, method, args ->
        when (method.name) {
            "commitText" -> {
                text += args?.getOrNull(0)?.toString().orEmpty()
                true
            }
            "deleteSurroundingText" -> {
                val before = (args?.getOrNull(0) as? Int) ?: 0
                text = text.dropLast(before.coerceAtMost(text.length))
                true
            }
            "getTextBeforeCursor" -> text.takeLast((args?.getOrNull(0) as? Int) ?: text.length)
            "getTextAfterCursor" -> ""
            "getExtractedText" -> ExtractedText().apply {
                this.text = this@SymOneSymbolTest.text
                selectionStart = this@SymOneSymbolTest.text.length
                selectionEnd = selectionStart
            }
            "beginBatchEdit", "endBatchEdit", "finishComposingText", "sendKeyEvent" -> true
            else -> when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                else -> null
            }
        }
    } as InputConnection

    @Suppress("UNCHECKED_CAST")
    private fun <T> getField(name: String): T = field(name).get(service) as T

    private fun setField(name: String, value: Any?) = field(name).set(service, value)

    private fun field(name: String): java.lang.reflect.Field {
        var type: Class<*>? = service.javaClass
        while (type != null) {
            runCatching { return type.getDeclaredField(name).apply { isAccessible = true } }
            type = type.superclass
        }
        error("No field $name")
    }
}
