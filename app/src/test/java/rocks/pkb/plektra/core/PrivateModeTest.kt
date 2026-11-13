package rocks.pkb.plektra.core

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.OfflineMode
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.commands.CommandExecutor
import rocks.pkb.plektra.commands.CommandRegistry
import rocks.pkb.plektra.commands.CommandSurface
import rocks.pkb.plektra.commands.PlektraCommandSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrivateModeTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        OfflineMode.load(context)
        IncognitoTyping.active = false
        IncognitoTyping.privateMode = false
    }

    @After
    fun tearDown() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        OfflineMode.update(false)
        IncognitoTyping.active = false
        IncognitoTyping.privateMode = false
    }

    @Test
    fun turnsOnIncognitoInEveryFieldAndOfflineModeAtOnce() {
        assertTrue(PrivateMode.toggle(context, announce = false))

        assertTrue(PrivateMode.isOn(context))
        assertTrue(OfflineMode.enabled)
        // The field already being typed in is incognito from now, not only the next one
        assertTrue(IncognitoTyping.active)
        IncognitoTyping.active = false
        assertTrue(IncognitoTyping.active)
    }

    @Test
    fun turningItOffPutsBothBackAsTheyWere() {
        PrivateMode.toggle(context, announce = false)
        assertFalse(PrivateMode.toggle(context, announce = false))

        assertFalse(PrivateMode.isOn(context))
        assertFalse(OfflineMode.enabled)
        assertFalse(IncognitoTyping.active)

        // A field that asks for no learning stays incognito; offline mode chosen before stays on
        OfflineMode.set(context, true)
        IncognitoTyping.active = true
        PrivateMode.toggle(context, announce = false)
        PrivateMode.toggle(context, announce = false)
        assertTrue(OfflineMode.enabled)
        assertTrue(IncognitoTyping.active)
    }

    @Test
    fun keptAcrossRestarts() {
        PrivateMode.toggle(context, announce = false)
        IncognitoTyping.privateMode = false

        PrivateMode.load(context)
        assertTrue(IncognitoTyping.privateMode)
    }

    @Test
    fun aCommandForAKeyOrTheQuickLauncher() {
        val registry = CommandRegistry(context)
        val command = registry.resolve(PlektraCommandSource.COMMAND_TOGGLE_PRIVATE_MODE)
        assertNotNull(command)
        assertTrue(registry.getCommands(CommandSurface.QuickLauncher).any { it.id == command!!.id })
        assertTrue(registry.getCommands(CommandSurface.AssignedKey).any { it.id == command!!.id })
        assertEquals(command, registry.search(CommandSurface.QuickLauncher, "incognito").single { it.id == command!!.id })

        assertTrue(CommandExecutor(context, showToast = false).execute(command!!).isSuccess)
        assertTrue(PrivateMode.isOn(context))
    }
}
