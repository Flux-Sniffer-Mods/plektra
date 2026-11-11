package rocks.pkb.plektra

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * Opening a link or another app's page (a browser, the Play Store): when no installed app can
 * open it (a phone without a browser, or with it disabled), a short message says so instead of
 * Android ending the settings screen or the keyboard with ActivityNotFoundException.
 */
object ExternalLinks {
    private const val TAG = "ExternalLinks"

    /** Starts [intent]; returns false, after saying so, when no app can handle it. */
    fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No app for ${intent.action} ${intent.data?.scheme}", e)
        Toast.makeText(context, R.string.external_link_no_app, Toast.LENGTH_SHORT).show()
        false
    }
}
