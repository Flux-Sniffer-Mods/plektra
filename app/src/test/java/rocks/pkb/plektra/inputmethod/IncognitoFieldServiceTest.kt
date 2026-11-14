package rocks.pkb.plektra.inputmethod

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.core.IncognitoTyping

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IncognitoFieldServiceTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var controller: ServiceController<PhysicalKeyboardInputMethodService>
    private lateinit var service: PhysicalKeyboardInputMethodService

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        IncognitoTyping.active = false
        controller = Robolectric.buildService(PhysicalKeyboardInputMethodService::class.java).create()
        service = controller.get()
    }

    @After
    fun tearDown() {
        controller.destroy()
        IncognitoTyping.active = false
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    @Test
    fun eachFieldDecidesForItselfWhenItStarts() {
        startInput(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertTrue(IncognitoTyping.active)

        startInput(0)
        assertFalse(IncognitoTyping.active)

        IncognitoTyping.setAlways(context, true)
        startInput(0)
        assertTrue(IncognitoTyping.active)

        IncognitoTyping.setAlways(context, false)
        IncognitoTyping.setFollowsApps(context, false)
        startInput(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertFalse(IncognitoTyping.active)
    }

    private fun startInput(imeOptions: Int) {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            this.imeOptions = imeOptions
            packageName = "incognito.test"
        }
        val ic = mock(InputConnection::class.java)
        setField("mInputConnection", ic)
        setField("mStartedInputConnection", ic)
        setField("mInputEditorInfo", info)
        service.onStartInput(info, false)
    }

    private fun setField(name: String, value: Any?) {
        var type: Class<*>? = service.javaClass
        while (type != null) {
            runCatching { type.getDeclaredField(name) }.getOrNull()?.let { field ->
                field.isAccessible = true
                field.set(service, value)
                return
            }
            type = type.superclass
        }
        error("No field $name")
    }
}
