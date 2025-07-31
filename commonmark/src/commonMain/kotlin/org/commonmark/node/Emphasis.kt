package org.commonmark.node

/**
 * Emphasis, e.g.:
 * <pre>
 * Some *emphasis* or _emphasis_
</pre> *
 *
 * @see [CommonMark Spec: Emphasis and strong emphasis](https://spec.commonmark.org/0.31.2/.emphasis-and-strong-emphasis)
 */
class Emphasis : Node, Delimited {
    private var delimiter: String

    override val openingDelimiter: String
        get() = delimiter

    override val closingDelimiter: String
        get() = delimiter

    constructor(delimiter: String) {
        this.delimiter = delimiter
    }

    fun setDelimiter(delimiter: String) {
        this.delimiter = delimiter
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
