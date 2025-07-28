package org.commonmark.internal

import org.commonmark.internal.util.Escaping.unescapeString
import org.commonmark.internal.util.Parsing
import org.commonmark.node.FencedCodeBlock
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import org.commonmark.text.Characters

class FencedCodeBlockParser(
    private val fenceChar: Char,
    private val openingFenceLength: Int,
    fenceIndent: Int
) :
    AbstractBlockParser() {
    override val block = FencedCodeBlock()

    private var firstLine: String? = null
    private val otherLines = StringBuilder()

    init {
        block.fenceCharacter = fenceChar.toString()
        block.setOpeningFenceLength(openingFenceLength)
        block.fenceIndent = fenceIndent
    }


    override fun tryContinue(parserState: ParserState): BlockContinue {
        val nextNonSpace = parserState.nextNonSpaceIndex
        var newIndex = parserState.index
        val line = parserState.line.content
        if (parserState.indent < Parsing.CODE_BLOCK_INDENT && nextNonSpace < line.length && tryClosing(
                line,
                nextNonSpace
            )
        ) {
            // closing fence - we're at the end of the line, so we can finalize now
            return BlockContinue.finished()
        } else {
            // skip optional spaces of fence indent
            var i: Int = block.fenceIndent
            val length: Int = line.length
            while (i > 0 && newIndex < length && line[newIndex] == ' ') {
                newIndex++
                i--
            }
        }
        return BlockContinue.atIndex(newIndex)
    }

    override fun addLine(line: SourceLine) {
        if (firstLine == null) {
            firstLine = line.content.toString()
        } else {
            otherLines.append(line.content)
            otherLines.append('\n')
        }
    }

    override fun closeBlock() {
        // first line becomes info string
        block.info = unescapeString(firstLine.orEmpty().trim())
        block.literal = otherLines.toString()
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser
        ): BlockStart? {
            val indent: Int = state.indent
            if (indent >= Parsing.CODE_BLOCK_INDENT) {
                return BlockStart.none()
            }

            val nextNonSpace: Int = state.nextNonSpaceIndex
            val blockParser = checkOpener(
                state.line.content,
                nextNonSpace,
                indent
            )
            return if (blockParser != null) {
                BlockStart.of(arrayOf(blockParser))
                    .atIndex(nextNonSpace + blockParser.block.getOpeningFenceLength()!!)
            } else {
                BlockStart.none()
            }
        }
    }

    // spec: The content of the code block consists of all subsequent lines, until a closing code fence of the same type
    // as the code block began with (backticks or tildes), and with at least as many backticks or tildes as the opening
    // code fence.
    private fun tryClosing(line: CharSequence, index: Int): Boolean {
        val fences = Characters.skip(fenceChar, line, index, line.length) - index
        if (fences < openingFenceLength) {
            return false
        }
        // spec: The closing code fence [...] may be followed only by spaces, which are ignored.
        val after = Characters.skipSpaceTab(line, index + fences, line.length)
        if (after == line.length) {
            block.setClosingFenceLength(fences)
            return true
        }
        return false
    }

    companion object {
        // spec: A code fence is a sequence of at least three consecutive backtick characters (`) or tildes (~). (Tildes and
        // backticks cannot be mixed.)
        private fun checkOpener(
            line: CharSequence,
            index: Int,
            indent: Int
        ): FencedCodeBlockParser? {
            var backticks = 0
            var tildes = 0
            val length: Int = line.length
            loop@ for (i in index..<length) {
                when (line[i]) {
                    '`' -> backticks++
                    '~' -> tildes++
                    else -> break@loop
                }
            }
            if (backticks >= 3 && tildes == 0) {
                // spec: If the info string comes after a backtick fence, it may not contain any backtick characters.
                if (Characters.find('`', line, index + backticks) != -1) {
                    return null
                }
                return FencedCodeBlockParser('`', backticks, indent)
            } else if (tildes >= 3 && backticks == 0) {
                // spec: Info strings for tilde code blocks can contain backticks and tildes
                return FencedCodeBlockParser('~', tildes, indent)
            } else {
                return null
            }
        }
    }
}
