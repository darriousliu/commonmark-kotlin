package org.commonmark.ext.ins.internal

import org.commonmark.node.Node
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownWriter

class InsMarkdownNodeRenderer(private val context: MarkdownNodeRendererContext) :
    InsNodeRenderer() {
    private val writer: MarkdownWriter = context.writer

    override fun render(node: Node) {
        writer.raw("++")
        renderChildren(node)
        writer.raw("++")
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
