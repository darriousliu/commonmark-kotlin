package org.commonmark.parser.beta

/**
 * Position within a [Scanner]. This is intentionally kept opaque so as not to expose the internal structure of
 * the Scanner.
 */
class Position internal constructor(val lineIndex: Int, val index: Int)
