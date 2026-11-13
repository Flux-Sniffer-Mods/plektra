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

/** The emoji key (Right Shift here), on the real keys. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EmojiKeyTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var controller: ServiceController<PhysicalKeyboardInputMethodService>
    private lateinit var service: PhysicalKeyboardInputMethodService
    private var text = ""
    private var clock = 10_000L
    private val rightShift = KeyEvent.KEYCODE_SHIFT_RIGHT
    private val heldMeta = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON

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

    private fun emojiOfQ(): String {
        val emoji = layers.resolveEmojiLayerSymbol(KeyEvent.KEYCODE_Q, false)
        assertNotNull("Q has an emoji on the emoji layer", emoji)
        return emoji!!
    }

    private fun useRightShift(opensLayer: Boolean = true, sticky: Boolean = false) {
        assertTrue(EmojiKey.setKeyCode(context, rightShift))
        EmojiKey.setOpensLayer(context, opensLayer)
        EmojiKey.setStickyTap(context, sticky)
    }

    @Test
    fun offUntilAKeyIsChosenAndBackedUp() {
        assertEquals(KeyEvent.KEYCODE_UNKNOWN, EmojiKey.keyCode(context))
        press(rightShift)
        assertFalse(layers.isSymActive())
        listOf(EmojiKey.KEY_CODE to PreferenceValueType.INT, EmojiKey.KEY_OPENS_LAYER to PreferenceValueType.BOOLEAN,
            EmojiKey.KEY_STICKY_TAP to PreferenceValueType.BOOLEAN).forEach { (key, type) ->
            assertEquals(key, type, BackupPreferenceContract.expectedExportType("plektra_prefs", key))
        }
    }

    @Test
    fun keysThatTypeOrAreNeededWhileTypingCannotBeChosen() {
        assertFalse(EmojiKey.setKeyCode(context, KeyEvent.KEYCODE_A))
        assertFalse(EmojiKey.setKeyCode(context, KeyEvent.KEYCODE_SPACE))
        assertFalse(EmojiKey.setKeyCode(context, KeyEvent.KEYCODE_1))
        assertFalse(EmojiKey.isAllowed(KeyEvent.KEYCODE_COMMA, isPrintingKey = true))
        assertTrue(EmojiKey.setKeyCode(context, KeyEvent.KEYCODE_SHIFT_RIGHT))
        assertEquals(KeyEvent.KEYCODE_SHIFT_RIGHT, EmojiKey.keyCode(context))
    }

    @Test
    fun aTapOpensAndClosesItsScreen() {
        useRightShift()
        press(rightShift)
        assertTrue(layers.isSymActive())
        press(rightShift)
        assertFalse(layers.isSymActive())
        assertEquals("", text)
    }

    @Test
    fun heldTheNextKeyTypesItsEmojiWithoutShift() {
        useRightShift()
        val emoji = emojiOfQ()
        val start = clock
        service.onKeyDown(rightShift, KeyEvent(start, start, KeyEvent.ACTION_DOWN, rightShift, 0, heldMeta))
        press(KeyEvent.KEYCODE_Q, meta = heldMeta)
        service.onKeyUp(rightShift, KeyEvent(start, clock, KeyEvent.ACTION_UP, rightShift, 0, 0))
        clock += 200L
        press(KeyEvent.KEYCODE_Q)
        assertEquals("The chord opens nothing; Shift doesn't carry over", emoji + "q", text)
        assertFalse(layers.isSymActive())
    }

    @Test
    fun heldWithoutChoosingItJustLetsGo() {
        useRightShift()
        press(rightShift, holdMs = 2_000L)
        assertFalse(layers.isSymActive())
    }

    @Test
    fun tapForOneEmoji() {
        useRightShift(sticky = true)
        val emoji = emojiOfQ()
        press(rightShift)
        assertFalse("A tap waits for the key", layers.isSymActive())
        press(KeyEvent.KEYCODE_Q)
        press(KeyEvent.KEYCODE_Q)
        assertEquals(emoji + "q", text)

        // Twice: the screen opens
        press(rightShift)
        press(rightShift)
        assertTrue(layers.isSymActive())
    }

    @Test
    fun backCancelsATapForOneEmoji() {
        useRightShift(sticky = true)
        press(rightShift)
        assertTrue(service.onKeyDown(KeyEvent.KEYCODE_BACK, KeyEvent(clock, clock, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0, 0)))
        service.onKeyUp(KeyEvent.KEYCODE_BACK, KeyEvent(clock, clock + 30L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0, 0))
        clock += 200L
        press(KeyEvent.KEYCODE_Q)
        assertEquals("q", text)
    }

    private fun press(keyCode: Int, holdMs: Long = 30L, meta: Int = 0) {
        val start = clock
        clock += 200L + holdMs
        service.onKeyDown(keyCode, KeyEvent(start, start, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        service.onKeyUp(keyCode, KeyEvent(start, start + holdMs, KeyEvent.ACTION_UP, keyCode, 0, meta))
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
                this.text = this@EmojiKeyTest.text
                selectionStart = this@EmojiKeyTest.text.length
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
