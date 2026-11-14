package rocks.pkb.plektra

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExternalLinksTest {
    private val context = RuntimeEnvironment.getApplication()
    private fun link() = Intent(Intent.ACTION_VIEW, Uri.parse("https://pkb.rocks/donate")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    @Test
    fun noAppForTheLinkSaysSoInsteadOfThrowing() {
        // Robolectric checks for an activity that handles the intent, as a phone without a browser would
        shadowOf(context).checkActivities(true)

        assertFalse(ExternalLinks.start(context, link()))
        assertEquals(context.getString(R.string.external_link_no_app), ShadowToast.getTextOfLatestToast())
        assertNull(shadowOf(context).nextStartedActivity)
    }

    @Test
    fun anAppForTheLinkIsStarted() {
        val browser = ComponentName("org.example.browser", "org.example.browser.Main")
        shadowOf(context.packageManager).addActivityIfNotPresent(browser)
        shadowOf(context.packageManager).addIntentFilterForActivity(
            browser,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("https")
            }
        )
        shadowOf(context).checkActivities(true)

        assertTrue(ExternalLinks.start(context, link()))
        assertEquals(Uri.parse("https://pkb.rocks/donate"), shadowOf(context).nextStartedActivity.data)
    }
}
