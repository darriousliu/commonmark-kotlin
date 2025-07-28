package org.commonmark.internal

import org.commonmark.node.DefinitionMap
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.node.Paragraph
import org.commonmark.node.SourceSpan
import org.commonmark.parser.InlineParser
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.ParserState

class ParagraphParser : AbstractBlockParser() {
    override val block: Paragraph = Paragraph()
    private val linkReferenceDefinitionParser: LinkReferenceDefinitionParser =
        LinkReferenceDefinitionParser()

    override fun canHaveLazyContinuationLines(): Boolean {
        return true
    }

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        return if (!parserState.isBlank) {
            BlockContinue.atIndex(parserState.index)
        } else {
            BlockContinue.none()
        }
    }

    override fun addLine(line: SourceLine) {
        linkReferenceDefinitionParser.parse(line)
    }

    override fun addSourceSpan(sourceSpan: SourceSpan) {
        // Some source spans might belong to link reference definitions, others to the paragraph.
        // The parser will handle that.
        linkReferenceDefinitionParser.addSourceSpan(sourceSpan)
    }

    override val definitions: List<DefinitionMap<*>>
        get() {
            val map = DefinitionMap(LinkReferenceDefinition::class)
            for (def in linkReferenceDefinitionParser.getDefinitions()) {
                map.putIfAbsent(def.label, def)
            }
            return listOf(map)
        }

    override fun closeBlock() {
        for (def in linkReferenceDefinitionParser.getDefinitions()) {
            block.insertBefore(def)
        }

        if (linkReferenceDefinitionParser.getParagraphLines().isEmpty) {
            block.unlink()
        } else {
            block.setSourceSpans(linkReferenceDefinitionParser.paragraphSourceSpans)
        }
    }

    override fun parseInlines(inlineParser: InlineParser) {
        val lines = linkReferenceDefinitionParser.getParagraphLines()
        if (!lines.isEmpty) {
            inlineParser.parse(lines, block)
        }
    }

    val paragraphLines: SourceLines
        get() = linkReferenceDefinitionParser.getParagraphLines()

    fun removeLines(lines: Int): List<SourceSpan> {
        return linkReferenceDefinitionParser.removeLines(lines)
    }
}
