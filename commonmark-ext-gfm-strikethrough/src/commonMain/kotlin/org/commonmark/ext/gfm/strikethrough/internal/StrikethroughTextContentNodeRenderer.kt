package org.commonmark.ext.gfm.strikethrough.internal

import org.commonmark.node.Node
import org.commonmark.renderer.text.TextContentNodeRendererContext
import org.commonmark.renderer.text.TextContentWriter

class StrikethroughTextContentNodeRenderer(private val context: TextContentNodeRendererContext) :
    StrikethroughNodeRenderer() {
    private val textContent: TextContentWriter = context.writer

    override fun render(node: Node) {
        textContent.write('/')
        renderChildren(node)
        textContent.write('/')
    }

    private fun renderChildren(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            context.render(node)
            node = next
        }
    }
}
