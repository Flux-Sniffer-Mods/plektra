package rocks.pkb.plektra.inputmethod.expansion

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TextExpansionPopupTest {
    @Test
    fun popupIsOffsetByItsFullHeightAboveTheImeParentFrame() {
        assertEquals(-500, TextExpansionPopup.popupOffsetY(popupHeight = 500))
    }

    @Test
    fun anAnchorWithoutAWindowShowsNothingInsteadOfCrashing() {
        val context = RuntimeEnvironment.getApplication()
        val popup = TextExpansionPopup(context, onSelected = {}, onDismissed = {})
        // The keyboard's view after its window closed: never attached
        val anchor = View(context)

        popup.show(anchor, listOf(ExpansionMatch(ExpansionProvider.SNIPPET, "sig", "Regards", "!sig", 0)))

        assertFalse(popup.isShowing())
    }
}
