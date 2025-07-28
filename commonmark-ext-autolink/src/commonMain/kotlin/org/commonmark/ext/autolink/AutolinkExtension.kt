package org.commonmark.ext.autolink

import org.commonmark.Extension
import org.commonmark.ext.autolink.internal.AutolinkPostProcessor
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Extension for automatically turning plain URLs and email addresses into links.
 *
 *
 * Create it with [.create] and then configure it on the builders
 * ([Parser.Builder.extensions],
 * [HtmlRenderer.Builder.extensions]).
 *
 *
 *
 * The parsed links are turned into normal [org.commonmark.node.Link] nodes.
 *
 */
class AutolinkExtension private constructor() : Parser.ParserExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessor(AutolinkPostProcessor())
    }

    companion object {
        fun create(): Extension {
            return AutolinkExtension()
        }
    }
}
