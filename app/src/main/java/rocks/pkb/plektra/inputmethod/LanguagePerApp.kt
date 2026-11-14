package rocks.pkb.plektra.inputmethod

import android.content.Context
import rocks.pkb.plektra.SettingsManager

/**
 * Remember the language per app: a language chosen while typing in an app becomes that app's,
 * and entering the app again switches back to it. Off unless switched on. The languages are kept
 * in a file of their own, beside the settings rather than among them, so backups leave them out.
 */
object LanguagePerApp {
    const val KEY = "language_per_app_enabled"
    private const val LANGUAGES = "app_languages"

    fun isEnabled(context: Context): Boolean =
        SettingsManager.getPreferences(context).getBoolean(KEY, false)

    /** Turning it off forgets every app's language. */
    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY, enabled).apply()
        if (!enabled) languages(context).edit().clear().apply()
    }

    /** The language (a [SubtypeCycler.subtypeKey]) last used in [packageName], if any. */
    fun languageFor(context: Context, packageName: String): String? =
        languages(context).getString(packageName, null)

    fun remember(context: Context, packageName: String, subtypeKey: String) {
        languages(context).edit().putString(packageName, subtypeKey).apply()
    }

    private fun languages(context: Context) =
        context.applicationContext.getSharedPreferences(LANGUAGES, Context.MODE_PRIVATE)
}
