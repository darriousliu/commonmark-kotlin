package org.commonmark.node

/**
 * A text node, e.g. in:
 * <pre>
 * foo *bar*
</pre> *
 *
 *
 * The `foo ` is a text node, and the `bar` inside the emphasis is also a text node.
 *
 * @see [CommonMark Spec](https://spec.commonmark.org/0.31.2/.textual-content)
 */
class Text(var literal: String) : Node() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun toStringAttributes(): String? {
        return "literal=$literal"
    }
}
