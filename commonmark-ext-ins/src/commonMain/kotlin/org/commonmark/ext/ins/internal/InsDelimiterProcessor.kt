package org.commonmark.ext.ins.internal

import org.commonmark.ext.ins.Ins
import org.commonmark.node.Node
import org.commonmark.node.Nodes
import org.commonmark.node.SourceSpans
import org.commonmark.node.Text
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

class InsDelimiterProcessor : DelimiterProcessor {
    override val openingCharacter: Char
        get() = '+'

    override val closingCharacter: Char
        get() = '+'

    override val minLength: Int
        get() = 2

    override fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int {
        if (openingRun.length() >= 2 && closingRun.length() >= 2) {
            // Use exactly two delimiters even if we have more, and don't care about internal openers/closers.

            val opener: Text = openingRun.opener

            // Wrap nodes between delimiters in ins.
            val ins: Node = Ins()

            val sourceSpans = SourceSpans()
            sourceSpans.addAllFrom(openingRun.getOpeners(2))

            for (node in Nodes.between(opener, closingRun.closer)) {
                ins.appendChild(node)
                sourceSpans.addAll(node.getSourceSpans())
            }

            sourceSpans.addAllFrom(closingRun.getClosers(2))
            ins.setSourceSpans(sourceSpans.getSourceSpans())

            opener.insertAfter(ins)

            return 2
        } else {
            return 0
        }
    }
}
