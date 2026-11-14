package rocks.pkb.plektra.otp

import android.text.InputType
import android.view.inputmethod.EditorInfo

/** Where a one-time code goes: a number field, or a text field whose hint or name says code. */
object OneTimeCodeFields {
    private val words = Regex("code|otp|one.?time|verif|2fa|pin|token|passcode|sms|codice|código|kod|код|mã")
    private val notCodes = setOf(
        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_URI, InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
        InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
    )

    fun takesACode(info: EditorInfo?): Boolean {
        info ?: return false
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE) return true
        if (cls != InputType.TYPE_CLASS_TEXT) return false
        if ((info.inputType and InputType.TYPE_MASK_VARIATION) in notCodes) return false
        val text = listOf(info.hintText, info.fieldName, info.label, info.privateImeOptions)
            .joinToString(" ") { it?.toString().orEmpty() }.lowercase()
        return words.containsMatchIn(text)
    }
}
