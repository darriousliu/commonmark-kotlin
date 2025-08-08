package org.commonmark.ext.image.attributes.internal

import org.commonmark.ext.image.attributes.ImageAttributes
import org.commonmark.node.Image
import org.commonmark.node.Node
import org.commonmark.node.Nodes
import org.commonmark.node.Text
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

class ImageAttributesDelimiterProcessor : DelimiterProcessor {
    override val openingCharacter: Char
        get() = '{'

    override val closingCharacter: Char
        get() = '}'

    override val minLength: Int
        get() = 1

    override fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int {
        if (openingRun.length() != 1) {
            return 0
        }

        // Check if the attributes can be applied - if the previous node is an Image, and if all the attributes are in
        // the set of SUPPORTED_ATTRIBUTES
        val opener: Text = openingRun.opener
        val nodeToStyle = opener.previous
        if (nodeToStyle !is Image) {
            return 0
        }

        val toUnlink = ArrayList<Node>()
        val content = StringBuilder()

        for (node in Nodes.between(opener, closingRun.closer)) {
            // Only Text nodes can be used for attributes
            if (node is Text) {
                content.append(node.literal)
                toUnlink.add(node)
            } else {
                // This node type is not supported, so stop here (no need to check any further ones).
                return 0
            }
        }

        val attributesMap = LinkedHashMap<String, String?>()
        val attributes = content.toString()
        for (s in attributes.split("\\s+")) {
            val attribute = s.split("=")
            if (attribute.size > 1 && SUPPORTED_ATTRIBUTES.contains(attribute[0].lowercase())) {
                attributesMap.put(attribute[0], attribute[1])
            } else {
                // This attribute is not supported, so stop here (no need to check any further ones).
                return 0
            }
        }

        // Unlink the tmp nodes
        for (node in toUnlink) {
            node.unlink()
        }

        if (attributesMap.isNotEmpty()) {
            val imageAttributes = ImageAttributes(attributesMap)

            // The new node is added as a child of the image node to which the attributes apply.
            nodeToStyle.appendChild(imageAttributes)
        }

        return 1
    }

    companion object {
        // Only allow a defined set of attributes to be used.
        private val SUPPORTED_ATTRIBUTES: Set<String> = setOf("width", "height")
    }
}
