package rocks.pkb.plektra.shortcuts

import android.content.ComponentName
import android.content.Intent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.backup.BackupPreferenceContract
import rocks.pkb.plektra.backup.PreferenceValueType
import rocks.pkb.plektra.commands.AppShortcutCommandSource
import rocks.pkb.plektra.commands.CommandJson
import rocks.pkb.plektra.commands.CommandLaunchSpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppShortcutCommandsTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @After
    fun tearDown() {
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    @Test
    fun offByDefaultAndBackedUp() {
        assertTrue(AppShortcutCommandSource().getCommands(context).isEmpty())
        assertEquals(PreferenceValueType.BOOLEAN,
            BackupPreferenceContract.expectedExportType("plektra_prefs", AppShortcutCommandSource.KEY_ENABLED))
    }

    @Test
    fun anAppWithoutShortcutsOffersNone() {
        assertEquals(DiscoveredAppActions.NONE, AppActionDiscovery.discover(context, "com.example.not.installed"))
    }

    @Test
    fun aShortcutOpensOnlyAnAppsOwnScreenThatOthersMayOpen() {
        val own = Intent(Intent.ACTION_MAIN).setComponent(ComponentName(context, rocks.pkb.plektra.MainActivity::class.java))
        val action = DiscoveredAction("open", "Open", own.toUri(Intent.URI_INTENT_SCHEME))
        val intent = AppActionDiscovery.intentFor(context, context.packageName, action)
        assertNotNull(intent)
        assertTrue(intent!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        // Claimed for another app: refused
        assertNull(AppActionDiscovery.intentFor(context, "com.example.other", action))
    }

    @Test
    fun aShortcutKeepsItsIntentWhenSavedToAKey() {
        val spec = CommandLaunchSpec.IntentUri(action = Intent.ACTION_VIEW, packageName = "com.example", intentUri = "intent:#Intent;end")
        assertEquals(spec, CommandJson.launchFromJson(CommandJson.launchToJson(spec)))
    }
}
