package rocks.pkb.plektra.inputmethod

/**
 * Terminal and SSH apps: there's no use for dictation in a shell, so the keyboard bar and its
 * menu leave the microphone button out while one is in front.
 */
object TerminalApps {
    private val PACKAGES = setOf(
        "com.termux",
        "com.termux.nix",
        "jackpal.androidterm",
        "com.termoneplus",
        "yarolegovich.materialterminal",
        "org.connectbot",
        "com.sonelli.juicessh",
        "com.server.auditor.ssh.client",
        "com.offsec.nhterm",
        "de.mud.terminal",
        "com.github.xtermjs.app",
        "io.github.mobilutils.mnt"
    )

    fun isTerminal(packageName: String?): Boolean = packageName != null && packageName in PACKAGES

    /** Whether the app whose field the keyboard is serving is a terminal. */
    @Volatile
    var inTerminal: Boolean = false
        private set

    fun onAppInFront(packageName: String?) {
        inTerminal = isTerminal(packageName)
    }

    /** The bar's buttons less the ones a terminal has no use for. */
    fun <T> withoutUnused(buttons: List<T>, idOf: (T) -> rocks.pkb.plektra.inputmethod.statusbar.StatusBarButtonId): List<T> =
        if (inTerminal) buttons.filterNot { idOf(it) == rocks.pkb.plektra.inputmethod.statusbar.StatusBarButtonId.Microphone } else buttons
}
