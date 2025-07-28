package org.commonmark.parser

import org.commonmark.node.SourceSpan

/**
 * A set of lines ([SourceLine]) from the input source.
 *
 * @since 0.16.0
 */
class SourceLines {
    private val lines: MutableList<SourceLine> = ArrayList()

    fun addLine(sourceLine: SourceLine) {
        lines.add(sourceLine)
    }

    fun getLines(): List<SourceLine> {
        return lines
    }

    val isEmpty: Boolean
        get() = lines.isEmpty()

    val content: String
        get() {
            val sb = StringBuilder()
            for (i in 0..<lines.size) {
                if (i != 0) {
                    sb.append('\n')
                }
                sb.append(lines[i].content)
            }
            return sb.toString()
        }

    val sourceSpans: List<SourceSpan>
        get() {
            val sourceSpans = mutableListOf<SourceSpan>()
            for (line in lines) {
                val sourceSpan = line.getSourceSpan()
                if (sourceSpan != null) {
                    sourceSpans.add(sourceSpan)
                }
            }
            return sourceSpans
        }

    companion object {
        fun empty(): SourceLines {
            return SourceLines()
        }

        fun of(sourceLine: SourceLine): SourceLines {
            val sourceLines = SourceLines()
            sourceLines.addLine(sourceLine)
            return sourceLines
        }

        fun of(sourceLines: List<SourceLine>): SourceLines {
            val result = SourceLines()
            result.lines.addAll(sourceLines)
            return result
        }
    }
}
