package org.commonmark.parser

import org.commonmark.node.SourceSpan

/**
 * A line or part of a line from the input source.
 *
 * @since 0.16.0
 */
class SourceLine private constructor(
    val content: CharSequence,
    private val sourceSpan: SourceSpan?
) {
    fun getSourceSpan(): SourceSpan? {
        return sourceSpan
    }

    fun substring(beginIndex: Int, endIndex: Int): SourceLine {
        val newContent = content.subSequence(beginIndex, endIndex)
        var newSourceSpan: SourceSpan? = null
        if (sourceSpan != null) {
            val length = endIndex - beginIndex
            if (length != 0) {
                val columnIndex: Int = sourceSpan.columnIndex + beginIndex
                val inputIndex: Int = sourceSpan.inputIndex + beginIndex
                newSourceSpan = SourceSpan.of(sourceSpan.lineIndex, columnIndex, inputIndex, length)
            }
        }
        return of(newContent, newSourceSpan)
    }

    companion object {
        fun of(content: CharSequence, sourceSpan: SourceSpan?): SourceLine {
            return SourceLine(content, sourceSpan)
        }
    }
}
