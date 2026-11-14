package rocks.pkb.plektra.commands

import android.content.Context
import android.content.Intent
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.shortcuts.AppActionDiscovery

/**
 * Apps' own shortcuts, the ones their launcher icon shows on a long press ("New message",
 * "Scan QR code"): each is a quick launcher result of its own (off by default). Read from the
 * apps on the phone, and kept until an app updates.
 */
class AppShortcutCommandSource : CommandSource {
    override val id = CommandSourceId.AppActions

    private data class Cached(val updatedAt: Long, val commands: List<CommandTarget>)

    override fun getCommands(context: Context): List<CommandTarget> {
        if (!isEnabled(context)) return emptyList()
        val launchers = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { it.activityInfo.packageName }.distinct().filter { it != context.packageName }
        return launchers.flatMap { packageName -> commandsFor(context, packageName) }
    }

    private fun commandsFor(context: Context, packageName: String): List<CommandTarget> {
        val pm = context.packageManager
        val updatedAt = runCatching { pm.getPackageInfo(packageName, 0).lastUpdateTime }.getOrNull() ?: return emptyList()
        cache[packageName]?.takeIf { it.updatedAt == updatedAt }?.let { return it.commands }
        val appName = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }
            .getOrDefault(packageName)
        val icon = runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
        val commands = AppActionDiscovery.discover(context, packageName).launcherShortcuts.map { action ->
            CommandTarget(
                id = "shortcut:$packageName:${action.id}",
                source = id,
                kind = CommandKind.Shortcut,
                label = action.label,
                subtitle = appName,
                icon = CommandIcon.DrawableIcon(icon),
                launch = CommandLaunchSpec.IntentUri(action = Intent.ACTION_VIEW, packageName = packageName, intentUri = action.intentUri),
                capabilities = setOf(CommandCapability.SendsIntent, CommandCapability.RequiresInstalledPackage),
                defaultSurfaces = setOf(CommandSurface.QuickLauncher, CommandSurface.AssignedKey),
                searchTokens = listOf(action.label, appName, "$appName ${action.label}", "shortcut")
            )
        }
        cache[packageName] = Cached(updatedAt, commands)
        return commands
    }

    companion object {
        const val KEY_ENABLED = "quick_launcher_app_shortcuts"
        /** Its settings-search link. */
        const val LINK_ID = "quick_launcher.app_shortcuts"
        private val cache = java.util.concurrent.ConcurrentHashMap<String, Cached>()

        fun isEnabled(context: Context): Boolean =
            SettingsManager.getPreferences(context).getBoolean(KEY_ENABLED, false)

        fun setEnabled(context: Context, enabled: Boolean) {
            SettingsManager.getPreferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        }
    }
}
