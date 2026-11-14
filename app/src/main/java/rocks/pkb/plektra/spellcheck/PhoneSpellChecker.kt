package rocks.pkb.plektra.spellcheck

import android.content.Context
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale
import rocks.pkb.plektra.SettingsManager

/**
 * The phone's own spell checker (the one chosen in Android's settings, Gboard's say) as a second
 * opinion: for a word it thinks is misspelt, its corrections join the suggestion bar. Only when
 * that spell checker is another app's, and only while the option is on (off by default: the
 * words being typed go to that app).
 */
class PhoneSpellChecker(private val context: Context) {
    private var session: SpellCheckerSession? = null
    private var sessionLocale: Locale? = null
    @Volatile private var pending: ((List<String>) -> Unit)? = null

    /** The phone's spell checker is another app's (not Plektra's own, if it is one). */
    private fun otherSpellChecker(tsm: TextServicesManager): Boolean = runCatching {
        val info = if (android.os.Build.VERSION.SDK_INT >= 31) tsm.currentSpellCheckerInfo else null
        info != null && info.packageName != context.packageName
    }.getOrDefault(false)

    /** Asks for [word]'s corrections; [onResult] gets them, and isn't called when it's fine or unknown. */
    fun suggest(word: String, locale: Locale, onResult: (List<String>) -> Unit) {
        if (!isEnabled(context) || word.length < 3 || !word.any { it.isLetter() }) return
        val tsm = context.getSystemService(TextServicesManager::class.java) ?: return
        if (!otherSpellChecker(tsm)) return
        val active = session?.takeIf { sessionLocale == locale } ?: runCatching {
            session?.close()
            tsm.newSpellCheckerSession(null, locale, listener, false)
        }.getOrNull()?.also { session = it; sessionLocale = locale } ?: return
        pending = onResult
        runCatching { active.getSentenceSuggestions(arrayOf(TextInfo(word)), MAX) }
    }

    fun close() {
        runCatching { session?.close() }
        session = null
    }

    internal val listener = object : SpellCheckerSession.SpellCheckerSessionListener {
        override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) = Unit

        override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
            val callback = pending ?: return
            pending = null
            corrections(results)?.let(callback)
        }
    }

    companion object {
        const val KEY_ENABLED = "phone_spell_checker_suggestions"
        /** Its settings-search link. */
        const val LINK_ID = "auto_correction.phone_spell_checker"
        private const val MAX = 3

        fun isEnabled(context: Context): Boolean =
            SettingsManager.getPreferences(context).getBoolean(KEY_ENABLED, false)

        fun setEnabled(context: Context, enabled: Boolean) {
            SettingsManager.getPreferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        }

        /** A spell checker's answer for one word: its corrections if it looks misspelt, else null. */
        internal fun corrections(results: Array<out SentenceSuggestionsInfo>?): List<String>? {
            val info = results?.firstOrNull()?.takeIf { it.suggestionsCount > 0 }?.getSuggestionsInfoAt(0) ?: return null
            val typo = info.suggestionsAttributes and
                (SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS) != 0
            if (!typo) return null
            return (0 until info.suggestionsCount).mapNotNull { info.getSuggestionAt(it) }.filter { it.isNotBlank() }
        }
    }
}
