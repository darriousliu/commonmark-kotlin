package org.commonmark.node

/**
 * Utility class for working with multiple [Node]s.
 *
 * @since 0.16.0
 */
object Nodes {
    /**
     * The nodes between (not including) start and end.
     */
    fun between(start: Node?, end: Node?): Iterable<Node> {
        return NodeIterable(start?.next, end)
    }

    private class NodeIterable(private val first: Node?, private val end: Node?) : Iterable<Node> {
        override fun iterator(): Iterator<Node> {
            return NodeIterator(first, end)
        }
    }

    private class NodeIterator(private var first: Node?, private var end: Node?) : Iterator<Node> {
        override fun hasNext(): Boolean {
            return first != null && first !== end
        }

        override fun next(): Node {
            if (!hasNext()) throw NoSuchElementException()
            val result = first!!
            first = first?.next
            return result
        }
    }
}

