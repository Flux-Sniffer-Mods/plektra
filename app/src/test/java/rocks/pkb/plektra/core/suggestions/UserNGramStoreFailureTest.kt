package rocks.pkb.plektra.core.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UserNGramStoreFailureTest {

    @Test
    fun aDatabaseThatCannotOpenCostsTheSuggestionsNotTheKeyboard() {
        val context = RuntimeEnvironment.getApplication()
        val path = context.getDatabasePath("user_ngrams.db")
        path.parentFile?.mkdirs()
        path.delete()
        // A directory where the database file belongs: SQLite can't open it, as with storage
        // that is full, locked or damaged
        assertTrue(path.mkdirs())
        val store = UserNGramStore(context)
        try {
            store.learn("en-US", "good", "morning", nowMs = 1L)

            assertEquals(emptyList<UserNGramStore.Prediction>(), store.predict("en-US", "good", limit = 3))
            assertEquals(0, store.delete("en-US", "good", "morning"))
            assertEquals(0, store.deleteNextWord("en-US", "morning"))
            store.clearAll()
        } finally {
            store.close()
            path.deleteRecursively()
        }
    }
}
