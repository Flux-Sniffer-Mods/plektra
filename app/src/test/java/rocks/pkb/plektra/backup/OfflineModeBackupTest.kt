package rocks.pkb.plektra.backup

import android.net.Uri
import android.os.Looper
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import rocks.pkb.plektra.OfflineMode
import rocks.pkb.plektra.SettingsManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OfflineModeBackupTest {
    @After
    fun tearDown() = OfflineMode.update(false)

    @Test fun zipRestoresOfflineModeAndItTakesEffectAtOnce() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val prefs = SettingsManager.getPreferences(context)
        prefs.edit().clear().commit()
        OfflineMode.load(context)
        OfflineMode.set(context, true)
        val archive = File(context.cacheDir, "offline-mode.zip")
        assertTrue(BackupManager.createBackup(context, Uri.fromFile(archive)) is BackupResult.Success)
        prefs.edit().clear().commit()
        assertFalse(OfflineMode.isOn(context))
        assertFalse(OfflineMode.enabled)
        assertTrue(RestoreManager.restore(context, Uri.fromFile(archive)) is RestoreResult.Success)
        assertTrue(OfflineMode.isOn(context))
        // Preference changes reach their listeners on the main thread
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(OfflineMode.enabled)
    }
}
