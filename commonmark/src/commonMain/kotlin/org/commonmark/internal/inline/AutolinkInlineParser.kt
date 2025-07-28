package org.commonmark.internal.inline

import org.commonmark.node.Link
import org.commonmark.node.Text
import org.commonmark.parser.SourceLines
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.ParsedInline
import org.commonmark.parser.beta.Scanner

/**
 * Attempt to parse an autolink (URL or email in pointy brackets).
 */
class AutolinkInlineParser : InlineContentParser {

    override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
        val scanner: Scanner = inlineParserState.scanner()
        scanner.next()
        val textStart = scanner.position()
        if (scanner.find('>') > 0) {
            val textSource: SourceLines = scanner.getSource(textStart, scanner.position())
            val content = textSource.content
            scanner.next()

            var destination: String? = null
            if (URI.matches(content)) {
                destination = content
            } else if (EMAIL.matches(content)) {
                destination = "mailto:$content"
            }

            if (destination != null) {
                val link = Link(destination, null)
                val text = Text(content)
                text.setSourceSpans(textSource.sourceSpans)
                link.appendChild(text)
                return ParsedInline.of(link, scanner.position())
            }
        }
        return ParsedInline.none()
    }

    class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char>
            get() = setOf('<')

        override fun create(): InlineContentParser {
            return AutolinkInlineParser()
        }
    }

    companion object {
        private val URI = Regex("^[a-zA-Z][a-zA-Z0-9.+-]{1,31}:[^<>\u0000-\u0020]*$")

        private val EMAIL =
            Regex("^([a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)$")
    }
}
