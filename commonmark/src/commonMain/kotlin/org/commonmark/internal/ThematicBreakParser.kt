package org.commonmark.internal

import org.commonmark.node.ThematicBreak
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState

class ThematicBreakParser(literal: String?) : AbstractBlockParser() {
    override val block: ThematicBreak = ThematicBreak()

    init {
        block.literal = literal
    }

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        // a horizontal rule can never container > 1 line, so fail to match
        return BlockContinue.none()
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser
        ): BlockStart? {
            if (state.indent >= 4) {
                return BlockStart.none()
            }
            val nextNonSpace: Int = state.nextNonSpaceIndex
            val line: CharSequence = state.line.content
            if (isThematicBreak(line, nextNonSpace)) {
                val literal = line.subSequence(state.index, line.length).toString()
                return BlockStart.of(arrayOf(ThematicBreakParser(literal))).atIndex(line.length)
            } else {
                return BlockStart.none()
            }
        }
    }

    companion object {
        // spec: A line consisting of 0-3 spaces of indentation, followed by a sequence of three or more matching -, _, or *
        // characters, each followed optionally by any number of spaces, forms a thematic break.
        private fun isThematicBreak(line: CharSequence, index: Int): Boolean {
            var dashes = 0
            var underscores = 0
            var asterisks = 0
            val length: Int = line.length
            for (i in index..<length) {
                when (line[i]) {
                    '-' -> dashes++
                    '_' -> underscores++
                    '*' -> asterisks++
                    ' ', '\t' -> {}
                    else -> return false
                }
            }

            return ((dashes >= 3 && underscores == 0 && asterisks == 0) ||
                    (underscores >= 3 && dashes == 0 && asterisks == 0) ||
                    (asterisks >= 3 && dashes == 0 && underscores == 0))
        }
    }
}
