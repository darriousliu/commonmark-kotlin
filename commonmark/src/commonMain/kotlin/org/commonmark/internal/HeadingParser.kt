package org.commonmark.internal

import org.commonmark.internal.util.Parsing
import org.commonmark.node.Heading
import org.commonmark.parser.InlineParser
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.parser.beta.Scanner
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import org.commonmark.text.Characters

class HeadingParser(level: Int, private val content: SourceLines) : AbstractBlockParser() {
    override val block: Heading = Heading(level)

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        // In both ATX and Setext headings, once we have the heading markup, there's nothing more to parse.
        return BlockContinue.none()
    }

    override fun parseInlines(inlineParser: InlineParser) {
        inlineParser.parse(content, block)
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser
        ): BlockStart? {
            if (state.indent >= Parsing.CODE_BLOCK_INDENT) {
                return BlockStart.none()
            }

            val line = state.line
            val nextNonSpace = state.nextNonSpaceIndex
            if (line.content[nextNonSpace] == '#') {
                val atxHeading =
                    getAtxHeading(
                        line.substring(
                            nextNonSpace,
                            line.content.length
                        )
                    )
                if (atxHeading != null) {
                    return BlockStart.of(arrayOf(atxHeading)).atIndex(line.content.length)
                }
            }

            val setextHeadingLevel = getSetextHeadingLevel(line.content, nextNonSpace)
            if (setextHeadingLevel > 0) {
                val paragraph = matchedBlockParser.paragraphLines
                if (!paragraph.isEmpty) {
                    return BlockStart.of(arrayOf(HeadingParser(setextHeadingLevel, paragraph)))
                        .atIndex(line.content.length)
                        .replaceParagraphLines(paragraph.getLines().size)
                }
            }

            return BlockStart.none()
        }
    }

    companion object {
        // spec: An ATX heading consists of a string of characters, parsed as inline content, between an opening sequence of
        // 1-6 unescaped # characters and an optional closing sequence of any number of unescaped # characters. The opening
        // sequence of # characters must be followed by a space or by the end of line. The optional closing sequence of #s
        // must be preceded by a space and may be followed by spaces only.
        private fun getAtxHeading(line: SourceLine): HeadingParser? {
            val scanner = Scanner.of(SourceLines.of(line))
            val level = scanner.matchMultiple('#')

            if (level == 0 || level > 6) {
                return null
            }

            if (!scanner.hasNext()) {
                // End of line after markers is an empty heading
                return HeadingParser(level, SourceLines.empty())
            }

            val next: Char = scanner.peek()
            if (!(next == ' ' || next == '\t')) {
                return null
            }

            scanner.whitespace()
            val start = scanner.position()
            var end = start
            var hashCanEnd = true

            while (scanner.hasNext()) {
                val c: Char = scanner.peek()
                when (c) {
                    '#' -> if (hashCanEnd) {
                        scanner.matchMultiple('#')
                        val whitespace: Int = scanner.whitespace()
                        // If there are other characters, the hashes and spaces were part of the heading
                        if (scanner.hasNext()) {
                            end = scanner.position()
                        }
                        hashCanEnd = whitespace > 0
                    } else {
                        scanner.next()
                        end = scanner.position()
                    }

                    ' ', '\t' -> {
                        hashCanEnd = true
                        scanner.next()
                    }

                    else -> {
                        hashCanEnd = false
                        scanner.next()
                        end = scanner.position()
                    }
                }
            }

            val source: SourceLines = scanner.getSource(start, end)
            val content: String = source.content
            if (content.isEmpty()) {
                return HeadingParser(level, SourceLines.empty())
            }
            return HeadingParser(level, source)
        }

        // spec: A setext heading underline is a sequence of = characters or a sequence of - characters, with no more than
        // 3 spaces indentation and any number of trailing spaces.
        private fun getSetextHeadingLevel(line: CharSequence, index: Int): Int {
            when (line[index]) {
                '=' -> if (isSetextHeadingRest(line, index + 1, '=')) {
                    return 1
                }

                '-' -> if (isSetextHeadingRest(line, index + 1, '-')) {
                    return 2
                }
            }
            return 0
        }

        private fun isSetextHeadingRest(line: CharSequence, index: Int, marker: Char): Boolean {
            val afterMarker = Characters.skip(marker, line, index, line.length)
            val afterSpace = Characters.skipSpaceTab(line, afterMarker, line.length)
            return afterSpace >= line.length
        }
    }
}
