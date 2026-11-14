package rocks.pkb.plektra.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import rocks.pkb.plektra.SettingsManager

/** A shortcut added to the quick launcher by hand, the way a home screen adds one. */
data class UserShortcut(val id: String, val label: String, val packageName: String, val intentUri: String)

/**
 * Shortcuts people add to the quick launcher from apps that offer them (a contact's direct dial,
 * a bookmark, a settings page). Only the default launcher may read the ones already on the home
 * screen, so each is added once here, through the offering app's own screen.
 */
object UserShortcuts {
    const val KEY = "quick_launcher_user_shortcuts"
    const val UNSUPPORTED_KEY = "quick_launcher_unsupported_shortcut_screens"
    /** Its settings-search link. */
    const val LINK_ID = "quick_launcher.add_shortcut"

    /** The launch action marking a hand-added shortcut: its intent is started as the app made it. */
    const val LAUNCH_ACTION = "rocks.pkb.plektra.USER_SHORTCUT"

    fun all(context: Context): List<UserShortcut> {
        val raw = SettingsManager.getPreferences(context).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                UserShortcut(o.getString("id"), o.getString("label"), o.getString("package"), o.getString("intent"))
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, shortcuts: List<UserShortcut>) {
        val array = JSONArray()
        shortcuts.forEach {
            array.put(JSONObject().put("id", it.id).put("label", it.label).put("package", it.packageName).put("intent", it.intentUri))
        }
        SettingsManager.getPreferences(context).edit().putString(KEY, array.toString()).apply()
    }

    /** A shortcut screen that only pins to the home screen: it isn't offered again. */
    fun markUnsupported(context: Context, component: String) {
        val prefs = SettingsManager.getPreferences(context)
        val set = prefs.getStringSet(UNSUPPORTED_KEY, emptySet()).orEmpty() + component
        prefs.edit().putStringSet(UNSUPPORTED_KEY, set).apply()
    }

    fun isUnsupported(context: Context, component: String): Boolean =
        component in SettingsManager.getPreferences(context).getStringSet(UNSUPPORTED_KEY, emptySet()).orEmpty()

    fun remove(context: Context, id: String) {
        save(context, all(context).filterNot { it.id == id })
        iconFile(context, id).delete()
    }

    private fun iconFile(context: Context, id: String) = File(File(context.filesDir, "user_shortcut_icons"), "$id.png")

    /** The shortcut's own icon, or its app's. */
    fun icon(context: Context, shortcut: UserShortcut): Drawable? {
        val file = iconFile(context, shortcut.id)
        if (file.exists()) {
            BitmapFactory.decodeFile(file.path)?.let { return BitmapDrawable(context.resources, it) }
        }
        return runCatching { context.packageManager.getApplicationIcon(shortcut.packageName) }.getOrNull()
    }

    /**
     * Keeps the shortcut an app's shortcut screen returned, or returns null when it can't be
     * opened from here (a newer-style shortcut only the home screen may start).
     */
    fun addFromResult(context: Context, packageName: String, data: Intent?): UserShortcut? {
        data ?: return null
        var label: String? = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
        var intent: Intent? = legacyIntent(data)
        val icon: Bitmap? = legacyIcon(data)

        if (intent == null) {
            val info = runCatching {
                context.getSystemService(LauncherApps::class.java)?.getPinItemRequest(data)?.shortcutInfo
            }.getOrNull()
            if (info != null) {
                label = label ?: (info.shortLabel ?: info.longLabel)?.toString()
                intent = runCatching { info.intent }.getOrNull()
                if (intent == null) {
                    // A shortcut the app also declares in its manifest can be started by its intent
                    AppActionDiscovery.discover(context, info.`package`).launcherShortcuts
                        .firstOrNull { it.id == info.id }
                        ?.let { action ->
                            intent = runCatching { Intent.parseUri(action.intentUri, Intent.URI_INTENT_SCHEME) }.getOrNull()
                            if (label.isNullOrBlank()) label = action.label
                        }
                }
            }
        }
        val target = intent ?: return null
        // Never one of the keyboard's own screens: an app can't open those through it
        if (opensOwnScreen(context, target)) return null
        val appName = runCatching {
            context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
        val shortcut = UserShortcut(
            id = UUID.randomUUID().toString(),
            label = label?.takeIf { it.isNotBlank() } ?: appName,
            packageName = packageName,
            intentUri = target.toUri(Intent.URI_INTENT_SCHEME)
        )
        icon?.let { bitmap ->
            runCatching {
                val file = iconFile(context, shortcut.id)
                file.parentFile?.mkdirs()
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        save(context, all(context) + shortcut)
        return shortcut
    }

    /**
     * The intent a stored shortcut starts: as its app made it, without any access to files it
     * might carry (stored intents can come back from elsewhere), and never one of the
     * keyboard's own screens. Null when it can't be started.
     */
    fun startableIntent(context: Context, intentUri: String): Intent? {
        val intent = runCatching { Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME) }.getOrNull() ?: return null
        intent.selector = null
        intent.removeFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        intent.clipData = null
        if (opensOwnScreen(context, intent)) return null
        // A contact's direct dial needs the phone permission home screens hold: without it,
        // the dialer opens with the number ready
        if (intent.action == Intent.ACTION_CALL &&
            context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            intent.action = Intent.ACTION_DIAL
            intent.component = null
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun opensOwnScreen(context: Context, intent: Intent): Boolean {
        val own = context.packageName
        if (intent.component?.packageName == own || intent.`package` == own) return true
        return runCatching {
            context.packageManager.queryIntentActivities(intent, 0).any { it.activityInfo.packageName == own }
        }.getOrDefault(false)
    }

    // Shortcuts made the way it was done before Android 8: the intent and icon as extras
    @Suppress("DEPRECATION")
    private fun legacyIntent(data: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)
        } else {
            data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT)
        }

    @Suppress("DEPRECATION")
    private fun legacyIcon(data: Intent): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON, Bitmap::class.java)
        } else {
            data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON)
        }
}
