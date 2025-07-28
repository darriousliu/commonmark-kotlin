package org.commonmark.ext.gfm.strikethrough

import org.commonmark.Extension
import org.commonmark.ext.gfm.strikethrough.internal.StrikethroughDelimiterProcessor
import org.commonmark.ext.gfm.strikethrough.internal.StrikethroughHtmlNodeRenderer
import org.commonmark.ext.gfm.strikethrough.internal.StrikethroughMarkdownNodeRenderer
import org.commonmark.ext.gfm.strikethrough.internal.StrikethroughTextContentNodeRenderer
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
 * Extension for GFM strikethrough using `~` or `~~` (GitHub Flavored Markdown).
 *
 * Example input:
 * <pre>`~foo~ or ~~bar~~`</pre>
 *
 * Example output (HTML):
 * <pre>`<del>foo</del> or <del>bar</del>`</pre>
 *
 *
 * Create the extension with [.create] and then add it to the parser and renderer builders
 * ([org.commonmark.parser.Parser.Builder.extensions],
 * [HtmlRenderer.Builder.extensions]).
 *
 *
 *
 * The parsed strikethrough text regions are turned into [Strikethrough] nodes.
 *
 *
 *
 * If you have another extension that only uses a single tilde (`~`) syntax, you will have to configure this
 * [StrikethroughExtension] to only accept the double tilde syntax, like this:
 *
 * <pre>
 * `StrikethroughExtension.builder().requireTwoTildes(true).build();
` *
</pre> *
 *
 *
 * If you don't do that, there's a conflict between the two extensions and you will get an
 * [IllegalArgumentException] when constructing the parser.
 *
 */
class StrikethroughExtension private constructor(builder: Builder) : Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension, TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    private val requireTwoTildes: Boolean

    init {
        this.requireTwoTildes = builder.requireTwoTildes
    }

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customDelimiterProcessor(StrikethroughDelimiterProcessor(requireTwoTildes))
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : HtmlNodeRendererFactory {
            override fun create(context: HtmlNodeRendererContext): NodeRenderer {
                return StrikethroughHtmlNodeRenderer(context)
            }
        })
    }

    override fun extend(rendererBuilder: TextContentRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : TextContentNodeRendererFactory {
            override fun create(context: TextContentNodeRendererContext): NodeRenderer {
                return StrikethroughTextContentNodeRenderer(context)
            }
        })
    }

    override fun extend(rendererBuilder: MarkdownRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : MarkdownNodeRendererFactory {
            override fun create(context: MarkdownNodeRendererContext): NodeRenderer {
                return StrikethroughMarkdownNodeRenderer(context)
            }

            override val specialCharacters: Set<Char>
                get() = setOf('~')
        })
    }

    class Builder {
        internal var requireTwoTildes = false

        /**
         * @param requireTwoTildes Whether two tilde characters (`~~`) are required for strikethrough or whether
         * one is also enough. Default is `false`; both a single tilde and two tildes can be used for strikethrough.
         * @return `this`
         */
        fun requireTwoTildes(requireTwoTildes: Boolean): Builder {
            this.requireTwoTildes = requireTwoTildes
            return this
        }

        /**
         * @return a configured extension
         */
        fun build(): Extension {
            return StrikethroughExtension(this)
        }
    }

    companion object {
        /**
         * @return the extension with default options
         */
        fun create(): Extension {
            return builder().build()
        }

        /**
         * @return a builder to configure the behavior of the extension
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}
