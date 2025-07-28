package org.commonmark.node

/**
 * A thematic break, e.g. between text:
 * <pre>
 * Some text
 *
 * ___
 *
 * Some other text.
</pre> *
 *
 * @see [CommonMark Spec](https://spec.commonmark.org/0.31.2/.thematic-breaks)
 */
class ThematicBreak : Block() {
    /**
     * @return the source literal that represents this node, if available
     */
    var literal: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
