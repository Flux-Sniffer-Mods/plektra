package rocks.pkb.plektra.core

import java.io.File
import java.io.IOException

/**
 * Writes the file whole, then swaps it in: a save cut short (a crash, the battery running out,
 * a storage error) leaves the previous version rather than a half-written file that reads as
 * empty or broken.
 */
fun File.writeBytesAtomically(bytes: ByteArray) {
    val temp = File(parentFile, "$name.tmp")
    try {
        temp.writeBytes(bytes)
        if (!temp.renameTo(this)) throw IOException("Couldn't replace $name")
    } finally {
        temp.delete()
    }
}

fun File.writeTextAtomically(text: String) = writeBytesAtomically(text.toByteArray(Charsets.UTF_8))
