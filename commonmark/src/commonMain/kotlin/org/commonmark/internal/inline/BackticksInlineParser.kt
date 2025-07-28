package org.commonmark.internal.inline

import org.commonmark.node.Code
import org.commonmark.node.Text
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.ParsedInline
import org.commonmark.text.Characters

/**
 * Attempt to parse backticks, returning either a backtick code span or a literal sequence of backticks.
 */
class BackticksInlineParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline {
        val scanner = inlineParserState.scanner()
        val start = scanner.position()
        val openingTicks = scanner.matchMultiple('`')
        val afterOpening = scanner.position()

        while (scanner.find('`') > 0) {
            val beforeClosing = scanner.position()
            val count = scanner.matchMultiple('`')
            if (count == openingTicks) {
                var content = scanner.getSource(afterOpening, beforeClosing).content
                content = content.replace('\n', ' ')

                // spec: If the resulting string both begins and ends with a space character but does not consist
                // entirely of space characters, a single space character is removed from the front and back.
                if (content.length >= 3 && content[0] == ' ' && content[content.length - 1] == ' ' &&
                    Characters.hasNonSpace(content)
                ) {
                    content = content.substring(1, content.length - 1)
                }
                val node = Code(content)
                return ParsedInline.of(node, scanner.position())
            }
        }

        // If we got here, we didn't find a matching closing backtick sequence.
        val source = scanner.getSource(start, afterOpening)
        val text = Text(source.content)
        return ParsedInline.of(text, afterOpening)
    }

    class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char>
            get() = setOf('`')

        override fun create(): InlineContentParser {
            return BackticksInlineParser()
        }
    }
}
