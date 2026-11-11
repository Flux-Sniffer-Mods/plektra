package rocks.pkb.plektra.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class AtomicFilesTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun writesTheWholeTextAndLeavesNoTemporaryFile() {
        val file = File(folder.root, "mappings.json")

        file.writeTextAtomically("{\"a\":1}")
        file.writeTextAtomically("{\"b\":2}")

        assertEquals("{\"b\":2}", file.readText())
        assertFalse(File(folder.root, "mappings.json.tmp").exists())
    }

    @Test
    fun aFailedSaveKeepsThePreviousVersion() {
        val file = File(folder.root, "user_defaults.json")
        file.writeTextAtomically("[\"kept\"]")
        // Something in the temporary file's place makes the write fail before the swap
        File(folder.root, "user_defaults.json.tmp").apply { mkdirs() }
        File(folder.root, "user_defaults.json.tmp/blocker").writeText("x")

        try {
            file.writeTextAtomically("[\"half")
            fail("The save should report its failure")
        } catch (expected: IOException) {
        }

        assertEquals("[\"kept\"]", file.readText())
    }

    @Test
    fun bytesAreWrittenAsGiven() {
        val file = File(folder.root, "custom_base.dict")
        val bytes = byteArrayOf(0, 1, 2, -1, 127)

        file.writeBytesAtomically(bytes)

        assertEquals(bytes.toList(), file.readBytes().toList())
    }
}
