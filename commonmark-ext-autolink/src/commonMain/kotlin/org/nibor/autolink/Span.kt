package org.nibor.autolink

/**
 * A reference to a piece of the input text, either a link (see [LinkSpan]) or plain text.
 */
interface Span {
    /**
     * @return begin index (inclusive) in the original input that this link starts at
     */
    val beginIndex: Int

    /**
     * @return end index (exclusive) in the original input that this link ends at; in other words, index of first
     * character after link
     */
    val endIndex: Int
}
