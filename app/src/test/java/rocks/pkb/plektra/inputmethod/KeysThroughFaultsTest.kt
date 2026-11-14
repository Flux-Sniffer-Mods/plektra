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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import rocks.pkb.plektra.BackgroundFailures
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.core.InputContextState

/** The keyboard keeps going through a fault in handling a key, a field or another thread. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KeysThroughFaultsTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var controller: ServiceController<PhysicalKeyboardInputMethodService>
    private lateinit var service: PhysicalKeyboardInputMethodService
    private var text = ""
    private var clock = 10_000L
    private val editorInfo = EditorInfo().apply {
        inputType = InputType.TYPE_CLASS_TEXT
        packageName = "rocks.pkb.plektra.test"
    }

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        controller = Robolectric.buildService(PhysicalKeyboardInputMethodService::class.java).create()
        service = controller.get()
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

    @Test
    fun aKeyThatCannotBeHandledGoesToTheAppAndTypingCarriesOn() {
        val filter = getField<Any>("accidentalKeyPressFilter")
        val bounce = getField<Any>("bounceKeyFilter")
        // A fault in handling every key: down, long press and up
        setField("accidentalKeyPressFilter", null)
        setField("bounceKeyFilter", null)
        val down = KeyEvent(clock, clock, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Q, 0, 0)
        assertFalse("Passed on as a plain key", service.onKeyDown(KeyEvent.KEYCODE_Q, down))
        service.onKeyLongPress(KeyEvent.KEYCODE_Q, down)
        service.onKeyUp(KeyEvent.KEYCODE_Q, KeyEvent(clock, clock + 30L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Q, 0, 0))
        assertEquals("", text)

        setField("accidentalKeyPressFilter", filter)
        setField("bounceKeyFilter", bounce)
        press(KeyEvent.KEYCODE_Q)
        assertEquals("q", text)
    }

    @Test
    fun aFieldThatCannotBeSetUpIsLoggedNotFatal() {
        val bounce = getField<Any>("bounceKeyFilter")
        setField("bounceKeyFilter", null)
        service.onStartInput(editorInfo, false)
        service.onUpdateSelection(0, 0, 1, 1, -1, -1)
        setField("bounceKeyFilter", bounce)
        service.onStartInput(editorInfo, false)
        setField("mInputConnection", connection())
        press(KeyEvent.KEYCODE_Q)
        assertEquals("q", text)
    }

    @Test
    fun aBackgroundThreadsFailureEndsThatThreadAlone() {
        val ended = mutableListOf<Thread>()
        val handler = BackgroundFailures.handler { thread, _ -> ended += thread }
        val worker = Thread {}
        handler.uncaughtException(worker, IllegalStateException("download failed"))
        assertTrue("Logged, not passed on to end the app", ended.isEmpty())

        // The main thread's failures and running out of memory end it as before
        handler.uncaughtException(android.os.Looper.getMainLooper().thread, IllegalStateException("main"))
        handler.uncaughtException(worker, OutOfMemoryError())
        assertEquals(2, ended.size)
    }

    private fun press(keyCode: Int) {
        val start = clock
        clock += 200L
        service.onKeyDown(keyCode, KeyEvent(start, start, KeyEvent.ACTION_DOWN, keyCode, 0, 0))
        service.onKeyUp(keyCode, KeyEvent(start, start + 30L, KeyEvent.ACTION_UP, keyCode, 0, 0))
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
                this.text = this@KeysThroughFaultsTest.text
                selectionStart = this@KeysThroughFaultsTest.text.length
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
