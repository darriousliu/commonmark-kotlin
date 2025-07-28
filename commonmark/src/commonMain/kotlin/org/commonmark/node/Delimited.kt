package org.commonmark.node

/**
 * A node that uses delimiters in the source form (e.g. `*bold*`).
 */
interface Delimited {
    /**
     * @return the opening (beginning) delimiter, e.g. `*`
     */
    val openingDelimiter: String?

    /**
     * @return the closing (ending) delimiter, e.g. `*`
     */
    val closingDelimiter: String?
}
