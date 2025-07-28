package org.commonmark.node

/**
 * Strong emphasis, e.g.:
 * <pre>`
 * Some **strong emphasis** or __strong emphasis__
`</pre> *
 *
 * @see [CommonMark Spec: Emphasis and strong emphasis](https://spec.commonmark.org/0.31.2/.emphasis-and-strong-emphasis)
 */
class StrongEmphasis : Node, Delimited {
    private var delimiter: String? = null

    override val openingDelimiter: String?
        get() = delimiter
    override val closingDelimiter: String?
        get() = delimiter

    constructor()

    constructor(delimiter: String?) {
        this.delimiter = delimiter
    }

    fun setDelimiter(delimiter: String?) {
        this.delimiter = delimiter
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
