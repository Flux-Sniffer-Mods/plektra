package rocks.pkb.plektra

/**
 * A layer editor's key held and dragged onto another: the two swap what they type. A key with
 * nothing takes the other's, which is left empty.
 */
internal fun swapKeys(mappings: Map<Int, String>, from: Int, to: Int): Map<Int, String> {
    val result = mappings.toMutableMap()
    val a = mappings[from]
    val b = mappings[to]
    if (b != null) result[from] = b else result.remove(from)
    if (a != null) result[to] = a else result.remove(to)
    return result
}
