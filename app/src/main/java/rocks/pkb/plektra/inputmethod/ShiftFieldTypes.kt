package rocks.pkb.plektra.inputmethod

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import rocks.pkb.plektra.R
import rocks.pkb.plektra.SettingsManager

/**
 * Automatic Shift by kind of text field: which fields start (and continue sentences) with Shift.
 * Password fields never do.
 */
object ShiftFieldTypes {
    enum class Type(val id: String, val labelRes: Int, val onByDefault: Boolean) {
        TEXT("text", R.string.shift_field_text, true),
        NAMES("names", R.string.shift_field_names, true),
        ADDRESSES("addresses", R.string.shift_field_addresses, true),
        SEARCH("search", R.string.shift_field_search, false),
        LINKS("links", R.string.shift_field_links, false),
        EMAIL("email", R.string.shift_field_email, false),
        /**
         * Fields that don't say what they are (no input type): off until chosen, since apps also
         * report one when nothing is focused, where letters are the app's own shortcuts.
         */
        OTHER("other", R.string.shift_field_other, false)
    }

    /** The kinds the former "Shift in all text fields" switch added. */
    private val ALL_TEXT_FIELDS_EXTRA = setOf(Type.SEARCH, Type.LINKS, Type.EMAIL)

    /** What kind of field [info] is; null for fields automatic Shift never applies to. */
    fun of(info: EditorInfo?): Type? = info?.let { of(it.inputType, it.imeOptions) }

    internal fun of(inputType: Int, imeOptions: Int): Type? {
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        if (inputClass == InputType.TYPE_NULL) return Type.OTHER
        if (inputClass != InputType.TYPE_CLASS_TEXT) return null
        return when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> null
            InputType.TYPE_TEXT_VARIATION_URI -> Type.LINKS
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> Type.EMAIL
            InputType.TYPE_TEXT_VARIATION_FILTER -> Type.SEARCH
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> Type.NAMES
            InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> Type.ADDRESSES
            else ->
                if (imeOptions and EditorInfo.IME_MASK_ACTION == EditorInfo.IME_ACTION_SEARCH) Type.SEARCH else Type.TEXT
        }
    }

    /** The kinds of field with automatic Shift. */
    fun enabled(context: Context): Set<Type> {
        val stored = SettingsManager.getAutoShiftFieldTypes(context)
        if (stored != null) return Type.entries.filter { it.id in stored }.toSet()
        // Until chosen: messages, names and addresses, plus what "Shift in all text fields" added
        val defaults = Type.entries.filter { it.onByDefault }.toSet()
        return if (SettingsManager.getAutoCapitalizeRestrictedFields(context)) defaults + ALL_TEXT_FIELDS_EXTRA else defaults
    }

    fun setEnabled(context: Context, types: Set<Type>) {
        SettingsManager.setAutoShiftFieldTypes(context, types.map { it.id }.toSet())
    }
}
