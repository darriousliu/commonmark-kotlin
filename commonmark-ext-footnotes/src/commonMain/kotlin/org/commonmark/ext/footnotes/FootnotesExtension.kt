package org.commonmark.ext.footnotes

import org.commonmark.Extension
import org.commonmark.ext.footnotes.internal.FootnoteBlockParser
import org.commonmark.ext.footnotes.internal.FootnoteHtmlNodeRenderer
import org.commonmark.ext.footnotes.internal.FootnoteLinkProcessor
import org.commonmark.ext.footnotes.internal.FootnoteMarkdownNodeRenderer
import org.commonmark.parser.Parser
import org.commonmark.parser.Parser.ParserExtension
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlNodeRendererFactory
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.html.HtmlRenderer.HtmlRendererExtension
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownNodeRendererFactory
import org.commonmark.renderer.markdown.MarkdownRenderer
import org.commonmark.renderer.markdown.MarkdownRenderer.MarkdownRendererExtension

/**
 * Extension for footnotes with syntax like GitHub Flavored Markdown:
 * <pre>`
 * Some text with a footnote[^1].
 *
 * [^1]: The text of the footnote.
`</pre> *
 * The `[^1]` is a [FootnoteReference], with "1" being the label.
 *
 *
 * The line with `[^1]: ...` is a [FootnoteDefinition], with the contents as child nodes (can be a
 * paragraph like in the example, or other blocks like lists).
 *
 *
 * All the footnotes (definitions) will be rendered in a list at the end of a document, no matter where they appear in
 * the source. The footnotes will be numbered starting from 1, then 2, etc, depending on the order in which they appear
 * in the text (and not dependent on the label). The footnote reference is a link to the footnote, and from the footnote
 * there is a link back to the reference (or multiple).
 *
 *
 * There is also optional support for inline footnotes, use [.builder] and then set [Builder.inlineFootnotes].
 *
 * @see [GitHub docs for footnotes](https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax.footnotes)
 */
class FootnotesExtension private constructor(private val inlineFootnotes: Boolean) :
    ParserExtension, HtmlRendererExtension, MarkdownRendererExtension {

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder
            .customBlockParserFactory(FootnoteBlockParser.Factory())
            .linkProcessor(FootnoteLinkProcessor())
        if (inlineFootnotes) {
            parserBuilder.linkMarker('^')
        }
    }


    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : HtmlNodeRendererFactory {
            override fun create(context: HtmlNodeRendererContext): NodeRenderer {
                return FootnoteHtmlNodeRenderer(context)
            }
        })
    }

    override fun extend(rendererBuilder: MarkdownRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : MarkdownNodeRendererFactory {
            override fun create(context: MarkdownNodeRendererContext): NodeRenderer {
                return FootnoteMarkdownNodeRenderer(context)
            }

            override val specialCharacters: Set<Char>
                get() = setOf()
        })
    }

    class Builder {
        private var inlineFootnotes = false

        /**
         * Enable support for inline footnotes without definitions, e.g.:
         * <pre>
         * Some text^[this is an inline footnote]
        </pre> *
         */
        fun inlineFootnotes(inlineFootnotes: Boolean): Builder {
            this.inlineFootnotes = inlineFootnotes
            return this
        }

        fun build(): FootnotesExtension {
            return FootnotesExtension(inlineFootnotes)
        }
    }

    companion object {
        /**
         * The extension with the default configuration (no support for inline footnotes).
         */
        fun create(): Extension {
            return builder().build()
        }

        fun builder(): Builder {
            return Builder()
        }
    }
}
