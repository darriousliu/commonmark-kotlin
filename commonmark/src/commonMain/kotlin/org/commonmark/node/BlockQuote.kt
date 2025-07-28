package org.commonmark.node

/**
 * A block quote, e.g.:
 * <pre>
 * &gt; Some quoted text
</pre> *
 *
 *
 * Note that child nodes are themselves blocks, e.g. [Paragraph], [ListBlock] etc.
 *
 * @see [CommonMark Spec](https://spec.commonmark.org/0.31.2/.block-quotes)
 */
class BlockQuote : Block() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
