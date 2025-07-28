package org.commonmark.internal.inline

import org.commonmark.internal.util.Html5Entities
import org.commonmark.node.Text
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.ParsedInline
import org.commonmark.parser.beta.Position
import org.commonmark.parser.beta.Scanner
import org.commonmark.text.AsciiMatcher

/**
 * Attempts to parse an HTML entity or numeric character reference.
 */
class EntityInlineParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
        val scanner = inlineParserState.scanner()
        val start = scanner.position()
        // Skip `&`
        scanner.next()

        val c = scanner.peek()
        if (c == '#') {
            // Numeric
            scanner.next()
            if (scanner.next('x') || scanner.next('X')) {
                val digits = scanner.match(hex)
                if (1 <= digits && digits <= 6 && scanner.next(';')) {
                    return entity(scanner, start)
                }
            } else {
                val digits = scanner.match(dec)
                if (1 <= digits && digits <= 7 && scanner.next(';')) {
                    return entity(scanner, start)
                }
            }
        } else if (entityStart.matches(c)) {
            scanner.match(entityContinue)
            if (scanner.next(';')) {
                return entity(scanner, start)
            }
        }

        return ParsedInline.none()
    }

    private fun entity(scanner: Scanner, start: Position): ParsedInline {
        val text = scanner.getSource(start, scanner.position()).content
        return ParsedInline.of(Text(Html5Entities.entityToString(text)), scanner.position())
    }

    class Factory : InlineContentParserFactory {

        override val triggerCharacters: Set<Char>
            get() = setOf('&')

        override fun create(): InlineContentParser {
            return EntityInlineParser()
        }
    }

    companion object {
        private val hex =
            AsciiMatcher.builder().range('0', '9').range('A', 'F').range('a', 'f').build()
        private val dec = AsciiMatcher.builder().range('0', '9').build()
        private val entityStart = AsciiMatcher.builder().range('A', 'Z').range('a', 'z').build()
        private val entityContinue = entityStart.newBuilder().range('0', '9').build()
    }
}
