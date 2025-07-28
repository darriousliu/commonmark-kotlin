package org.commonmark.ext.latex.block

import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.text.TextContentNodeRendererContext
import kotlin.reflect.KClass

class LatexBlockTextRenderer(private val context: TextContentNodeRendererContext) : NodeRenderer {
    override val nodeTypes: Set<KClass<out Node>> = setOf(LatexBlock::class)

    override fun render(node: Node) {
        val latexNode = node as LatexBlock
        val writer = context.writer

        writer.line()
        writer.write("\\[")
        writer.write(latexNode.latex)
        writer.write("\\]")
        writer.line()
    }
}
