package org.commonmark.ext.ins

import org.commonmark.Extension
import org.commonmark.ext.ins.internal.InsDelimiterProcessor
import org.commonmark.ext.ins.internal.InsHtmlNodeRenderer
import org.commonmark.ext.ins.internal.InsMarkdownNodeRenderer
import org.commonmark.ext.ins.internal.InsTextContentNodeRenderer
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlNodeRendererFactory
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownNodeRendererFactory
import org.commonmark.renderer.markdown.MarkdownRenderer
import org.commonmark.renderer.text.TextContentNodeRendererContext
import org.commonmark.renderer.text.TextContentNodeRendererFactory
import org.commonmark.renderer.text.TextContentRenderer

/**
 * Extension for ins using ++
 *
 *
 * Create it with [.create] and then configure it on the builders
 * ([Parser.Builder.extensions],
 * [HtmlRenderer.Builder.extensions]).
 *
 *
 *
 * The parsed ins text regions are turned into [Ins] nodes.
 *
 */
class InsExtension private constructor() : Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension, TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customDelimiterProcessor(InsDelimiterProcessor())
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : HtmlNodeRendererFactory {
            override fun create(context: HtmlNodeRendererContext): NodeRenderer {
                return InsHtmlNodeRenderer(context)
            }
        })
    }

    override fun extend(rendererBuilder: TextContentRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : TextContentNodeRendererFactory {
            override fun create(context: TextContentNodeRendererContext): NodeRenderer {
                return InsTextContentNodeRenderer(context)
            }
        })
    }

    override fun extend(rendererBuilder: MarkdownRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : MarkdownNodeRendererFactory {
            override fun create(context: MarkdownNodeRendererContext): NodeRenderer {
                return InsMarkdownNodeRenderer(context)
            }

            override val specialCharacters: Set<Char>
                get() =// We technically don't need to escape single occurrences of +, but that's all the extension API
                    // exposes currently.
                    setOf('+')
        })
    }

    companion object {
        fun create(): Extension {
            return InsExtension()
        }
    }
}
