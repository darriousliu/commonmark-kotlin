package org.commonmark.internal

import org.commonmark.internal.util.Parsing
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Paragraph
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import org.commonmark.text.Characters

class IndentedCodeBlockParser : AbstractBlockParser() {
    override val block: IndentedCodeBlock = IndentedCodeBlock()
    private val lines: MutableList<CharSequence> = mutableListOf()

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        return when {
            parserState.indent >= Parsing.CODE_BLOCK_INDENT -> BlockContinue.atColumn(parserState.column + Parsing.CODE_BLOCK_INDENT)
            parserState.isBlank -> BlockContinue.atIndex(parserState.nextNonSpaceIndex)
            else -> BlockContinue.none()
        }
    }

    override fun addLine(line: SourceLine) {
        lines.add(line.content)
    }

    override fun closeBlock() {
        var lastNonBlank = lines.size - 1
        while (lastNonBlank >= 0) {
            if (!Characters.isBlank(lines[lastNonBlank])) {
                break
            }
            lastNonBlank--
        }

        val sb = StringBuilder()
        for (i in 0..<lastNonBlank + 1) {
            sb.append(lines[i])
            sb.append('\n')
        }

        val literal = sb.toString()
        block.literal = literal
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser
        ): BlockStart? {
            // An indented code block cannot interrupt a paragraph.
            return if (state.indent >= Parsing.CODE_BLOCK_INDENT && !state.isBlank && (state.activeBlockParser
                    .block !is Paragraph)
            ) {
                BlockStart.of(arrayOf(IndentedCodeBlockParser()))
                    .atColumn(state.column + Parsing.CODE_BLOCK_INDENT)
            } else {
                BlockStart.none()
            }
        }
    }
}

