package org.commonmark.internal.inline

import org.commonmark.internal.util.Escaping
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Text
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.ParsedInline

/**
 * Parse a backslash-escaped special character, adding either the escaped  character, a hard line break
 * (if the backslash is followed by a newline), or a literal backslash to the block's children.
 */
class BackslashInlineParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline {
        val scanner = inlineParserState.scanner()
        // Backslash
        scanner.next()

        val next = scanner.peek()
        if (next == '\n') {
            scanner.next()
            return ParsedInline.of(HardLineBreak(), scanner.position())
        } else if (ESCAPABLE.matches(next.toString())) {
            scanner.next()
            return ParsedInline.of(Text(next.toString()), scanner.position())
        } else {
            return ParsedInline.of(Text("\\"), scanner.position())
        }
    }

    class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char>
            get() = setOf('\\')

        override fun create(): InlineContentParser {
            return BackslashInlineParser()
        }
    }

    companion object {
        private val ESCAPABLE = Regex('^' + Escaping.ESCAPABLE)
    }
}
