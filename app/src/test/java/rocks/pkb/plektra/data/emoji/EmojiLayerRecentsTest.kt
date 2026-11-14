package rocks.pkb.plektra.data.emoji

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rocks.pkb.plektra.EmojiPagesSettings
import rocks.pkb.plektra.SettingsManager
import rocks.pkb.plektra.backup.BackupPreferenceContract
import rocks.pkb.plektra.backup.PreferenceValueType
import rocks.pkb.plektra.core.SymLayoutController
import rocks.pkb.plektra.inputmethod.AlternateCharacterManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EmojiLayerRecentsTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val commits = mutableListOf<String>()
    private val connection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader, arrayOf(InputConnection::class.java)
    ) { _, method, args ->
        if (method.name == "commitText") commits.add(args!![0].toString())
        when (method.returnType) { java.lang.Boolean.TYPE -> true; java.lang.Integer.TYPE -> 0; else -> null }
    } as InputConnection

    @Before
    fun setUp() {
        SettingsManager.getPreferences(context).edit().clear().commit()
        // The classic layer: one emoji per key
        EmojiPagesSettings.setEmojiPagesEnabled(context, false)
        SettingsManager.setSymAutoClose(context, false)
    }

    @After
    fun tearDown() {
        SettingsManager.getPreferences(context).edit().clear().commit()
    }

    private val q = KeyEvent.KEYCODE_Q
    private val w = KeyEvent.KEYCODE_W
    private val e = KeyEvent.KEYCODE_E
    private val a = KeyEvent.KEYCODE_A

    @Test
    fun recentOnesTakeTheFirstKeysAndTheLayersOwnMoveAlong() {
        val layer = mapOf(a to "🍎", e to "🐱", w to "🐶", q to "😀")
        val arranged = EmojiLayerRecents.arrange(layer, listOf("🚀", "🐱"), count = 2)
        // Reading order Q W E … A: the recent ones first, the layer's own after, none twice
        assertEquals(mapOf(q to "🚀", w to "🐱", e to "😀", a to "🐶"), arranged)
    }

    @Test
    fun onlyTheChosenNumberAndKeysWithoutAnEmojiStayEmpty() {
        val layer = mapOf(q to "😀", e to "🐱")
        val arranged = EmojiLayerRecents.arrange(layer, listOf("🚀", "🌟", "🍕"), count = 1)
        assertEquals(mapOf(q to "🚀", e to "😀"), arranged)
    }

    @Test
    fun offByDefaultKeepsNothingAndIsBackedUp() {
        val layer = mapOf(q to "😀")
        EmojiLayerRecents.markUsed(context, "🚀")
        assertTrue(EmojiLayerRecents.used(context).isEmpty())
        assertEquals(layer, EmojiLayerRecents.layer(context, layer))
        assertEquals(PreferenceValueType.BOOLEAN, BackupPreferenceContract.expectedExportType("plektra_prefs", EmojiLayerRecents.KEY_ENABLED))
        assertEquals(PreferenceValueType.INT, BackupPreferenceContract.expectedExportType("plektra_prefs", EmojiLayerRecents.KEY_COUNT))
        assertTrue(EmojiLayerRecents.KEY_USED in BackupPreferenceContract.deliberatelyExcludedPlektraKeys)
    }

    @Test
    fun usedNewestFirstWithoutRepeats() {
        EmojiLayerRecents.setEnabled(context, true)
        listOf("😀", "🚀", "😀").forEach { EmojiLayerRecents.markUsed(context, it) }
        assertEquals(listOf("😀", "🚀"), EmojiLayerRecents.used(context))
    }

    @Test
    fun anEmojiTypedFromTheLayerComesFirstOnIt() {
        EmojiLayerRecents.setEnabled(context, true)
        val prefs = SettingsManager.getPreferences(context)
        val sym = SymLayoutController(context, prefs, AlternateCharacterManager(context.assets, prefs, context))
        sym.openEmojiPage()
        val own = sym.currentSymMappings()!!
        // The layer's last emoji in reading order, typed from the layer
        val key = "QWERTYUIOPASDFGHJKLZXCVBNM".map { KeyEvent.KEYCODE_A + (it - 'A') }.last { it in own }
        val typed = own.getValue(key)
        sym.handleKeyWhenActive(key, KeyEvent(0, 0, KeyEvent.ACTION_DOWN, key, 0), connection, false, false, {})
        assertEquals(listOf(typed), commits)

        val shown = sym.currentSymMappings()!!
        assertEquals(typed, shown[q])
        assertEquals("The layer's own Q moves along", own[q], shown[w])
        assertEquals(typed, sym.resolveChordSymbol(q, shiftPressed = false))
        assertEquals("Every key keeps an emoji", own.keys, shown.keys)
    }

    @Test
    fun aChordFromTheEmojiLayerCountsAsUsed() {
        EmojiLayerRecents.setEnabled(context, true)
        val prefs = SettingsManager.getPreferences(context)
        val sym = SymLayoutController(context, prefs, AlternateCharacterManager(context.assets, prefs, context))
        sym.openEmojiPage()
        sym.chordSymbolTyped("🚀")
        assertEquals(listOf("🚀"), EmojiLayerRecents.used(context))
    }
}
