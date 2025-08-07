package org.commonmark.ext.footnotes.internal

import org.commonmark.ext.footnotes.FootnoteDefinition
import org.commonmark.node.Block
import org.commonmark.node.DefinitionMap
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockParserFactory
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import org.commonmark.text.Characters

/**
 * Parser for a single [FootnoteDefinition] block.
 */
class FootnoteBlockParser(label: String) : AbstractBlockParser() {
    override val block: FootnoteDefinition = FootnoteDefinition(label)

    override val isContainer: Boolean
        get() = true

    override fun canContain(childBlock: Block): Boolean {
        return true
    }

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        if (parserState.indent >= 4) {
            // It looks like content needs to be indented by 4 so that it's part of a footnote (instead of starting a new block).
            return BlockContinue.atColumn(4)
        } else {
            // We're not continuing to give other block parsers a chance to interrupt this definition.
            // But if no other block parser applied (including another FootnotesBlockParser), we will
            // accept the line via lazy continuation (same as a block quote).
            return BlockContinue.none()
        }
    }

    override val definitions: List<DefinitionMap<*>>
        get() {
            val map = DefinitionMap(FootnoteDefinition::class)
            map.putIfAbsent(block.label, block)
            return listOf(map)
        }

    class Factory : BlockParserFactory {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser
        ): BlockStart? {
            if (state.indent >= 4) {
                return BlockStart.none()
            }
            var index = state.nextNonSpaceIndex
            val content = state.line.content
            if (content[index] != '[' || index + 1 >= content.length) {
                return BlockStart.none()
            }
            index++
            if (content[index] != '^' || index + 1 >= content.length) {
                return BlockStart.none()
            }
            // Now at first label character (if any)
            index++
            val labelStart = index

            index = labelStart
            while (index < content.length) {
                val c = content[index]
                when (c) {
                    ']' -> if (index > labelStart && index + 1 < content.length && content[index + 1] == ':') {
                        val label = content.subSequence(labelStart, index).toString()
                        // After the colon, any number of spaces is skipped (not part of the content)
                        val afterSpaces =
                            Characters.skipSpaceTab(content, index + 2, content.length)
                        return BlockStart.of(arrayOf(FootnoteBlockParser(label)))
                            .atIndex(afterSpaces)
                    } else {
                        return BlockStart.none()
                    }

                    ' ', '\r', '\n', '\u0000', '\t' -> return BlockStart.none()
                }
                index++
            }

            return BlockStart.none()
        }
    }
}
