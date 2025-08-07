package org.commonmark.ext.footnotes.internal

import org.commonmark.ext.footnotes.FootnoteDefinition
import org.commonmark.ext.footnotes.FootnoteReference
import org.commonmark.ext.footnotes.InlineFootnote
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownWriter
import kotlin.reflect.KClass

class FootnoteMarkdownNodeRenderer(private val context: MarkdownNodeRendererContext) : NodeRenderer {
    private val writer: MarkdownWriter = context.writer

    override val nodeTypes: Set<KClass<out Node>>
        get() = setOf(
            FootnoteReference::class,
            InlineFootnote::class,
            FootnoteDefinition::class
        )

    override fun render(node: Node) {
        when (node) {
            is FootnoteReference -> {
                renderReference(node)
            }

            is InlineFootnote -> {
                renderInline(node)
            }

            is FootnoteDefinition -> {
                renderDefinition(node)
            }
        }
    }

    private fun renderReference(ref: FootnoteReference) {
        writer.raw("[^")
        // The label is parsed as-is without escaping, so we can render it back as-is
        writer.raw(ref.label)
        writer.raw("]")
    }

    private fun renderInline(inlineFootnote: InlineFootnote) {
        writer.raw("^[")
        renderChildren(inlineFootnote)
        writer.raw("]")
    }

    private fun renderDefinition(def: FootnoteDefinition) {
        writer.raw("[^")
        writer.raw(def.label)
        writer.raw("]: ")

        writer.pushPrefix("    ")
        writer.pushTight(true)
        renderChildren(def)
        writer.popTight()
        writer.popPrefix()
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
