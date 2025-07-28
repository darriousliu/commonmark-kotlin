package org.commonmark.internal.inline

import org.commonmark.node.Node
import org.commonmark.parser.beta.LinkResult
import org.commonmark.parser.beta.Position

class LinkResultImpl(val type: Type, private val node: Node, private val position: Position) :
    LinkResult {
    override fun includeMarker(): LinkResult {
        this.isIncludeMarker = true
        return this
    }

    enum class Type {
        WRAP,
        REPLACE
    }

    var isIncludeMarker: Boolean = false
        private set

    fun getNode(): Node {
        return node
    }

    fun getPosition(): Position {
        return position
    }
}
