package org.commonmark.renderer

import org.commonmark.node.Node

interface Renderer {
    /**
     * Render the tree of nodes to output.
     *
     * @param node the root node
     * @param output output for rendering
     */
    fun render(node: Node, output: Appendable)

    /**
     * Render the tree of nodes to string.
     *
     * @param node the root node
     * @return the rendered string
     */
    fun render(node: Node): String
}
