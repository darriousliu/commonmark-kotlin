package org.commonmark.parser.block

import org.commonmark.internal.BlockStartImpl

/**
 * Result object for starting parsing of a block, see static methods for constructors.
 */
abstract class BlockStart protected constructor() {
    /**
     * Continue parsing at the specified index.
     *
     * @param newIndex the new index, see [ParserState.getIndex]
     */
    abstract fun atIndex(newIndex: Int): BlockStart

    /**
     * Continue parsing at the specified column (for tab handling).
     *
     * @param newColumn the new column, see [ParserState.getColumn]
     */
    abstract fun atColumn(newColumn: Int): BlockStart

    @Deprecated(
        """use {@link #replaceParagraphLines(int)} instead; please raise an issue if that doesn't work for you
      for some reason."""
    )
    abstract fun replaceActiveBlockParser(): BlockStart

    /**
     * Replace a number of lines from the current paragraph (as returned by
     * [MatchedBlockParser.getParagraphLines]) with the new block.
     *
     *
     * This is useful for parsing blocks that start with normal paragraphs and only have special marker syntax in later
     * lines, e.g. in this:
     * <pre>
     * Foo
     * ===
    </pre> *
     * The `Foo` line is initially parsed as a normal paragraph, then `===` is parsed as a heading
     * marker, replacing the 1 paragraph line before. The end result is a single Heading block.
     *
     *
     * Note that source spans from the replaced lines are automatically added to the new block.
     *
     * @param lines the number of lines to replace (at least 1); use [Int.MAX_VALUE] to replace the whole
     * paragraph
     */
    abstract fun replaceParagraphLines(lines: Int): BlockStart

    companion object {
        /**
         * Result for when there is no block start.
         */
        fun none(): BlockStart? {
            return null
        }

        /**
         * Start block(s) with the specified parser(s).
         */
        fun of(blockParsers: Array<BlockParser>): BlockStart {
            return BlockStartImpl(blockParsers)
        }
    }
}
