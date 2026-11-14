package rocks.pkb.plektra.data.mappings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.KeyEvent
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import rocks.pkb.plektra.SettingsManager

/**
 * Emoji layer profiles (SYM customization): 26 emoji, one per letter key, for a situation.
 * Built-in ones cover common situations; you can save your own. Applying a profile makes it your
 * emoji layer. With "switch by app" (off by default), the layer follows the app you're in (chat
 * apps get Chatting, work apps Work, and so on) without changing your saved one.
 */
data class EmojiLayerProfile(
    val id: String,
    val name: String,
    val mappings: Map<Int, String>,
    val builtIn: Boolean = false
)

object EmojiLayerProfiles {
    const val PREF_KEY = "emoji_layer_profiles"
    const val PREF_ACTIVE = "emoji_layer_active_profile"
    const val PREF_SWITCH_BY_APP = "emoji_layer_switch_by_app"
    /** Settings-search links. */
    const val LINK_ID = "sym.emoji_profiles"
    const val SWITCH_BY_APP_LINK_ID = "sym.emoji_profiles_switch_by_app"

    /** The emoji layer's keys, in keyboard order (Q to P, A to L, Z to M). */
    val KEYS: List<Int> = listOf(
        KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_T,
        KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_P,
        KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G,
        KeyEvent.KEYCODE_H, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
        KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_B,
        KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_M
    )

    private fun layer(vararg emoji: String): Map<Int, String> {
        require(emoji.size == KEYS.size) { "An emoji layer has ${KEYS.size} keys" }
        return KEYS.zip(emoji).toMap()
    }

    // The most used emoji of each situation first, on the top row where they're easiest to reach
    val BUILT_IN: List<EmojiLayerProfile> = listOf(
        EmojiLayerProfile("everyday", "Everyday", layer(
            "😂", "❤️", "🤣", "👍", "😭", "🙏", "😘", "🥰", "😍", "😊",
            "🎉", "😁", "🥺", "😅", "🔥", "✨", "🙄", "😉", "👏",
            "😎", "🤔", "💯", "👀", "🤗", "👎", "😳"), builtIn = true),
        EmojiLayerProfile("chatting", "Chatting", layer(
            "😂", "❤️", "👍", "🙏", "😭", "🥺", "🔥", "✨", "😅", "🙌",
            "😊", "😍", "🤣", "😘", "🤔", "👀", "💀", "😎", "🥰",
            "👏", "💯", "🎉", "😢", "😉", "🙄", "🤷"), builtIn = true),
        EmojiLayerProfile("work", "Work", layer(
            "✅", "❌", "👍", "👀", "📅", "📌", "📎", "📝", "💡", "⚠️",
            "🙏", "👋", "☕", "⏰", "📞", "💬", "📊", "📈", "🚀",
            "🔥", "🎯", "🤝", "🔗", "📧", "👏", "🤔"), builtIn = true),
        EmojiLayerProfile("social", "Social media", layer(
            "🔥", "💯", "✨", "😍", "🙌", "💀", "😭", "👀", "🤩", "💅",
            "🥹", "🫶", "😂", "❤️", "📸", "🎥", "🎶", "🌈", "⭐",
            "🤣", "🥳", "👑", "💪", "😎", "🫡", "🤌"), builtIn = true),
        EmojiLayerProfile("love", "Love", layer(
            "❤️", "😍", "😘", "🥰", "💕", "💖", "💘", "💋", "🌹", "💐",
            "😊", "🥺", "🫶", "💞", "💓", "😻", "💑", "💍", "🤗",
            "☺️", "😚", "💌", "🍷", "🌙", "✨", "💝"), builtIn = true),
        EmojiLayerProfile("celebrations", "Celebrations", layer(
            "🎉", "🥳", "🎂", "🎁", "🎈", "🍾", "🥂", "🎊", "✨", "🎆",
            "🙌", "👏", "💃", "🕺", "🎶", "🍰", "🏆", "🥇", "🌟",
            "😄", "🤩", "💐", "🎀", "🍻", "🎇", "🙏"), builtIn = true),
        EmojiLayerProfile("food", "Food and drink", layer(
            "🍕", "🍔", "🍟", "🌮", "🍣", "🍜", "🍝", "🥗", "🍩", "🍪",
            "☕", "🍺", "🍷", "🍹", "🥤", "🍰", "🍫", "🍦", "🍓",
            "🍎", "🥑", "🍳", "🥐", "🧀", "🌶️", "😋"), builtIn = true),
        EmojiLayerProfile("travel", "Travel and outdoors", layer(
            "✈️", "🚗", "🚆", "🏖️", "🏔️", "🗺️", "🧳", "🏨", "📍", "🌍",
            "☀️", "🌧️", "❄️", "🌊", "🏕️", "🚴", "🥾", "📸", "🌅",
            "🚕", "⛽", "🎒", "🏝️", "🌴", "🧭", "⏰"), builtIn = true),
        EmojiLayerProfile("gaming", "Gaming", layer(
            "🎮", "🕹️", "👾", "🏆", "💀", "🔥", "⚔️", "🛡️", "🎯", "💎",
            "😤", "😂", "🤣", "😭", "🙃", "👀", "💪", "🤝", "😈",
            "🐐", "🎲", "🧩", "🏹", "⚡", "🎧", "💯"), builtIn = true)
    )

    fun all(context: Context): List<EmojiLayerProfile> = BUILT_IN + custom(context)

    fun custom(context: Context): List<EmojiLayerProfile> {
        val stored = prefs(context).getString(PREF_KEY, null) ?: return emptyList()
        val array = runCatching { JSONArray(stored) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(::fromJson) }
    }

