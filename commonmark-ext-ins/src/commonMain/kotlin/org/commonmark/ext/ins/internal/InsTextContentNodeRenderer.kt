package org.commonmark.ext.ins.internal

import org.commonmark.node.Node
import org.commonmark.renderer.text.TextContentNodeRendererContext

class InsTextContentNodeRenderer(private val context: TextContentNodeRendererContext) :
    InsNodeRenderer() {
    override fun render(node: Node) {
        renderChildren(node)
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
