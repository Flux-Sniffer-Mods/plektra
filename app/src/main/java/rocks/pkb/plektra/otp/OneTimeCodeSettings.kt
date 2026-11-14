package rocks.pkb.plektra.otp

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import rocks.pkb.plektra.SettingsManager

/** The One-time codes switch, and Android's notification access it needs. */
object OneTimeCodeSettings {
    const val KEY = "one_time_codes"
    /** Its settings-search link. */
    const val LINK_ID = "advanced.one_time_codes"

    fun isEnabled(context: Context): Boolean = SettingsManager.getPreferences(context).getBoolean(KEY, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsManager.getPreferences(context).edit().putBoolean(KEY, enabled).apply()
    }

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Android's list of apps with notification access, where the user grants it. */
    fun openNotificationAccess(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
