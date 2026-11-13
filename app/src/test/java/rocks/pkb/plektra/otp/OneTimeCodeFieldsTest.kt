package rocks.pkb.plektra.otp

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OneTimeCodeFieldsTest {
    private val context get() = RuntimeEnvironment.getApplication()

    private fun field(type: Int, hint: String? = null) = EditorInfo().apply {
        inputType = type
        hintText = hint
    }

    @Test
    fun aCodeGoesInNumberFieldsAndFieldsThatSayCode() {
        assertTrue(OneTimeCodeFields.takesACode(field(InputType.TYPE_CLASS_NUMBER)))
        assertTrue(OneTimeCodeFields.takesACode(field(InputType.TYPE_CLASS_TEXT, "Verification code")))
        assertTrue(OneTimeCodeFields.takesACode(field(InputType.TYPE_CLASS_TEXT, "Codice SMS")))
    }

    @Test
    fun notInMessagesEmailsOrNames() {
        assertFalse(OneTimeCodeFields.takesACode(field(InputType.TYPE_CLASS_TEXT, "Message")))
        assertFalse(OneTimeCodeFields.takesACode(field(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, "Email or code")))
        assertFalse(OneTimeCodeFields.takesACode(field(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME, "Code name")))
        assertFalse(OneTimeCodeFields.takesACode(null))
    }

    @Test
    fun theCodeInANotification() {
        val plain = Notification.Builder(context, "test")
            .setContentTitle("Bank")
            .setContentText("Your verification code is 482913. Don't share it.")
            .build()
        assertEquals("482913", OneTimeCodeListener.codeIn(plain))

        val intent = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        val withCopy = Notification.Builder(context, "test")
            .setContentText("Order 845921 is on its way")
            .addAction(Notification.Action.Builder(null, "Copy 123 456", intent).build())
            .build()
        assertEquals("123456", OneTimeCodeListener.codeIn(withCopy))

        val noCode = Notification.Builder(context, "test").setContentText("See you at 2030 Main St").build()
        assertNull(OneTimeCodeListener.codeIn(noCode))
    }

    @Test
    fun theNewestMessageOfAConversationDecides() {
        val person = android.app.Person.Builder().setName("Service").build()
        val conversation = Notification.Builder(context, "test")
            .setStyle(
                Notification.MessagingStyle(person)
                    .addMessage("Your code is 111111", 1_000L, person)
                    .addMessage("Your new code is 222222", 2_000L, person)
            )
            .build()
        assertEquals("222222", OneTimeCodeListener.codeIn(conversation))
    }
}
