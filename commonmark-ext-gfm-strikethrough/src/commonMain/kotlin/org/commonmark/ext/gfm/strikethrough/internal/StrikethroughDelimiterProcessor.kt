package org.commonmark.ext.gfm.strikethrough.internal

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node.Node
import org.commonmark.node.Nodes
import org.commonmark.node.SourceSpans
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun
import kotlin.jvm.JvmOverloads

class StrikethroughDelimiterProcessor @JvmOverloads constructor(private val requireTwoTildes: Boolean = false) :
    DelimiterProcessor {
    override val openingCharacter: Char
        get() = '~'

    override val closingCharacter: Char
        get() = '~'

    override val minLength: Int
        get() = if (requireTwoTildes) 2 else 1

    override fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int {
        if (openingRun.length() == closingRun.length() && openingRun.length() <= 2) {
            // GitHub only accepts either one or two delimiters, but not a mix or more than that.

            val opener = openingRun.opener

            // Wrap nodes between delimiters in strikethrough.
            val delimiter =
                if (openingRun.length() == 1) opener.literal else opener.literal + opener.literal
            val strikethrough: Node = Strikethrough(delimiter)

            val sourceSpans = SourceSpans()
            sourceSpans.addAllFrom(openingRun.getOpeners(openingRun.length()))

            for (node in Nodes.between(opener, closingRun.closer)) {
                strikethrough.appendChild(node)
                sourceSpans.addAll(node.getSourceSpans())
            }

            sourceSpans.addAllFrom(closingRun.getClosers(closingRun.length()))
            strikethrough.setSourceSpans(sourceSpans.getSourceSpans())

            opener.insertAfter(strikethrough)

            return openingRun.length()
        } else {
            return 0
        }
    }
}
