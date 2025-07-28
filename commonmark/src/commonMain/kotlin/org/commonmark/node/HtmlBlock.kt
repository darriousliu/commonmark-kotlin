package org.commonmark.node

/**
 * HTML block
 *
 * @see [CommonMark Spec](http://spec.commonmark.org/0.31.2/.html-blocks)
 */
class HtmlBlock : Block() {
    var literal: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
