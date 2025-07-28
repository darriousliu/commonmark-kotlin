package org.commonmark.ext.gfm.tables

import org.commonmark.Extension
import org.commonmark.ext.gfm.tables.internal.TableBlockParser
import org.commonmark.ext.gfm.tables.internal.TableHtmlNodeRenderer
import org.commonmark.ext.gfm.tables.internal.TableMarkdownNodeRenderer
import org.commonmark.ext.gfm.tables.internal.TableTextContentNodeRenderer
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
 * Extension for GFM tables using "|" pipes (GitHub Flavored Markdown).
 *
 *
 * Create it with [.create] and then configure it on the builders
 * ([Parser.Builder.extensions],
 * [HtmlRenderer.Builder.extensions]).
 *
 *
 *
 * The parsed tables are turned into [TableBlock] blocks.
 *
 *
 * @see [Tables
](https://github.github.com/gfm/.tables-extension-) */
class TablesExtension private constructor() : Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    TextContentRenderer.TextContentRendererExtension, MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customBlockParserFactory(TableBlockParser.Factory())
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : HtmlNodeRendererFactory {
            override fun create(context: HtmlNodeRendererContext): NodeRenderer {
                return TableHtmlNodeRenderer(context)
            }
        })
    }

    override fun extend(rendererBuilder: TextContentRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : TextContentNodeRendererFactory {
            override fun create(context: TextContentNodeRendererContext): NodeRenderer {
                return TableTextContentNodeRenderer(context)
            }
        })
    }

    override fun extend(rendererBuilder: MarkdownRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : MarkdownNodeRendererFactory {
            override fun create(context: MarkdownNodeRendererContext): NodeRenderer {
                return TableMarkdownNodeRenderer(context)
            }

            override val specialCharacters: Set<Char>
                get() = setOf('|')
        })
    }

    companion object {
        fun create(): Extension {
            return TablesExtension()
        }
    }
}
