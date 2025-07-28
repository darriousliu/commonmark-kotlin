package org.commonmark.ext.autolink.internal

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.SourceSpan
import org.commonmark.node.Text
import org.commonmark.parser.PostProcessor
import org.nibor.autolink.LinkExtractor
import org.nibor.autolink.LinkSpan
import org.nibor.autolink.LinkType
import org.nibor.autolink.Span

class AutolinkPostProcessor : PostProcessor {
    private val linkExtractor: LinkExtractor = LinkExtractor.builder()
        .linkTypes(LinkType.entries.toSet())
        .build()

    override fun process(node: Node): Node {
        val autolinkVisitor = AutolinkVisitor()
        node.accept(autolinkVisitor)
        return node
    }

    private fun linkify(originalTextNode: Text) {
        val literal = originalTextNode.literal

        var lastNode: Node = originalTextNode
        val sourceSpans: List<SourceSpan> = originalTextNode.getSourceSpans()
        val sourceSpan = if (sourceSpans.size == 1) sourceSpans[0] else null

        val spans = linkExtractor.extractSpans(literal).iterator()
        while (spans.hasNext()) {
            val span: Span = spans.next()

            if (lastNode === originalTextNode && !spans.hasNext() && (span !is LinkSpan)) {
                // Didn't find any links, don't bother changing existing node.
                return
            }

            val textNode: Text =
                createTextNode(
                    literal,
                    span,
                    sourceSpan
                )
            if (span is LinkSpan) {
                val destination: String? =
                    getDestination(span, textNode.literal)

                val linkNode = Link(destination, null)
                linkNode.appendChild(textNode)
                linkNode.setSourceSpans(textNode.getSourceSpans())
                lastNode = insertNode(linkNode, lastNode)
            } else {
                lastNode = insertNode(textNode, lastNode)
            }
        }

        // Original node no longer needed
        originalTextNode.unlink()
    }

    private inner class AutolinkVisitor : AbstractVisitor() {
        var inLink: Int = 0

        override fun visit(link: Link) {
            inLink++
            super.visit(link)
            inLink--
        }

        override fun visit(text: Text) {
            if (inLink == 0) {
                linkify(text)
            }
        }
    }

    companion object {
        private fun createTextNode(
            literal: String,
            span: Span,
            sourceSpan: SourceSpan?
        ): Text {
            val beginIndex: Int = span.beginIndex
            val endIndex: Int = span.endIndex
            val text = literal.substring(beginIndex, endIndex)
            val textNode = Text(text)
            if (sourceSpan != null) {
                textNode.addSourceSpan(sourceSpan.subSpan(beginIndex, endIndex))
            }
            return textNode
        }

        private fun getDestination(linkSpan: LinkSpan, linkText: String?): String? {
            return if (linkSpan.type == LinkType.EMAIL) {
                "mailto:$linkText"
            } else {
                linkText
            }
        }

        private fun insertNode(
            node: Node,
            insertAfterNode: Node
        ): Node {
            insertAfterNode.insertAfter(node)
            return node
        }
    }
}
