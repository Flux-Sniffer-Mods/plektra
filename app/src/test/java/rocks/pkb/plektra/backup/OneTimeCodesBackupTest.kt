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
import rocks.pkb.plektra.otp.OneTimeCodeSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OneTimeCodesBackupTest {
    @Test fun zipRestoresTheOneTimeCodesSwitch() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val prefs = SettingsManager.getPreferences(context)
        prefs.edit().clear().commit()
        OneTimeCodeSettings.setEnabled(context, true)
        val archive = File(context.cacheDir, "one-time-codes.zip")
        assertTrue(BackupManager.createBackup(context, Uri.fromFile(archive)) is BackupResult.Success)
        prefs.edit().clear().commit()
        assertFalse(OneTimeCodeSettings.isEnabled(context))
        assertTrue(RestoreManager.restore(context, Uri.fromFile(archive)) is RestoreResult.Success)
        assertTrue(OneTimeCodeSettings.isEnabled(context))
    }
}
