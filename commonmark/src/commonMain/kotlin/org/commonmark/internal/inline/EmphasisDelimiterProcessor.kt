package org.commonmark.internal.inline

import org.commonmark.node.*
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

abstract class EmphasisDelimiterProcessor
protected constructor(private val delimiterChar: Char) : DelimiterProcessor {
    override val openingCharacter: Char
        get() = delimiterChar

    override val closingCharacter: Char
        get() = delimiterChar

    override val minLength: Int
        get() = 1


    override fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int {
        // "multiple of 3" rule for internal delimiter runs
        if ((openingRun.canClose() || closingRun.canOpen()) && closingRun.originalLength() % 3 != 0 && (openingRun.originalLength() + closingRun.originalLength()) % 3 == 0) {
            return 0
        }

        val usedDelimiters: Int
        val emphasis: Node?
        // calculate the actual number of delimiters used from this closer
        if (openingRun.length() >= 2 && closingRun.length() >= 2) {
            usedDelimiters = 2
            emphasis = StrongEmphasis(buildString {
                append(delimiterChar)
                append(delimiterChar)
            })
        } else {
            usedDelimiters = 1
            emphasis = Emphasis(delimiterChar.toString())
        }

        val sourceSpans: SourceSpans = SourceSpans.empty()
        sourceSpans.addAllFrom(openingRun.getOpeners(usedDelimiters))

        val opener = openingRun.opener
        for (node in Nodes.between(opener, closingRun.closer)) {
            emphasis.appendChild(node)
            sourceSpans.addAll(node.getSourceSpans())
        }

        sourceSpans.addAllFrom(closingRun.getClosers(usedDelimiters))

        emphasis.setSourceSpans(sourceSpans.getSourceSpans())
        opener.insertAfter(emphasis)

        return usedDelimiters
    }
}
