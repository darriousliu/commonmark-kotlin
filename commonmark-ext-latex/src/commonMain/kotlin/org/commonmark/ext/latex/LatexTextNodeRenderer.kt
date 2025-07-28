package org.commonmark.ext.latex

import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.text.TextContentNodeRendererContext
import kotlin.reflect.KClass

class LatexTextNodeRenderer(
    private val context: TextContentNodeRendererContext
) : NodeRenderer {

    override val nodeTypes: Set<KClass<out Node>> = setOf(LatexNode::class)

    override fun render(node: Node) {
        if (node is LatexNode) {
            val writer = context.writer
            // 对于行内 LaTeX 渲染，使用 \( 和 \) 或$ 和 $包裹
            writer.write(node.openingDelimiter)
            writer.write(node.latex)
            writer.write(node.closingDelimiter)
        }
    }
}
