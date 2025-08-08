package org.commonmark.ext.front.matter

import org.commonmark.Extension
import org.commonmark.ext.front.matter.internal.YamlFrontMatterBlockParser
import org.commonmark.parser.Parser

/**
 * Extension for YAML-like metadata.
 *
 *
 * Create it with [.create] and then configure it on the builders
 * ([Parser.Builder.extensions],
 * [org.commonmark.renderer.html.HtmlRenderer.Builder.extensions]).
 *
 *
 *
 * The parsed metadata is turned into [YamlFrontMatterNode]. You can access the metadata using [YamlFrontMatterVisitor].
 *
 */
class YamlFrontMatterExtension private constructor() : Parser.ParserExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customBlockParserFactory(YamlFrontMatterBlockParser.Factory())
    }

    companion object {
        fun create(): Extension {
            return YamlFrontMatterExtension()
        }
    }
}
