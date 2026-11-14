package rocks.pkb.plektra

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A small crash log in the app's own storage: what ended the keyboard (or a thread of it), with
 * its stack, so a debug export can carry it after the fact. The newest entries are kept, up to
 * [MAX_BYTES]; nothing typed is written, only what failed and where.
 */
object CrashLog {
    private const val TAG = "PlektraCrash"
    private const val FILE_NAME = "crash_log.txt"
    internal const val MAX_BYTES = 64 * 1024
    private val lock = Any()

    private fun file(context: Context) = File(context.applicationContext.filesDir, FILE_NAME)

    /** Records failures that reach the default handler, then passes them on as before. */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { record(app, "uncaught on ${thread.name}", error) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun record(context: Context, what: String, error: Throwable) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())
        val entry = "--- $stamp $what\n${Log.getStackTraceString(error).trimEnd()}\n"
        synchronized(lock) {
            val target = file(context)
            try {
                val kept = (if (target.exists()) target.readText() else "") + entry
                // The newest entries, starting at an entry's beginning
                val trimmed = if (kept.length <= MAX_BYTES) kept else {
                    val tail = kept.takeLast(MAX_BYTES)
                    tail.indexOf("--- ").takeIf { it >= 0 }?.let { tail.substring(it) } ?: tail
                }
                target.writeText(trimmed)
            } catch (e: Exception) {
                Log.w(TAG, "crash log not written", e)
            }
        }
    }

    fun read(context: Context): String = synchronized(lock) {
        runCatching { file(context).takeIf { it.exists() }?.readText() }.getOrNull().orEmpty()
    }

    fun clear(context: Context) {
        synchronized(lock) { runCatching { file(context).delete() } }
    }
}
