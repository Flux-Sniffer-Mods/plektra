package rocks.pkb.plektra.backup

import android.net.Uri
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.core.IncognitoTyping

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IncognitoTypingBackupTest {
    @Test fun zipRestoresBothIncognitoSettingsAfterPreferencesAreCleared() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val prefs = SettingsManager.getPreferences(context)
        prefs.edit().clear().commit()
        IncognitoTyping.setAlways(context, true)
        IncognitoTyping.setFollowsApps(context, false)
        val archive = File(context.cacheDir, "incognito-typing.zip")
        assertTrue(BackupManager.createBackup(context, Uri.fromFile(archive)) is BackupResult.Success)
        prefs.edit().clear().commit()
        assertFalse(IncognitoTyping.isAlways(context))
        assertTrue(IncognitoTyping.followsApps(context))
        assertTrue(RestoreManager.restore(context, Uri.fromFile(archive)) is RestoreResult.Success)
        assertTrue(IncognitoTyping.isAlways(context))
        assertFalse(IncognitoTyping.followsApps(context))
    }
}
