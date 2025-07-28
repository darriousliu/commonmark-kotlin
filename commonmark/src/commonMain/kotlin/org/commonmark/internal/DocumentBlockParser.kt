package org.commonmark.internal

import org.commonmark.node.Block
import org.commonmark.node.Document
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.ParserState

class DocumentBlockParser : AbstractBlockParser() {
    private val document = Document()

    override val isContainer: Boolean = true

    override fun canContain(childBlock: Block): Boolean {
        return true
    }

    override val block: Document
        get() = document

    override fun tryContinue(parserState: ParserState): BlockContinue {
        return BlockContinue.atIndex(parserState.index)
    }

    override fun addLine(line: SourceLine) {
    }
}
