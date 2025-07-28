package org.commonmark.ext.gfm.strikethrough.internal

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node.Node
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownWriter

class StrikethroughMarkdownNodeRenderer(private val context: MarkdownNodeRendererContext) :
    StrikethroughNodeRenderer() {
    private val writer: MarkdownWriter = context.writer

    override fun render(node: Node) {
        val strikethrough: Strikethrough = node as Strikethrough
        writer.raw(strikethrough.openingDelimiter)
        renderChildren(node)
        writer.raw(strikethrough.closingDelimiter)
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
