package rocks.pkb.plektra.core

import android.content.Context
import android.text.Editable
import android.text.Selection
import android.text.SpannableStringBuilder
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EditHistoryTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    /** A whole field, as an app hands it over. */
    private class Field(context: Context) : BaseInputConnection(View(context), true) {
        private val content = SpannableStringBuilder().also { Selection.setSelection(it, 0) }
        override fun getEditable(): Editable = content
        override fun getExtractedText(request: ExtractedTextRequest?, flags: Int) = ExtractedText().apply {
            text = content.toString()
            startOffset = 0
            selectionStart = Selection.getSelectionStart(content)
            selectionEnd = Selection.getSelectionEnd(content)
        }
        val text get() = content.toString()
    }

    @After
    fun tearDown() {
        EditHistory.resetForTest()
    }

    @Test
    fun undoGoesBackAStepAndRedoForward() {
        val field = Field(context)
        EditHistory.onFieldStarted("app:1:1", recordable = true)
        EditHistory.record(field)
        field.commitText("hello ", 1)
        EditHistory.record(field)
        field.commitText("world", 1)

        assertTrue(EditHistory.undo(field))
        assertEquals("hello ", field.text)
        assertTrue(EditHistory.undo(field))
        assertEquals("", field.text)
        assertFalse("nothing more to undo: the app's own undo can try", EditHistory.undo(field))

        assertTrue(EditHistory.redo(field))
        assertEquals("hello ", field.text)
        assertTrue(EditHistory.redo(field))
        assertEquals("hello world", field.text)
        assertFalse(EditHistory.redo(field))
    }

    @Test
    fun aRecordAfterAnUndoKeepsTheRedo() {
        val field = Field(context)
        EditHistory.onFieldStarted("app:1:1", recordable = true)
        EditHistory.record(field)
        field.commitText("typed", 1)
        EditHistory.undo(field)

        // A step scheduled before the undo lands after it: only the cursor is new
        EditHistory.record(field)

        assertTrue(EditHistory.redo(field))
        assertEquals("typed", field.text)
    }

    @Test
    fun aPasswordFieldKeepsNoHistory() {
        val field = Field(context)
        EditHistory.onFieldStarted("app:2:129", recordable = false)
        EditHistory.record(field)
        field.commitText("secret", 1)

        assertFalse(EditHistory.undo(field))
        assertEquals("secret", field.text)
    }

    @Test
    fun diffFindsOnlyTheChangedMiddle() {
        assertEquals(6 to 0, EditHistory.diff("hello world", "hello "))
        assertEquals(6 to 5, EditHistory.diff("hello brave world", "hello world"))
        assertEquals(3 to 0, EditHistory.diff("aaa", "aaaa"))
        assertEquals(0 to 0, EditHistory.diff("", "typed"))
    }
}
