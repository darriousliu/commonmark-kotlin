package org.commonmark.internal.inline

import org.commonmark.node.HtmlInline
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.ParsedInline
import org.commonmark.parser.beta.Position
import org.commonmark.parser.beta.Scanner
import org.commonmark.text.AsciiMatcher

/**
 * Attempt to parse inline HTML.
 */
class HtmlInlineParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
        val scanner = inlineParserState.scanner()
        val start = scanner.position()
        // Skip over `<`
        scanner.next()

        var c = scanner.peek()
        if (tagNameStart.matches(c)) {
            if (tryOpenTag(scanner)) {
                return htmlInline(start, scanner)
            }
        } else if (c == '/') {
            if (tryClosingTag(scanner)) {
                return htmlInline(start, scanner)
            }
        } else if (c == '?') {
            if (tryProcessingInstruction(scanner)) {
                return htmlInline(start, scanner)
            }
        } else if (c == '!') {
            // comment, declaration or CDATA
            scanner.next()
            c = scanner.peek()
            if (c == '-') {
                if (tryComment(scanner)) {
                    return htmlInline(start, scanner)
                }
            } else if (c == '[') {
                if (tryCdata(scanner)) {
                    return htmlInline(start, scanner)
                }
            } else if (asciiLetter.matches(c)) {
                if (tryDeclaration(scanner)) {
                    return htmlInline(start, scanner)
                }
            }
        }

        return ParsedInline.none()
    }

    class Factory : InlineContentParserFactory {

        override val triggerCharacters: Set<Char>
            get() = setOf('<')

        override fun create(): InlineContentParser {
            return HtmlInlineParser()
        }
    }

    companion object {
        private val asciiLetter = AsciiMatcher.builder().range('A', 'Z').range('a', 'z').build()

        // spec: A tag name consists of an ASCII letter followed by zero or more ASCII letters, digits, or hyphens (-).
        private val tagNameStart = asciiLetter
        private val tagNameContinue = tagNameStart.newBuilder().range('0', '9').c('-').build()

        // spec: An attribute name consists of an ASCII letter, _, or: followed by zero or more ASCII letters, digits,
        // _, ., :, or -. (Note: This is the XML specification restricted to ASCII. HTML5 is laxer.)
        private val attributeStart = asciiLetter.newBuilder().c('_').c(':').build()
        private val attributeContinue =
            attributeStart.newBuilder().range('0', '9').c('.').c('-').build()

        // spec: An unquoted attribute value is a nonempty string of characters not including whitespace, ", ', =, <, >, or `.
        private val attributeValueEnd = AsciiMatcher.builder()
            .c(' ').c('\t').c('\n').c('\u000B').c('\u000c').c('\r')
            .c('"').c('\'').c('=').c('<').c('>').c('`')
            .build()

        private fun htmlInline(start: Position, scanner: Scanner): ParsedInline {
            val text = scanner.getSource(start, scanner.position()).content
            val node = HtmlInline(text)
            return ParsedInline.of(node, scanner.position())
        }

        private fun tryOpenTag(scanner: Scanner): Boolean {
            // spec: An open tag consists of a < character, a tag name, zero or more attributes, optional whitespace,
            // an optional / character, and a > character.
            scanner.next()
            scanner.match(tagNameContinue)
            var whitespace = scanner.whitespace() >= 1
            // spec: An attribute consists of whitespace, an attribute name, and an optional attribute value specification.
            while (whitespace && scanner.match(attributeStart) >= 1) {
                scanner.match(attributeContinue)
                // spec: An attribute value specification consists of optional whitespace, a = character,
                // optional whitespace, and an attribute value.
                whitespace = scanner.whitespace() >= 1
                if (scanner.next('=')) {
                    scanner.whitespace()
                    val valueStart: Char = scanner.peek()
                    if (valueStart == '\'') {
                        scanner.next()
                        if (scanner.find('\'') < 0) {
                            return false
                        }
                        scanner.next()
                    } else if (valueStart == '"') {
                        scanner.next()
                        if (scanner.find('"') < 0) {
                            return false
                        }
                        scanner.next()
                    } else {
                        if (scanner.find(attributeValueEnd) <= 0) {
                            return false
                        }
                    }

                    // Whitespace is required between attributes
                    whitespace = scanner.whitespace() >= 1
                }
            }

            scanner.next('/')
            return scanner.next('>')
        }

        private fun tryClosingTag(scanner: Scanner): Boolean {
            // spec: A closing tag consists of the string </, a tag name, optional whitespace, and the character >.
            scanner.next()
            if (scanner.match(tagNameStart) >= 1) {
                scanner.match(tagNameContinue)
                scanner.whitespace()
                return scanner.next('>')
            }
            return false
        }

        private fun tryProcessingInstruction(scanner: Scanner): Boolean {
            // spec: A processing instruction consists of the string <?, a string of characters not including the string ?>,
            // and the string ?>.
            scanner.next()
            while (scanner.find('?') > 0) {
                scanner.next()
                if (scanner.next('>')) {
                    return true
                }
            }
            return false
        }

        private fun tryComment(scanner: Scanner): Boolean {
            // spec: An [HTML comment](@) consists of `<!-->`, `<!--->`, or  `<!--`, a string of
            // characters not including the string `-->`, and `-->` (see the
            // [HTML spec](https://html.spec.whatwg.org/multipage/parsing.html#markup-declaration-open-state)).

            // Skip first `-`

            scanner.next()
            if (!scanner.next('-')) {
                return false
            }

            if (scanner.next('>') || scanner.next("->")) {
                return true
            }

            while (scanner.find('-') >= 0) {
                if (scanner.next("-->")) {
                    return true
                } else {
                    scanner.next()
                }
            }

            return false
        }

        private fun tryCdata(scanner: Scanner): Boolean {
            // spec: A CDATA section consists of the string <![CDATA[, a string of characters not including the string ]]>,
            // and the string ]]>.

            // Skip `[`

            scanner.next()

            if (scanner.next("CDATA[")) {
                while (scanner.find(']') >= 0) {
                    if (scanner.next("]]>")) {
                        return true
                    } else {
                        scanner.next()
                    }
                }
            }

            return false
        }

        private fun tryDeclaration(scanner: Scanner): Boolean {
            // spec: A declaration consists of the string <!, an ASCII letter, zero or more characters not including
            // the character >, and the character >.
            scanner.match(asciiLetter)
            if (scanner.whitespace() <= 0) {
                return false
            }
            if (scanner.find('>') >= 0) {
                scanner.next()
                return true
            }
            return false
        }
    }
}
