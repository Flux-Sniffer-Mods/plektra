package rocks.pkb.plektra.shortcuts

import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.net.Uri
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.backup.BackupPreferenceContract
import rocks.pkb.plektra.commands.UserShortcutCommandSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UserShortcutsTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val browser = "com.example.browser"

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            packageName = browser
            applicationInfo = ApplicationInfo().apply { packageName = browser }
        })
    }

    @After
    fun tearDown() {
        UserShortcuts.all(context).forEach { UserShortcuts.remove(context, it.id) }
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    @Suppress("DEPRECATION")
    private fun result(intent: Intent, name: String) = Intent()
        .putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
        .putExtra(Intent.EXTRA_SHORTCUT_NAME, name)

    @Test
    fun anAppsShortcutBecomesAQuickLauncherResult() {
        val bookmark = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/news")).setPackage(browser)
        val added = UserShortcuts.addFromResult(context, browser, result(bookmark, "News"))
        assertNotNull(added)
        val command = UserShortcutCommandSource().getCommands(context).single()
        assertEquals("News", command.label)

        UserShortcuts.remove(context, added!!.id)
        assertTrue(UserShortcutCommandSource().getCommands(context).isEmpty())
    }

    @Test
    fun theKeyboardsOwnScreensAreNeverKept() {
        val own = Intent().setComponent(ComponentName(context, rocks.pkb.plektra.MainActivity::class.java))
        assertNull(UserShortcuts.addFromResult(context, browser, result(own, "Settings")))
        assertNull(UserShortcuts.startableIntent(context, own.toUri(Intent.URI_INTENT_SCHEME)))
    }

    @Test
    fun aStoredShortcutCarriesNoFileAccess() {
        val stored = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).setPackage(browser)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        stored.clipData = ClipData.newRawUri("", Uri.parse("content://rocks.pkb.plektra.fileprovider/x"))
        val intent = UserShortcuts.startableIntent(context, stored.toUri(Intent.URI_INTENT_SCHEME))!!
        assertEquals(0, intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
        assertNull(intent.clipData)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun aDirectDialWithoutThePhonePermissionOpensTheDialer() {
        val call = Intent(Intent.ACTION_CALL, Uri.parse("tel:123"))
        assertEquals(Intent.ACTION_DIAL, UserShortcuts.startableIntent(context, call.toUri(Intent.URI_INTENT_SCHEME))!!.action)
    }

    @Test
    fun keptOutOfBackups() {
        val excluded = BackupPreferenceContract.deliberatelyExcludedPlektraKeys
        assertTrue(UserShortcuts.KEY in excluded)
        assertTrue(UserShortcuts.UNSUPPORTED_KEY in excluded)
    }
}
