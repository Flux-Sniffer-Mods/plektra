package rocks.pkb.plektra.inputmethod

import android.os.Looper
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.SettingsManager.SoftwareKeyboardMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SearchBarWaitTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var controller: ServiceController<PhysicalKeyboardInputMethodService>
    private lateinit var service: PhysicalKeyboardInputMethodService
    private var ownShows = 0

    private val searchBar = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_FILTER
    private val message = InputType.TYPE_CLASS_TEXT

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        SettingsManager.setSoftwareKeyboardModeRuntimeOverride(context, SoftwareKeyboardMode.FORCE_HARDWARE)
        controller = Robolectric.buildService(PhysicalKeyboardInputMethodService::class.java).create()
        service = controller.get()
        val visibility = getField(service, "keyboardVisibilityController") as KeyboardVisibilityController
        @Suppress("UNCHECKED_CAST")
        val original = getField(visibility, "requestShowInputView") as () -> Unit
        setField(visibility, "requestShowInputView", { ownShows++; original() })
    }

    @After
    fun tearDown() {
        controller.destroy()
        SettingsManager.setSoftwareKeyboardModeRuntimeOverride(context, null)
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    @Test
    fun searchBarsAreFiltersOrFieldsWhoseActionIsSearch() {
        assertTrue(SearchBarWait.isSearchBar(EditorInfo().apply { inputType = searchBar }))
        assertTrue(SearchBarWait.isSearchBar(EditorInfo().apply { inputType = message; imeOptions = EditorInfo.IME_ACTION_SEARCH }))
        assertFalse(SearchBarWait.isSearchBar(EditorInfo().apply { inputType = message; imeOptions = EditorInfo.IME_ACTION_SEND }))
        assertFalse(SearchBarWait.isSearchBar(EditorInfo().apply { inputType = InputType.TYPE_CLASS_NUMBER; imeOptions = EditorInfo.IME_ACTION_SEARCH }))
        assertFalse(SearchBarWait.isSearchBar(null))
    }

    @Test
    fun offTheBarComesUpInASearchBarAsBefore() {
        startInput(searchBar, "com.example.maps")
        assertEquals(1, ownShows)
    }

    @Test
    fun anAppOpeningOnItsSearchBarWaitsForTheFirstKey() {
        SearchBarWait.setEnabled(context, true)
        startInput(searchBar, "com.example.maps")
        assertEquals("no own show as the app opens", 0, ownShows)

        pressKey(KeyEvent.KEYCODE_A)
        assertEquals("the first key brings it up", 1, ownShows)
    }

    @Test
    fun onlyAsTheAppOpens() {
        SearchBarWait.setEnabled(context, true)
        startInput(message, "com.example.maps")
        assertEquals(1, ownShows)
        // The search bar of an app already in use was gone to on purpose
        startInput(searchBar, "com.example.maps")
        assertEquals(2, ownShows)
        // Plektra's own screens are not an app opening
        startInput(searchBar, context.packageName)
        assertEquals(3, ownShows)
    }

    private fun startInput(inputType: Int, packageName: String) {
        val info = EditorInfo().apply { this.inputType = inputType; this.packageName = packageName }
        val ic = mock(InputConnection::class.java)
        setField(service, "mInputConnection", ic)
        setField(service, "mStartedInputConnection", ic)
        setField(service, "mInputEditorInfo", info)
        service.onStartInput(info, false)
        idle()
        // The window as Android would leave it: closed, so the next show is counted
        service.onWindowHidden()
        idle()
    }

    private fun pressKey(keyCode: Int) {
        service.onKeyDown(keyCode, KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0))
        service.onKeyUp(keyCode, KeyEvent(0L, 50L, KeyEvent.ACTION_UP, keyCode, 0))
        idle()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun getField(owner: Any, name: String): Any? = findField(owner.javaClass, name).get(owner)

    private fun setField(owner: Any, name: String, value: Any?) = findField(owner.javaClass, name).set(owner, value)

    private fun findField(type: Class<*>, name: String): java.lang.reflect.Field {
        var current: Class<*>? = type
        while (current != null) {
            runCatching { return current.getDeclaredField(name).apply { isAccessible = true } }
            current = current.superclass
        }
        error("Field $name not found")
    }
}
