package rocks.pkb.plektra

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.backup.BackupPreferenceContract
import rocks.pkb.plektra.backup.PreferenceValueType

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WallpaperKeyboardColoursTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val target = SettingsManager.KeyboardThemeTarget.HARDWARE

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    @After
    fun tearDown() {
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    /** Each palette's tone as a colour that says which it is: palette in red, tone in green and blue. */
    private val palette = WallpaperKeyboardColours.Palette(
        neutral = { tone -> colour(1, tone) },
        neutralVariant = { tone -> colour(2, tone) },
        primary = { tone -> colour(3, tone) },
        secondary = { tone -> colour(4, tone) },
        tertiary = { tone -> colour(5, tone) }
    )

    private fun colour(palette: Int, tone: Int) = (0xFF shl 24) or (palette shl 16) or tone

    @Test
    fun darkAndLightTakeTheirTonesAndKeepTheThemesShape() {
        val theme = SettingsManager.getEffectiveKeyboardTheme(context, target)
            .copy(background = 0x80123456.toInt(), keyHeightScale = 1.3f)
        val dark = WallpaperKeyboardColours.recolour(theme, palette, dark = true)
        assertEquals(colour(1, 800), dark.normalKey)
        assertEquals(colour(1, 50), dark.textAndIcons)
        assertEquals(colour(3, 200), dark.accent)
        assertEquals("A see-through background stays see-through", 0x80000000.toInt() or (colour(1, 900) and 0xFFFFFF), dark.background)
        assertEquals(1.3f, dark.keyHeightScale)

        val light = WallpaperKeyboardColours.recolour(theme, palette, dark = false)
        assertEquals(colour(1, 10), light.normalKey)
        assertEquals(colour(1, 900), light.textAndIcons)
        assertEquals(colour(3, 600), light.accent)
    }

    @Test
    fun offByDefaultAndBackedUp() {
        val theme = SettingsManager.getEffectiveKeyboardTheme(context, target)
        assertEquals(theme, WallpaperKeyboardColours.applyIfOn(context, theme))
        assertEquals(PreferenceValueType.BOOLEAN, BackupPreferenceContract.expectedExportType("plektra_prefs", WallpaperKeyboardColours.KEY_ENABLED))
        assertTrue("The keyboard redraws when it changes", SettingsManager.isKeyboardThemePreferenceKey(WallpaperKeyboardColours.KEY_ENABLED))
    }

    @Test
    fun onTheKeyboardsThemeTakesTheWallpapersColours() {
        val plain = SettingsManager.getEffectiveKeyboardTheme(context, target)
        WallpaperKeyboardColours.setEnabled(context, true)
        val coloured = SettingsManager.getEffectiveKeyboardTheme(context, target)
        // Light mode: keys in the wallpaper's lightest neutral tone, text in its darkest
        assertEquals(context.getColor(android.R.color.system_neutral1_10), coloured.normalKey)
        assertEquals(context.getColor(android.R.color.system_neutral1_900), coloured.textAndIcons)
        assertEquals(plain.keyCornerRadiusRatio, coloured.keyCornerRadiusRatio)
    }
}
