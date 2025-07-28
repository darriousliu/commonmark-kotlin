package org.commonmark.parser.beta

import org.commonmark.internal.inline.LinkResultImpl
import org.commonmark.node.Node

/**
 * What to do with a link/image processed by [LinkProcessor].
 */
interface LinkResult {
    /**
     * If a [LinkInfo.marker] is present, include it in processing (i.e. treat it the same way as the brackets).
     */
    fun includeMarker(): LinkResult

    companion object {
        /**
         * Link not handled by processor.
         */
        fun none(): LinkResult? {
            return null
        }

        /**
         * Wrap the link text in a node. This is the normal behavior for links, e.g. for this:
         * <pre>`
         * [my *text*](destination)
        `</pre> *
         * The text is `my *text*`, a text node and emphasis. The text is wrapped in a
         * [org.commonmark.node.Link] node, which means the text is added as child nodes to it.
         *
         * @param node     the node to which the link text nodes will be added as child nodes
         * @param position the position to continue parsing from
         */
        fun wrapTextIn(node: Node, position: Position): LinkResult {
            return LinkResultImpl(LinkResultImpl.Type.WRAP, node, position)
        }

        /**
         * Replace the link with a node. E.g. for this:
         * <pre>`
         * [^foo]
        `</pre> *
         * The processor could decide to create a `FootnoteReference` node instead which replaces the link.
         *
         * @param node     the node to replace the link with
         * @param position the position to continue parsing from
         */
        fun replaceWith(node: Node, position: Position): LinkResult {
            return LinkResultImpl(LinkResultImpl.Type.REPLACE, node, position)
        }
    }
}
