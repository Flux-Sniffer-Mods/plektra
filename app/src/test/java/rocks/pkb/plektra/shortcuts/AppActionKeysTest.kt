package rocks.pkb.plektra.shortcuts

import android.view.KeyEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.backup.BackupPreferenceContract
import rocks.pkb.plektra.backup.PreferenceValueType

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppActionKeysTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @After
    fun tearDown() {
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    @Test
    fun ctrlAltDigitsAndCtrlCommaChooseTheAppsScreens() {
        assertEquals(AppActionKeys.Target.Shortcut(0), AppActionKeys.targetFor(KeyEvent.KEYCODE_1, ctrl = true, alt = true, altChar = null))
        assertEquals(AppActionKeys.Target.Shortcut(3), AppActionKeys.targetFor(KeyEvent.KEYCODE_4, ctrl = true, alt = true, altChar = null))
        assertEquals(AppActionKeys.Target.Settings, AppActionKeys.targetFor(KeyEvent.KEYCODE_COMMA, ctrl = true, alt = false, altChar = null))
        // Without a number row: the key whose Alt character is the digit
        assertEquals(AppActionKeys.Target.Shortcut(1), AppActionKeys.targetFor(KeyEvent.KEYCODE_X, ctrl = true, alt = true, altChar = '2'))
    }

    @Test
    fun otherCombosAreLeftAlone() {
        assertNull(AppActionKeys.targetFor(KeyEvent.KEYCODE_1, ctrl = false, alt = true, altChar = null))
        assertNull(AppActionKeys.targetFor(KeyEvent.KEYCODE_5, ctrl = true, alt = true, altChar = null))
        assertNull(AppActionKeys.targetFor(KeyEvent.KEYCODE_C, ctrl = true, alt = false, altChar = null))
        assertNull(AppActionKeys.targetFor(KeyEvent.KEYCODE_X, ctrl = true, alt = true, altChar = '7'))
    }

    @Test
    fun offByDefaultAndBackedUp() {
        assertFalse(AppActionKeys.isEnabled(context))
        assertEquals(PreferenceValueType.BOOLEAN, BackupPreferenceContract.expectedExportType("plektra_prefs", AppActionKeys.KEY_ENABLED))
    }

    @Test
    fun anAppWithoutTheShortcutLetsTheKeyGoOn() {
        assertFalse(AppActionKeys.open(context, "com.example.not.installed", AppActionKeys.Target.Shortcut(0)))
        assertFalse(AppActionKeys.open(context, "com.example.not.installed", AppActionKeys.Target.Settings))
    }
}
