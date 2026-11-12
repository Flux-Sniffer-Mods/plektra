package rocks.pkb.plektra

import android.content.Context
import android.content.SharedPreferences

/**
 * Offline mode: nothing in Plektra goes online. Update checks and release notes are skipped,
 * dictionaries and layouts can't be downloaded, and voice input uses Android's recognition in
 * place of an online provider. Read when the app starts and kept up to date from the setting, so
 * code without a Context (the download managers, the speech clients) can check it too.
 */
object OfflineMode {
    const val KEY = "offline_mode"

    @Volatile
    var enabled: Boolean = false
        private set

    // Held here: SharedPreferences keeps its listeners only weakly
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var listenedTo: SharedPreferences? = null

    /** Reads the setting and follows it from then on, however it changes (a restored backup too). */
    fun load(context: Context) {
        val preferences = SettingsManager.getPreferences(context.applicationContext)
        enabled = preferences.getBoolean(KEY, false)
        listener?.let { listenedTo?.unregisterOnSharedPreferenceChangeListener(it) }
        listener = SharedPreferences.OnSharedPreferenceChangeListener { source, key ->
            if (key == null || key == KEY) enabled = source.getBoolean(KEY, false)
        }.also(preferences::registerOnSharedPreferenceChangeListener)
        listenedTo = preferences
    }

    fun isOn(context: Context): Boolean = SettingsManager.getPreferences(context).getBoolean(KEY, false)

    fun set(context: Context, on: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY, on).apply()
        enabled = on
    }

    /** For code under test that has no settings to read. */
    internal fun update(on: Boolean) {
        enabled = on
    }
}
