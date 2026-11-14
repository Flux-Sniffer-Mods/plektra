package rocks.pkb.plektra.inputmethod

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.backup.BackupPreferenceContract
import rocks.pkb.plektra.inputmethod.suggestions.ui.FullSuggestionsBar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InlineAutofillTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @After
    fun tearDown() {
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    private fun update(bar: FullSuggestionsBar, suggestions: List<String>) {
        bar.update(suggestions, true, null, null, false, null,
            null, null, null, null, null, null)
    }

    private fun chip(text: String) = TextView(context).apply {
        this.text = text
        layoutParams = ViewGroup.LayoutParams(200, 40)
    }

    private fun visibleViews(view: View): List<View> {
        if (view.visibility != View.VISIBLE) return emptyList()
        return listOf(view) + if (view is ViewGroup) {
            (0 until view.childCount).flatMap { visibleViews(view.getChildAt(it)) }
        } else emptyList()
    }

    @Test
    fun offByDefaultAndBackedUp() {
        assertFalse(InlineAutofillSettings.isEnabled(context))
        InlineAutofillSettings.setEnabled(context, true)
        assertTrue(InlineAutofillSettings.isEnabled(context))
        assertEquals(rocks.pkb.plektra.backup.PreferenceValueType.BOOLEAN, BackupPreferenceContract.expectedExportType("plektra_prefs", InlineAutofillSettings.KEY_ENABLED))
    }

    @Test
    fun chipsTakeThePlaceOfTheSuggestionsAndLeaveWithTheirClear() {
        val bar = FullSuggestionsBar(context)
        val frame = bar.ensureView()
        update(bar, listOf("Alpha", "Beta", "Gamma"))
        assertTrue(visibleViews(frame).any { it is TextView && it.text == "Alpha" })

        val strip = InlineAutofillStrip(context)
        val login = chip("me@example.com")
        strip.set(listOf(login))
        strip.render(bar, active = true, chipColour = 0xFF333333.toInt(), buttonHeight = 60)
        assertTrue(strip.isShown)
        assertSame(bar.centerAccessoryHost(), strip.views.single().parent.parent.parent.parent)
        update(bar, listOf("Alpha", "Beta", "Gamma"))
        val shown = visibleViews(frame)
        assertTrue(login in shown)
        assertFalse("The suggestions give way to the chips", shown.any { it is TextView && it.text == "Alpha" })

        strip.clear(bar)
        assertFalse(strip.isShown)
        assertTrue(strip.views.isEmpty())
        update(bar, listOf("Alpha", "Beta", "Gamma"))
        assertTrue(visibleViews(frame).any { it is TextView && it.text == "Alpha" })
    }

    @Test
    fun chipsShowWhereThereAreNoSuggestions() {
        val bar = FullSuggestionsBar(context)
        val frame = bar.ensureView()
        update(bar, emptyList())
        assertEquals(View.GONE, frame.visibility)

        val strip = InlineAutofillStrip(context)
        strip.set(listOf(chip("Card ending 4242")))
        strip.render(bar, active = true, chipColour = 0xFF333333.toInt(), buttonHeight = null)
        update(bar, emptyList())
        assertEquals(View.VISIBLE, frame.visibility)
        assertTrue(visibleViews(frame).any { it is TextView && it.text == "Card ending 4242" })
    }

    @Test
    fun chipsHeldBackLeaveTheBarToTheSuggestions() {
        val bar = FullSuggestionsBar(context)
        val frame = bar.ensureView()
        val strip = InlineAutofillStrip(context)
        strip.set(listOf(chip("me@example.com")))
        strip.render(bar, active = true, chipColour = 0, buttonHeight = null)
        // Symbols or the clipboard open: the chips step aside but are kept
        strip.render(bar, active = false, chipColour = 0, buttonHeight = null)
        assertFalse(strip.isShown)
        assertEquals(1, strip.views.size)
        update(bar, listOf("Alpha"))
        assertTrue(visibleViews(frame).any { it is TextView && it.text == "Alpha" })
    }

    @Test
    fun theKeyboardAsksForNoChipsWhileTheSettingIsOff() {
        val service = Robolectric.buildService(PhysicalKeyboardInputMethodService::class.java).create().get()
        try {
            assertNull(service.onCreateInlineSuggestionsRequest(Bundle()))
            InlineAutofillSettings.setEnabled(context, true)
            val request = service.onCreateInlineSuggestionsRequest(Bundle())
            assertTrue(request != null && request.maxSuggestionCount > 0)
        } finally {
            service.onDestroy()
        }
    }
}
