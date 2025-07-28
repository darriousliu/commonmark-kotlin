package org.commonmark.node

/**
 * A paragraph block, contains inline nodes such as [Text]
 *
 * @see [CommonMark Spec](https://spec.commonmark.org/0.31.2/.paragraphs)
 */
class Paragraph : Block() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
