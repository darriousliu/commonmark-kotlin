package org.commonmark.ext.ins

import org.commonmark.node.CustomNode
import org.commonmark.node.Delimited

/**
 * An ins node containing text and other inline nodes as children.
 */
class Ins : CustomNode(), Delimited {
    override val openingDelimiter: String
        get() = DELIMITER

    override val closingDelimiter: String
        get() = DELIMITER

    companion object {
        private const val DELIMITER = "++"
    }
}
