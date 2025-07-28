package org.commonmark.node

/**
 * An indented code block, e.g.:
 * <pre>`
 * Code follows:
 *
 * foo
 * bar
`</pre> *
 *
 *
 *
 * @see [CommonMark Spec](https://spec.commonmark.org/0.31.2/.indented-code-blocks)
 */
class IndentedCodeBlock : Block() {
    var literal: String = ""

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
