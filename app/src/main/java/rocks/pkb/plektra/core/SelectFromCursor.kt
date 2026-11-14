package rocks.pkb.plektra.core

/**
 * Selecting from the cursor (a command for an assigned key or Nav Mode): Nav Mode turns on and
 * its moves (arrows, Home and End, Page Up and Down, cursor swipes) extend the selection from
 * where the cursor was, until Nav Mode ends, whichever way it ends.
 */
object SelectFromCursor {
    @Volatile
    private var navModeActive: (() -> Boolean)? = null

    /** Whether moves extend the selection; false again as soon as Nav Mode is off. */
    val active: Boolean
        get() {
            val stillInNavMode = navModeActive ?: return false
            if (stillInNavMode()) return true
            navModeActive = null
            return false
        }

    fun start(navModeActive: () -> Boolean) {
        this.navModeActive = navModeActive
    }

    fun stop() {
        navModeActive = null
    }
}
