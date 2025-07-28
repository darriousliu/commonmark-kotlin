package org.commonmark.ext.gfm.strikethrough

import org.commonmark.node.CustomNode
import org.commonmark.node.Delimited

/**
 * A strikethrough node containing text and other inline nodes as children.
 */
class Strikethrough(
    private val delimiter: String
) : CustomNode(), Delimited {
    override val closingDelimiter: String
        get() = delimiter
    override val openingDelimiter: String
        get() = delimiter
}
