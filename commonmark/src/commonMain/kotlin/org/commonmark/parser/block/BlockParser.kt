package org.commonmark.parser.block

import org.commonmark.node.Block
import org.commonmark.node.DefinitionMap
import org.commonmark.node.SourceSpan
import org.commonmark.parser.InlineParser
import org.commonmark.parser.SourceLine

/**
 * Parser for a specific block node.
 *
 *
 * Implementations should subclass [AbstractBlockParser] instead of implementing this directly.
 */
interface BlockParser {
    /**
     * Return true if the block that is parsed is a container (contains other blocks), or false if it's a leaf.
     */
    val isContainer: Boolean

    /**
     * Return true if the block can have lazy continuation lines.
     *
     *
     * Lazy continuation lines are lines that were rejected by this [.tryContinue] but didn't match
     * any other block parsers either.
     *
     *
     * If true is returned here, those lines will get added via [.addLine]. For false, the block is
     * closed instead.
     */
    fun canHaveLazyContinuationLines(): Boolean

    fun canContain(childBlock: Block): Boolean

    val block: Block

    fun tryContinue(parserState: ParserState): BlockContinue?

    /**
     * Add the part of a line that belongs to this block parser to parse (i.e. without any container block markers).
     * Note that the line will only include a [SourceLine.getSourceSpan] if source spans are enabled for inlines.
     */
    fun addLine(line: SourceLine)

    /**
     * Add a source span of the currently parsed block. The default implementation in [AbstractBlockParser] adds
     * it to the block. Unless you have some complicated parsing where you need to check source positions, you don't
     * need to override this.
     *
     * @since 0.16.0
     */
    fun addSourceSpan(sourceSpan: SourceSpan)

    /**
     * Return definitions parsed by this parser. The definitions returned here can later be accessed during inline
     * parsing via [org.commonmark.parser.InlineParserContext.getDefinition].
     */
    val definitions: List<DefinitionMap<*>>

    fun closeBlock()

    fun parseInlines(inlineParser: InlineParser)
}
