package org.commonmark.parser.block

import org.commonmark.node.Block
import org.commonmark.node.DefinitionMap
import org.commonmark.node.SourceSpan
import org.commonmark.parser.InlineParser
import org.commonmark.parser.SourceLine

abstract class AbstractBlockParser : BlockParser {
    override val isContainer: Boolean = false

    override fun canHaveLazyContinuationLines(): Boolean {
        return false
    }

    override fun canContain(childBlock: Block): Boolean {
        return false
    }

    override fun addLine(line: SourceLine) {
    }

    override fun addSourceSpan(sourceSpan: SourceSpan) {
        block.addSourceSpan(sourceSpan)
    }

    override val definitions: List<DefinitionMap<*>> = emptyList()

    override fun closeBlock() {
    }

    override fun parseInlines(inlineParser: InlineParser) {
    }
}
