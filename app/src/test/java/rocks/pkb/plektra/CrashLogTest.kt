package rocks.pkb.plektra

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CrashLogTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private var saved: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setUp() {
        saved = Thread.getDefaultUncaughtExceptionHandler()
        CrashLog.clear(context)
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(saved)
        CrashLog.clear(context)
    }

    @Test
    fun aFailureIsKeptWithItsStack() {
        assertEquals("", CrashLog.read(context))
        CrashLog.record(context, "key down 45", IllegalStateException("no field"))
        val log = CrashLog.read(context)
        assertTrue(log.contains("key down 45"))
        assertTrue(log.contains("IllegalStateException: no field"))
        assertTrue("With where it happened", log.contains("CrashLogTest"))
        CrashLog.clear(context)
        assertEquals("", CrashLog.read(context))
    }

    @Test
    fun onlyTheNewestAreKept() {
        repeat(200) { CrashLog.record(context, "entry $it", RuntimeException("failure $it")) }
        val log = CrashLog.read(context)
        assertTrue(log.length <= CrashLog.MAX_BYTES)
        assertTrue(log.startsWith("--- "))
        assertTrue(log.contains("entry 199"))
        assertFalse(log.contains("entry 0\n"))
    }

    @Test
    fun anUncaughtFailureIsRecordedAndPassedOn() {
        val passedOn = mutableListOf<Throwable>()
        Thread.setDefaultUncaughtExceptionHandler { _, error -> passedOn += error }
        CrashLog.install(context)
        val error = IllegalArgumentException("bad layout")
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), error)
        assertEquals(listOf<Throwable>(error), passedOn)
        assertTrue(CrashLog.read(context).contains("IllegalArgumentException: bad layout"))
    }
}
