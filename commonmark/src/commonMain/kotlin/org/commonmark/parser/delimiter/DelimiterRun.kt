package org.commonmark.parser.delimiter

import org.commonmark.node.Text

/**
 * A delimiter run is one or more of the same delimiter character, e.g. `***`.
 */
interface DelimiterRun {
    /**
     * @return whether this can open a delimiter
     */
    fun canOpen(): Boolean

    /**
     * @return whether this can close a delimiter
     */
    fun canClose(): Boolean

    /**
     * @return the number of characters in this delimiter run (that are left for processing)
     */
    fun length(): Int

    /**
     * @return the number of characters originally in this delimiter run; at the start of processing, this is the same
     * as {[.length]}
     */
    fun originalLength(): Int

    /**
     * @return the innermost opening delimiter, e.g. for `***` this is the last `*`
     */
    val opener: Text

    /**
     * @return the innermost closing delimiter, e.g. for `***` this is the first `*`
     */
    val closer: Text

    /**
     * Get the opening delimiter nodes for the specified length of delimiters. Length must be between 1 and
     * [.length].
     *
     *
     * For example, for a delimiter run `***`, calling this with 1 would return the last `*`.
     * Calling it with 2 would return the second last `*` and the last `*`.
     */
    fun getOpeners(length: Int): Iterable<Text>

    /**
     * Get the closing delimiter nodes for the specified length of delimiters. Length must be between 1 and
     * [.length].
     *
     *
     * For example, for a delimiter run `***`, calling this with 1 would return the first `*`.
     * Calling it with 2 would return the first `*` and the second `*`.
     */
    fun getClosers(length: Int): Iterable<Text>
}
