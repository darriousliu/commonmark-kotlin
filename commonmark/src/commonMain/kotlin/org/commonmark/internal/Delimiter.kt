package org.commonmark.internal

import org.commonmark.node.Text
import org.commonmark.parser.delimiter.DelimiterRun

/**
 * Delimiter (emphasis, strong emphasis or custom emphasis).
 */
class Delimiter(
    val characters: MutableList<Text>,
    val delimiterChar: Char,
    // Can open emphasis, see spec.
    private val canOpen: Boolean,
    // Can close emphasis, see spec.
    private val canClose: Boolean,
    var previous: Delimiter?,
) : DelimiterRun {
    private val originalLength: Int = characters.size
    var next: Delimiter? = null

    override fun canOpen(): Boolean {
        return canOpen
    }

    override fun canClose(): Boolean {
        return canClose
    }

    override fun length(): Int {
        return characters.size
    }

    override fun originalLength(): Int {
        return originalLength
    }

    override val opener: Text
        get() = characters[characters.size - 1]

    override val closer: Text
        get() = characters[0]

    override fun getOpeners(length: Int): Iterable<Text> {
        require(length >= 1 && length <= length()) { "length must be between 1 and " + length() + ", was " + length }
        return characters.subList(characters.size - length, characters.size)
    }

    override fun getClosers(length: Int): Iterable<Text> {
        require(length >= 1 && length <= length()) { "length must be between 1 and " + length() + ", was " + length }
        return characters.subList(0, length)
    }
}
