package org.commonmark.node

/**
 * A heading, e.g.:
 * <pre>
 * First heading
 * =============
 *
 * ## Another heading
</pre> *
 *
 * @see [CommonMark Spec: ATX headings](https://spec.commonmark.org/0.31.2/.atx-headings)
 *
 * @see [CommonMark Spec: Setext headings](https://spec.commonmark.org/0.31.2/.setext-headings)
 */
class Heading(
    val level: Int
) : Block() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
