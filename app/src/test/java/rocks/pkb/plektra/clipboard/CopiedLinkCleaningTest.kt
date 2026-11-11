package rocks.pkb.plektra.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.PersistableBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.SettingsManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CopiedLinkCleaningTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val tracked = "https://example.com/article?utm_source=x&id=7&fbclid=abc"
    private val clean = "https://example.com/article?id=7"

    @Before
    fun resetSettings() {
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    @Test
    fun offUnlessSwitchedOn() {
        assertFalse(CopiedLinkCleaning.isEnabled(context))
        clipboard.setPrimaryClip(ClipData.newPlainText("link", tracked))

        assertFalse(CopiedLinkCleaning.cleanClipboard(context, clipboard))
        assertEquals(tracked, primaryText())
    }

    @Test
    fun copiedLinkIsCleanedOnTheClipboardWithItsLabel() {
        CopiedLinkCleaning.setEnabled(context, true)
        clipboard.setPrimaryClip(ClipData.newPlainText("link", "Read this: $tracked."))

        assertTrue(CopiedLinkCleaning.cleanClipboard(context, clipboard))
        assertEquals("Read this: $clean.", primaryText())
        assertEquals("link", clipboard.primaryClip?.description?.label)
        // The clean copy comes back as a clipboard change: nothing more to do, so no loop
        assertFalse(CopiedLinkCleaning.cleanClipboard(context, clipboard))
    }

    @Test
    fun sensitiveCopiesAreNeverRewritten() {
        val clip = ClipData.newPlainText("password", tracked)
        clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }

        assertNull(CopiedLinkCleaning.cleanedText(clip))
    }

    @Test
    fun formattedTextIsRewrittenOnlyWhenItIsJustALink() {
        assertEquals(clean, CopiedLinkCleaning.cleanedText(ClipData.newHtmlText("link", tracked, "<a href=\"$tracked\">$tracked</a>")))
        assertNull(CopiedLinkCleaning.cleanedText(ClipData.newHtmlText("page", "See $tracked", "<p>See <a href=\"$tracked\">it</a></p>")))
    }

    @Test
    fun otherCopiesAreLeftAsTheyAre() {
        assertNull(CopiedLinkCleaning.cleanedText(ClipData.newPlainText("text", "No link here")))
        assertNull(CopiedLinkCleaning.cleanedText(ClipData.newPlainText("link", clean)))
        assertNull(CopiedLinkCleaning.cleanedText(ClipData.newRawUri("file", Uri.parse("content://media/1"))))
        val two = ClipData.newPlainText("two", tracked).apply { addItem(ClipData.Item(tracked)) }
        assertNull(CopiedLinkCleaning.cleanedText(two))
        assertNull(CopiedLinkCleaning.cleanedText(ClipData.newPlainText("long", tracked + " " + "x".repeat(4_000))))
    }

    private fun primaryText(): String? =
        clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
}
