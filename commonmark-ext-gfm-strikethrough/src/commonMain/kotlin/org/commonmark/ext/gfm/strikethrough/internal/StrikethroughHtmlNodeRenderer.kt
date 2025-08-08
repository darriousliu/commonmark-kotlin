package org.commonmark.ext.gfm.strikethrough.internal

import org.commonmark.node.Node
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlWriter

class StrikethroughHtmlNodeRenderer(private val context: HtmlNodeRendererContext) :
    StrikethroughNodeRenderer() {
    private val html: HtmlWriter = context.writer

    override fun render(node: Node) {
        val attributes = context.extendAttributes(node, "del", mutableMapOf())
        html.tag("del", attributes)
        renderChildren(node)
        html.tag("/del")
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
