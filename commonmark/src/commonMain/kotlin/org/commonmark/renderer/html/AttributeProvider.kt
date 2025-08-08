package org.commonmark.renderer.html

import org.commonmark.node.Node

/**
 * Extension point for adding/changing attributes on HTML tags for a node.
 */
interface AttributeProvider {
    /**
     * Set the attributes for a HTML tag of the specified node by modifying the provided map.
     *
     *
     * This allows to change or even remove default attributes. With great power comes great responsibility.
     *
     *
     * The attribute key and values will be escaped (preserving character entities), so don't escape them here,
     * otherwise they will be double-escaped.
     *
     *
     * This method may be called multiple times for the same node, if the node is rendered using multiple nested
     * tags (e.g. code blocks).
     *
     * @param node the node to set attributes for
     * @param tagName the HTML tag name that these attributes are for (e.g. `h1`, `pre`, `code`).
     * @param attributes the attributes, with any default attributes already set in the map
     */
    fun setAttributes(node: Node, tagName: String, attributes: MutableMap<String, String?>)
}
