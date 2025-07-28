package org.commonmark.node

/**
 * Inline code span, e.g.:
 * <pre>
 * Some `inline code`
</pre> *
 *
 * @see [CommonMark Spec](https://spec.commonmark.org/0.31.2/.code-spans)
 */
class Code(
    /**
     * @return the literal text in the code span (note that it's not necessarily the raw text between tildes,
     * e.g., when spaces are stripped)
     */
    val literal: String
) : Node() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
