package rocks.pkb.plektra.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.os.PersistableBundle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecentCopyTrackerTest {
    private val context = RuntimeEnvironment.getApplication()
    private val clipboard = context.getSystemService(ClipboardManager::class.java)
    private val tracker = RecentCopyTracker(context) { 5_000L }

    @After
    fun tearDown() {
        tracker.stop()
    }

    @Test
    fun aCopyIsKeptInMemoryUntilUsed() {
        tracker.start()
        clipboard.setPrimaryClip(ClipData.newPlainText("test", "hello"))

        assertEquals(RecentCopyTracker.RecentCopy(5_000L, "hello"), tracker.recentCopy)
        tracker.consume()
        assertNull(tracker.recentCopy)
    }

    @Test
    fun aPasswordManagersCopyIsMarkedSensitiveAndNeverLoggedAsText() {
        tracker.start()
        clipboard.setPrimaryClip(ClipData.newPlainText("test", "s3cret").apply {
            description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        })

        val copy = tracker.recentCopy!!
        assertTrue(copy.sensitive)
        assertTrue("s3cret" !in copy.toString())
    }

    @Test
    fun blankCopiesAndCopiesAfterStopAreNotKept() {
        tracker.start()
        clipboard.setPrimaryClip(ClipData.newPlainText("test", "   "))
        assertNull(tracker.recentCopy)

        tracker.stop()
        clipboard.setPrimaryClip(ClipData.newPlainText("test", "later"))
        assertNull(tracker.recentCopy)
    }
}
