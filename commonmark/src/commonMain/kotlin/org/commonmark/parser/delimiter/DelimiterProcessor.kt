package org.commonmark.parser.delimiter

/**
 * Custom delimiter processor for additional delimiters besides `_` and `*`.
 *
 *
 * Note that implementations of this need to be thread-safe, the same instance may be used by multiple parsers.
 *
 * @see org.commonmark.parser.beta.InlineContentParserFactory
 */
interface DelimiterProcessor {
    /**
     * @return the character that marks the beginning of a delimited node, must not clash with any built-in special
     * characters
     */
    val openingCharacter: Char

    /**
     * @return the character that marks the the ending of a delimited node, must not clash with any built-in special
     * characters. Note that for a symmetric delimiter such as "*", this is the same as the opening.
     */
    val closingCharacter: Char

    /**
     * Minimum number of delimiter characters that are needed to activate this. Must be at least 1.
     */
    val minLength: Int

    /**
     * Process the delimiter runs.
     *
     *
     * The processor can examine the runs and the nodes and decide if it wants to process or not. If not, it should not
     * change any nodes and return 0. If yes, it should do the processing (wrapping nodes, etc) and then return how many
     * delimiters were used.
     *
     *
     * Note that removal (unlinking) of the used delimiter [Text] nodes is done by the caller.
     *
     * @param openingRun the opening delimiter run
     * @param closingRun the closing delimiter run
     * @return how many delimiters were used; must not be greater than length of either opener or closer
     */
    fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int
}
