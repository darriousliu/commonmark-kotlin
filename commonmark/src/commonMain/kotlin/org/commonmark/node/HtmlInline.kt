package org.commonmark.node

/**
 * Inline HTML element.
 *
 * @see [CommonMark Spec](http://spec.commonmark.org/0.31.2/.raw-html)
 */
class HtmlInline(
    val literal: String
) : Node() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
