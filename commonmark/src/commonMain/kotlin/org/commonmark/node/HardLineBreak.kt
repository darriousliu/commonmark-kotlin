package org.commonmark.node

/**
 * A hard line break, e.g.:
 * <pre>
 * line\
 * break
</pre> *
 *
 *
 *
 * @see [CommonMark Spec](https://spec.commonmark.org/0.31.2/.hard-line-breaks)
 */
class HardLineBreak : Node() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
