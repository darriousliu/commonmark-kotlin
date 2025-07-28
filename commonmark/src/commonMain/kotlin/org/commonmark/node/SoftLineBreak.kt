package org.commonmark.node

/**
 * A soft line break (as opposed to a [HardLineBreak]), e.g. between:
 * <pre>
 * foo
 * bar
</pre> *
 *
 * @see [CommonMark Spec](https://spec.commonmark.org/0.31.2/.soft-line-breaks)
 */
class SoftLineBreak : Node() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
