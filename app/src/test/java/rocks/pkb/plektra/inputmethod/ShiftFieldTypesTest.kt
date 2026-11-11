package rocks.pkb.plektra.inputmethod

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.inputmethod.ShiftFieldTypes.Type

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShiftFieldTypesTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    private fun text(variation: Int) = InputType.TYPE_CLASS_TEXT or variation

    @Test
    fun kindsOfFieldAreToldApart() {
        assertEquals(Type.TEXT, ShiftFieldTypes.of(text(InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE), 0))
        assertEquals(Type.SEARCH, ShiftFieldTypes.of(text(InputType.TYPE_TEXT_VARIATION_NORMAL), EditorInfo.IME_ACTION_SEARCH))
        assertEquals(Type.SEARCH, ShiftFieldTypes.of(text(InputType.TYPE_TEXT_VARIATION_FILTER), 0))
        assertEquals(Type.LINKS, ShiftFieldTypes.of(text(InputType.TYPE_TEXT_VARIATION_URI), EditorInfo.IME_ACTION_GO))
        assertEquals(Type.EMAIL, ShiftFieldTypes.of(text(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS), 0))
        assertEquals(Type.NAMES, ShiftFieldTypes.of(text(InputType.TYPE_TEXT_VARIATION_PERSON_NAME), 0))
        assertEquals(Type.ADDRESSES, ShiftFieldTypes.of(text(InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS), 0))
        assertEquals(Type.OTHER, ShiftFieldTypes.of(InputType.TYPE_NULL, 0))
    }

    @Test
    fun passwordsAndNumbersAreNeverShifted() {
        assertNull(ShiftFieldTypes.of(text(InputType.TYPE_TEXT_VARIATION_PASSWORD), 0))
        assertNull(ShiftFieldTypes.of(text(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD), 0))
        assertNull(ShiftFieldTypes.of(InputType.TYPE_CLASS_NUMBER, 0))
    }

    @Test
    fun untilChosen_messagesNamesAndAddresses_plusWhatShiftInAllTextFieldsAdded() {
        assertEquals(setOf(Type.TEXT, Type.NAMES, Type.ADDRESSES), ShiftFieldTypes.enabled(context))

        SettingsManager.setAutoCapitalizeRestrictedFields(context, true)
        assertEquals(
            setOf(Type.TEXT, Type.NAMES, Type.ADDRESSES, Type.SEARCH, Type.LINKS, Type.EMAIL),
            ShiftFieldTypes.enabled(context)
        )
    }

    @Test
    fun aChoiceIsKeptAndWinsOverTheFormerSwitch() {
        SettingsManager.setAutoCapitalizeRestrictedFields(context, true)
        ShiftFieldTypes.setEnabled(context, setOf(Type.EMAIL, Type.OTHER))
        assertEquals(setOf(Type.EMAIL, Type.OTHER), ShiftFieldTypes.enabled(context))

        ShiftFieldTypes.setEnabled(context, emptySet())
        assertEquals(emptySet<Type>(), ShiftFieldTypes.enabled(context))
    }
}
