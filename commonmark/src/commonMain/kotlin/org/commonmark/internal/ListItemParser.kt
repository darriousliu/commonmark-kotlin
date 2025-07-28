package org.commonmark.internal

import org.commonmark.node.Block
import org.commonmark.node.ListBlock
import org.commonmark.node.ListItem
import org.commonmark.node.Paragraph
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.ParserState

class ListItemParser(
    markerIndent: Int,
    /**
     * Minimum number of columns that the content has to be indented (relative to the containing block) to be part of
     * this list item.
     */
    private val contentIndent: Int
) : AbstractBlockParser() {
    override val block: ListItem = ListItem()

    private var hadBlankLine = false

    init {
        block.setMarkerIndent(markerIndent)
        block.setContentIndent(contentIndent)
    }

    override val isContainer: Boolean
        get() = true

    override fun canContain(childBlock: Block): Boolean {
        if (hadBlankLine) {
            // We saw a blank line in this list item, that means the list block is loose.
            //
            // spec: if any of its constituent list items directly contain two block-level elements with a blank line
            // between them
            val parent = block.getParent1()
            if (parent is ListBlock) {
                parent.isTight = false
            }
        }
        return true
    }

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        if (parserState.isBlank) {
            if (block.firstChild == null) {
                // Blank line after the empty list item
                return BlockContinue.none()
            } else {
                val activeBlock: Block? = parserState.activeBlockParser.block
                // If the active block is a code block, blank lines in it should not affect if the list is tight.
                hadBlankLine = activeBlock is Paragraph || activeBlock is ListItem
                return BlockContinue.atIndex(parserState.nextNonSpaceIndex)
            }
        }

        return if (parserState.indent >= contentIndent) {
            BlockContinue.atColumn(parserState.column + contentIndent)
        } else {
            // Note: We'll hit this case for lazy continuation lines, they will get added later.
            BlockContinue.none()
        }
    }
}
