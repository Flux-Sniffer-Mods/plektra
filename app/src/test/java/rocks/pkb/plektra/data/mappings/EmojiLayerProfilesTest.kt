package rocks.pkb.plektra.data.mappings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.backup.BackupPreferenceContract
import rocks.pkb.plektra.backup.PreferenceValueType
import rocks.pkb.plektra.inputmethod.AlternateCharacterManager
import rocks.pkb.plektra.inputmethod.PhysicalKeyboardInputMethodService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EmojiLayerProfilesTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    @After
    fun tearDown() {
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    private fun builtIn(id: String) = EmojiLayerProfiles.BUILT_IN.single { it.id == id }

    @Test
    fun everyBuiltInFillsTheLetterKeys() {
        EmojiLayerProfiles.BUILT_IN.forEach { profile ->
            assertEquals(profile.id, EmojiLayerProfiles.KEYS.toSet(), profile.mappings.keys)
            assertTrue(profile.id, profile.builtIn)
        }
    }

    @Test
    fun applyingMakesItTheEmojiLayer() {
        val work = builtIn("work")
        EmojiLayerProfiles.apply(context, work)
        assertEquals(work.mappings, SettingsManager.getSymMappings(context))
        assertEquals("work", EmojiLayerProfiles.activeId(context))
    }

    @Test
    fun yourOwnAreKeptRenamedAndDeleted() {
        val created = EmojiLayerProfiles.createCustom(context, "Mine", builtIn("love").mappings)
        EmojiLayerProfiles.apply(context, created)
        EmojiLayerProfiles.saveCustom(context, created.copy(name = "Ours"))
        assertEquals(listOf("Ours"), EmojiLayerProfiles.custom(context).map { it.name })
        assertEquals(created.mappings, EmojiLayerProfiles.custom(context).single().mappings)

        EmojiLayerProfiles.deleteCustom(context, created.id)
        assertTrue(EmojiLayerProfiles.custom(context).isEmpty())
        assertNull("Deleting the one in use forgets it", EmojiLayerProfiles.activeId(context))
    }

    @Test
    fun appsGetTheirSituation() {
        assertEquals("chatting", EmojiLayerProfiles.forApp(context, "com.whatsapp")?.id)
        assertEquals("work", EmojiLayerProfiles.forApp(context, "com.google.android.gm")?.id)
        assertEquals("food", EmojiLayerProfiles.forApp(context, "com.ubercab.eats")?.id)
        assertNull(EmojiLayerProfiles.forApp(context, "com.example.unknown"))

        // An app not on the list goes by the category it gives itself
        shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            packageName = "com.example.maps"
            applicationInfo = ApplicationInfo().apply {
                packageName = "com.example.maps"
                category = ApplicationInfo.CATEGORY_MAPS
            }
        })
        assertEquals("travel", EmojiLayerProfiles.forApp(context, "com.example.maps")?.id)
    }

    @Test
    fun switchingByAppIsOffByDefaultAndBackedUp() {
        assertNull(EmojiLayerProfiles.overrideFor(context, "com.whatsapp"))
        EmojiLayerProfiles.setSwitchByApp(context, true)
        assertEquals(builtIn("chatting").mappings, EmojiLayerProfiles.overrideFor(context, "com.whatsapp"))
        listOf(
            EmojiLayerProfiles.PREF_KEY to PreferenceValueType.STRING,
            EmojiLayerProfiles.PREF_ACTIVE to PreferenceValueType.STRING,
            EmojiLayerProfiles.PREF_SWITCH_BY_APP to PreferenceValueType.BOOLEAN
        ).forEach { (key, type) -> assertEquals(key, type, BackupPreferenceContract.expectedExportType("plektra_prefs", key)) }
    }

    @Test
    fun theLayerFollowsTheAppWithoutChangingYourOwn() {
        val prefs = SettingsManager.getPreferences(context)
        val manager = AlternateCharacterManager(context.assets, prefs, context)
        val own = manager.getSymMappings().toMap()
        val chatting = builtIn("chatting").mappings
        manager.setEmojiLayerOverride(chatting)
        assertEquals(chatting, manager.getSymMappings())
        manager.setEmojiLayerOverride(null)
        assertEquals(own, manager.getSymMappings())
        assertTrue("Your saved layer is untouched", SettingsManager.getSymMappings(context).isEmpty())
    }

    @Test
    fun theKeyboardSwitchesTheLayerAsAnAppStarts() {
        EmojiLayerProfiles.setSwitchByApp(context, true)
        val controller = Robolectric.buildService(PhysicalKeyboardInputMethodService::class.java).create()
        try {
            val service = controller.get()
            val field = PhysicalKeyboardInputMethodService::class.java.getDeclaredField("alternateCharacterManager")
                .apply { isAccessible = true }
            val manager = field.get(service) as AlternateCharacterManager
            val own = manager.getSymMappings().toMap()
            service.onStartInput(EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT
                packageName = "com.whatsapp"
            }, false)
            assertEquals(builtIn("chatting").mappings, manager.getSymMappings())
            service.onStartInput(EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT
                packageName = "com.example.unknown"
            }, false)
            assertEquals(own, manager.getSymMappings())
        } finally {
            controller.destroy()
        }
    }
}
