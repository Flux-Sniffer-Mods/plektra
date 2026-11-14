package rocks.pkb.plektra.inputmethod

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LanguagePerAppTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private var controller: ServiceController<PhysicalKeyboardInputMethodService>? = null

    private val german = InputMethodSubtype.InputMethodSubtypeBuilder()
        .setLanguageTag("de-DE")
        .setSubtypeMode("keyboard")
        .setSubtypeExtraValue("KeyboardLayout=qwertz")
        .build()

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        LanguagePerApp.setEnabled(context, false)
    }

    @After
    fun tearDown() {
        controller?.destroy()
        LanguagePerApp.setEnabled(context, false)
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    @Test
    fun offUnlessSwitchedOnAndTurningItOffForgetsEveryApp() {
        assertFalse(LanguagePerApp.isEnabled(context))
        LanguagePerApp.setEnabled(context, true)
        LanguagePerApp.remember(context, "com.example.chat", "de-DE|")
        assertEquals("de-DE|", LanguagePerApp.languageFor(context, "com.example.chat"))

        LanguagePerApp.setEnabled(context, false)
        assertNull(LanguagePerApp.languageFor(context, "com.example.chat"))
    }

    @Test
    fun aLanguageIsKeptWithItsInputStyle() {
        assertEquals("de-DE|KeyboardLayout=qwertz", SubtypeCycler.subtypeKey(german))
        assertEquals("en-US|", SubtypeCycler.subtypeKey("en-US", null))
    }

    @Test
    fun aLanguageChosenInAnAppBecomesThatAppsLanguage() {
        val service = startService("com.example.chat")
        subtypeChanged(service, german)
        assertNull("off: nothing is remembered", LanguagePerApp.languageFor(context, "com.example.chat"))

        LanguagePerApp.setEnabled(context, true)
        subtypeChanged(service, german)
        assertEquals("de-DE|KeyboardLayout=qwertz", LanguagePerApp.languageFor(context, "com.example.chat"))
    }

    @Test
    fun theKeyboardsOwnSettingsAreNotAnApp() {
        LanguagePerApp.setEnabled(context, true)
        val service = startService(context.packageName)
        subtypeChanged(service, german)
        assertNull(LanguagePerApp.languageFor(context, context.packageName))
        assertTrue(LanguagePerApp.isEnabled(context))
    }

    // Android calls it; protected, so through reflection here
    private fun subtypeChanged(service: PhysicalKeyboardInputMethodService, subtype: InputMethodSubtype) {
        PhysicalKeyboardInputMethodService::class.java
            .getDeclaredMethod("onCurrentInputMethodSubtypeChanged", InputMethodSubtype::class.java)
            .apply { isAccessible = true }
            .invoke(service, subtype)
    }

    private fun startService(packageName: String): PhysicalKeyboardInputMethodService {
        val built = Robolectric.buildService(PhysicalKeyboardInputMethodService::class.java).create()
        controller = built
        val service = built.get()
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            this.packageName = packageName
        }
        var type: Class<*>? = service.javaClass
        while (type != null) {
            runCatching { type.getDeclaredField("mInputEditorInfo") }.getOrNull()?.let {
                it.isAccessible = true
                it.set(service, info)
                return service
            }
            type = type.superclass
        }
        error("No editor info field")
    }
}
