package org.commonmark.ext.latex

import org.commonmark.ext.latex.block.LatexBlockParserFactory
import org.commonmark.ext.latex.block.LatexBlockTextRenderer
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.text.TextContentNodeRendererContext
import org.commonmark.renderer.text.TextContentNodeRendererFactory
import org.commonmark.renderer.text.TextContentRenderer

class LatexExtension : Parser.ParserExtension, TextContentRenderer.TextContentRendererExtension {

    override fun extend(parserBuilder: Parser.Builder) {
        // 先添加块级解析器（优先级更高）
        parserBuilder.customBlockParserFactory(LatexBlockParserFactory())
        // 再添加内联解析器
        parserBuilder.customInlineContentParserFactory(LatexInlineContentParser.Factory())
    }

    override fun extend(rendererBuilder: TextContentRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : TextContentNodeRendererFactory {
            override fun create(context: TextContentNodeRendererContext): NodeRenderer {
                return LatexTextNodeRenderer(context)
            }
        })

        rendererBuilder.nodeRendererFactory(object : TextContentNodeRendererFactory {
            override fun create(context: TextContentNodeRendererContext): NodeRenderer {
                return LatexBlockTextRenderer(context)
            }
        })
    }

    companion object {
        fun create(): LatexExtension {
            return LatexExtension()
        }
    }
}