    fun saveCustom(context: Context, profile: EmojiLayerProfile) {
        require(!profile.builtIn)
        val updated = custom(context).filterNot { it.id == profile.id } + profile
        prefs(context).edit().putString(PREF_KEY, JSONArray().apply { updated.forEach { put(toJson(it)) } }.toString()).apply()
    }

    fun deleteCustom(context: Context, id: String) {
        val updated = custom(context).filterNot { it.id == id }
        val edit = prefs(context).edit().putString(PREF_KEY, JSONArray().apply { updated.forEach { put(toJson(it)) } }.toString())
        if (activeId(context) == id) edit.remove(PREF_ACTIVE)
        edit.apply()
    }

    /** A new profile of your own from [mappings] (the emoji layer as it is now, say). */
    fun createCustom(context: Context, name: String, mappings: Map<Int, String>): EmojiLayerProfile =
        EmojiLayerProfile(UUID.randomUUID().toString().take(8), name, mappings.filterKeys { it in KEYS })
            .also { saveCustom(context, it) }

    /** Makes [profile] your emoji layer. */
    fun apply(context: Context, profile: EmojiLayerProfile) {
        SettingsManager.saveSymMappings(context, profile.mappings)
        prefs(context).edit().putString(PREF_ACTIVE, profile.id).apply()
    }

    fun activeId(context: Context): String? = prefs(context).getString(PREF_ACTIVE, null)

    fun switchByApp(context: Context): Boolean = prefs(context).getBoolean(PREF_SWITCH_BY_APP, false)

    fun setSwitchByApp(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(PREF_SWITCH_BY_APP, enabled).apply()
    }

    /** The emoji layer for [packageName] while switching by app; null keeps your own. */
    fun overrideFor(context: Context, packageName: String?): Map<Int, String>? =
        if (switchByApp(context)) forApp(context, packageName)?.mappings else null

    /**
     * The built-in profile for the app you're in, from a short list of well-known apps, then
     * the category the app gives itself; null keeps your own emoji layer.
     */
    fun forApp(context: Context, packageName: String?): EmojiLayerProfile? {
        packageName ?: return null
        val id = AppSituations.forPackage(packageName) ?: run {
            val category = runCatching { context.packageManager.getApplicationInfo(packageName, 0).category }
                .getOrDefault(ApplicationInfo.CATEGORY_UNDEFINED)
            AppSituations.forCategory(category)
        } ?: return null
        return BUILT_IN.firstOrNull { it.id == id }
    }

    fun toJson(profile: EmojiLayerProfile): JSONObject = JSONObject().apply {
        put("id", profile.id)
        put("name", profile.name)
        put("mappings", JSONObject().apply {
            KEYS.forEach { key -> profile.mappings[key]?.let { put(KeyEvent.keyCodeToString(key), it) } }
        })
    }

    fun fromJson(json: JSONObject): EmojiLayerProfile? {
        val mappingsJson = json.optJSONObject("mappings") ?: return null
        val mappings = mutableMapOf<Int, String>()
        mappingsJson.keys().forEach { name ->
            val key = KeyEvent.keyCodeFromString(name)
            val emoji = mappingsJson.optString(name)
            if (key in KEYS && emoji.isNotEmpty()) mappings[key] = emoji
        }
        val id = json.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString().take(8)
        return EmojiLayerProfile(id, json.optString("name").ifBlank { id }, mappings)
    }

    private fun prefs(context: Context) = SettingsManager.getPreferences(context)
}

/** Which built-in profile an app gets while the emoji layer switches by app. */
internal object AppSituations {
    private val CHAT = setOf(
        "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "org.thoughtcrime.securesms",
        "com.facebook.orca", "com.discord", "com.viber.voip", "jp.naver.line.android", "com.kakao.talk",
        "com.tencent.mm", "im.vector.app", "ch.threema.app", "ch.threema.app.libre",
        "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms",
        "com.snapchat.android"
    )
    private val WORK = setOf(
        "com.google.android.gm", "com.microsoft.office.outlook", "com.Slack", "com.microsoft.teams",
        "com.google.android.apps.dynamite", "us.zoom.videomeetings", "com.linkedin.android",
        "com.google.android.apps.docs", "com.google.android.apps.docs.editors.docs", "com.notion.id"
    )
    private val SOCIAL = setOf(
        "com.instagram.android", "com.zhiliaoapp.musically", "com.twitter.android", "com.facebook.katana",
        "com.reddit.frontpage", "com.pinterest", "com.bluesky.app", "org.joinmastodon.android",
        "com.instagram.barcelona"
    )
    private val LOVE = setOf("com.tinder", "com.bumble.app", "co.hinge.app", "com.okcupid.okcupid")
    private val FOOD = setOf(
        "com.ubercab.eats", "com.deliveroo.orderapp", "com.dd.doordash", "com.justeat.app.uk",
        "com.grubhub.android", "com.global.foodpanda.android", "com.wolt.android"
    )
    private val TRAVEL = setOf(
        "com.google.android.apps.maps", "com.waze", "com.booking", "com.airbnb.android",
        "com.ubercab", "com.tripadvisor.tripadvisor", "com.google.android.apps.travel.onthego"
    )

    fun forPackage(packageName: String): String? = when (packageName) {
        in CHAT -> "chatting"
        in WORK -> "work"
        in SOCIAL -> "social"
        in LOVE -> "love"
        in FOOD -> "food"
        in TRAVEL -> "travel"
        else -> null
    }

    fun forCategory(category: Int): String? = when (category) {
        ApplicationInfo.CATEGORY_SOCIAL -> "social"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "work"
        ApplicationInfo.CATEGORY_MAPS -> "travel"
        else -> null
    }
}
