package org.commonmark.node

import kotlin.jvm.JvmOverloads

/**
 * A source span references a snippet of text from the source input.
 *
 *
 * It has a starting position (line and column index) and a length of how many characters it spans.
 *
 *
 * For example, this CommonMark source text:
 * <pre>`
 * > foo
`</pre> *
 * The [BlockQuote] node would have this source span: line 0, column 0, length 5.
 *
 *
 * The [Paragraph] node inside it would have: line 0, column 2, length 3.
 *
 *
 * If a block has multiple lines, it will have a source span for each line.
 *
 *
 * Note that the column index and length are measured in Java characters (UTF-16 code units). If you're outputting them
 * to be consumed by another programming language, e.g. one that uses UTF-8 strings, you will need to translate them,
 * otherwise characters such as emojis will result in incorrect positions.
 *
 * @since 0.16.0
 */
class SourceSpan private constructor(
    lineIndex: Int,
    columnIndex: Int,
    inputIndex: Int,
    length: Int
) {
    /**
     * @return 0-based line index, e.g. 0 for first line, 1 for the second line, etc
     */
    val lineIndex: Int

    /**
     * @return 0-based index of column (character on line) in source, e.g. 0 for the first character of a line, 1 for
     * the second character, etc
     */
    val columnIndex: Int

    /**
     * @return 0-based index in whole input
     * @since 0.24.0
     */
    val inputIndex: Int

    /**
     * @return length of the span in characters
     */
    val length: Int

    init {
        require(lineIndex >= 0) { "lineIndex $lineIndex must be >= 0" }
        require(columnIndex >= 0) { "columnIndex $columnIndex must be >= 0" }
        require(inputIndex >= 0) { "inputIndex $inputIndex must be >= 0" }
        require(length >= 0) { "length $length must be >= 0" }
        this.lineIndex = lineIndex
        this.columnIndex = columnIndex
        this.inputIndex = inputIndex
        this.length = length
    }

    @JvmOverloads
    fun subSpan(beginIndex: Int, endIndex: Int = length): SourceSpan {
        if (beginIndex < 0) {
            throw IndexOutOfBoundsException("beginIndex $beginIndex + must be >= 0")
        }
        if (beginIndex > length) {
            throw IndexOutOfBoundsException("beginIndex $beginIndex must be <= length $length")
        }
        if (endIndex < 0) {
            throw IndexOutOfBoundsException("endIndex $endIndex + must be >= 0")
        }
        if (endIndex > length) {
            throw IndexOutOfBoundsException("endIndex $endIndex must be <= length $length")
        }
        if (beginIndex > endIndex) {
            throw IndexOutOfBoundsException("beginIndex $beginIndex must be <= endIndex $endIndex")
        }
        if (beginIndex == 0 && endIndex == length) {
            return this
        }
        return SourceSpan(
            lineIndex,
            columnIndex + beginIndex,
            inputIndex + beginIndex,
            endIndex - beginIndex
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || this::class !== other::class) {
            return false
        }
        val that = other as SourceSpan
        return lineIndex == that.lineIndex && columnIndex == that.columnIndex && inputIndex == that.inputIndex && length == that.length
    }

    override fun hashCode(): Int {
        return arrayOf(lineIndex, columnIndex, inputIndex, length).contentHashCode()
    }

    override fun toString(): String {
        return "SourceSpan{" +
                "line=" + lineIndex +
                ", column=" + columnIndex +
                ", input=" + inputIndex +
                ", length=" + length +
                "}"
    }

    companion object {
        fun of(line: Int, col: Int, input: Int, length: Int): SourceSpan {
            return SourceSpan(line, col, input, length)
        }

        @Deprecated("Use {{@link #of(int, int, int, int)}} instead to also specify input index. Using the deprecated one will set {@link #inputIndex} to 0.")
        fun of(lineIndex: Int, columnIndex: Int, length: Int): SourceSpan {
            return of(lineIndex, columnIndex, 0, length)
        }
    }
}
