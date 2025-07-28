package org.commonmark.node

/**
 * A list of source spans that can be added to. Takes care of merging adjacent source spans.
 *
 * @since 0.16.0
 */
class SourceSpans {
    private var sourceSpans: MutableList<SourceSpan>? = null

    fun getSourceSpans(): List<SourceSpan> {
        return sourceSpans ?: emptyList()
    }

    fun addAllFrom(nodes: Iterable<Node>) {
        for (node in nodes) {
            addAll(node.getSourceSpans())
        }
    }

    fun addAll(other: List<SourceSpan>) {
        if (other.isEmpty()) {
            return
        }

        val sourceSpans = sourceSpans ?: mutableListOf<SourceSpan>().also { sourceSpans = it }

        if (sourceSpans.isEmpty()) {
            sourceSpans.addAll(other)
        } else {
            val lastIndex = sourceSpans.size - 1
            val a = sourceSpans[lastIndex]
            val b = other[0]
            if (a.inputIndex + a.length == b.inputIndex) {
                sourceSpans[lastIndex] = SourceSpan.of(
                    a.lineIndex,
                    a.columnIndex,
                    a.inputIndex,
                    a.length + b.length
                )
                sourceSpans.addAll(other.subList(1, other.size))
            } else {
                sourceSpans.addAll(other)
            }
        }
    }

    companion object {
        fun empty(): SourceSpans {
            return SourceSpans()
        }
    }
}
