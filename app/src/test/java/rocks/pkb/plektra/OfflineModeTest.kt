package rocks.pkb.plektra

import android.os.Looper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import rocks.pkb.plektra.data.layout.LayoutRepositoryManager
import rocks.pkb.plektra.dictionaries.DictionaryRepositoryManager
import rocks.pkb.plektra.inputmethod.speech.SpeechProvider
import rocks.pkb.plektra.inputmethod.speech.SpeechSettings
import rocks.pkb.plektra.update.fetchReleaseNotesForVersion
import rocks.pkb.plektra.update.shouldUseGithubUpdateChecks

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OfflineModeTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        OfflineMode.load(context)
    }

    @After
    fun tearDown() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        OfflineMode.update(false)
    }

    @Test
    fun offUnlessSwitchedOn() {
        assertFalse(OfflineMode.enabled)
        assertFalse(OfflineMode.isOn(context))

        OfflineMode.set(context, true)
        assertTrue(OfflineMode.enabled)
        assertTrue(OfflineMode.isOn(context))

        OfflineMode.set(context, false)
        assertFalse(OfflineMode.enabled)
    }

    @Test
    fun followsThePreferenceHoweverItChanges() {
        val preferences = SettingsManager.getPreferences(context)

        preferences.edit().putBoolean(OfflineMode.KEY, true).commit()
        assertTrue(OfflineMode.enabled)

        preferences.edit().clear().commit()
        assertFalse(OfflineMode.enabled)
    }

    @Test
    fun nothingIsFetchedWhileItIsOn() = runBlocking {
        OfflineMode.set(context, true)

        assertFalse(shouldUseGithubUpdateChecks(context))
        assertEquals("Offline mode is on", DictionaryRepositoryManager.fetchManifest().exceptionOrNull()?.message)
        assertEquals("Offline mode is on", LayoutRepositoryManager.fetchManifest().exceptionOrNull()?.message)

        var answered = false
        var notes: Any? = "unanswered"
        fetchReleaseNotesForVersion("1.0.0", "en") {
            answered = true
            notes = it
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(answered)
        assertNull(notes)
    }

    @Test
    fun voiceInputUsesAndroidsInPlaceOfAnOnlineProvider() {
        SpeechSettings.setProvider(context, SpeechProvider.OPENAI)
        assertEquals(SpeechProvider.OPENAI, SpeechSettings.providerInUse(context))

        OfflineMode.set(context, true)
        assertEquals(SpeechProvider.SYSTEM, SpeechSettings.providerInUse(context))
        // The choice itself is kept for when offline mode is turned off again
        assertEquals(SpeechProvider.OPENAI, SpeechSettings.provider(context))

        SpeechSettings.setProvider(context, SpeechProvider.FUTO)
        assertEquals(SpeechProvider.FUTO, SpeechSettings.providerInUse(context))
    }
}
