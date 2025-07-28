package org.commonmark.parser

import org.commonmark.node.Node

/**
 * Parser for inline content (text, links, emphasized text, etc).
 */
interface InlineParser {
    /**
     * @param lines the source content to parse as inline
     * @param block the node to append resulting nodes to (as children)
     */
    fun parse(lines: SourceLines, block: Node)
}
