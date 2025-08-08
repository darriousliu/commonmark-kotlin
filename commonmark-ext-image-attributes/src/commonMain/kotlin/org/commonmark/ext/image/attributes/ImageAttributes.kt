package org.commonmark.ext.image.attributes

import org.commonmark.node.CustomNode
import org.commonmark.node.Delimited

/**
 * A node containing text and other inline nodes as children.
 */
class ImageAttributes(val attributes: Map<String, String?>) : CustomNode(), Delimited {
    override val openingDelimiter: String?
        get() = "{"

    override val closingDelimiter: String?
        get() = "}"

    override fun toStringAttributes(): String? {
        return "imageAttributes=$attributes"
    }
}
