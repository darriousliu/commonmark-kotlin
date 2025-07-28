package org.commonmark.parser.beta

import org.commonmark.internal.inline.ParsedInlineImpl
import org.commonmark.node.Node

/**
 * The result of a single inline parser. Use the static methods to create instances.
 *
 *
 * *This interface is not intended to be implemented by clients.*
 */
interface ParsedInline {
    companion object {
        fun none(): ParsedInline? {
            return null
        }

        fun of(node: Node, position: Position): ParsedInline {
            return ParsedInlineImpl(node, position)
        }
    }
}
