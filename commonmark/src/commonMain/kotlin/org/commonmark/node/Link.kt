package org.commonmark.node

/**
 * A link with a destination and an optional title; the link text is in child nodes.
 *
 *
 * Example for an inline link in a CommonMark document:
 * <pre>`
 * [link](/uri "title")
`</pre> *
 *
 *
 * The corresponding Link node would look like this:
 *
 *  * [.getDestination] returns `"/uri"`
 *  * [.getTitle] returns `"title"`
 *  * A [Text] child node with [getLiteral][Text.getLiteral] that returns `"link"`
 *
 *
 *
 * Note that the text in the link can contain inline formatting, so it could also contain an [Image] or
 * [Emphasis], etc.
 *
 * @see [CommonMark Spec](http://spec.commonmark.org/0.31.2/.links)
 */
class Link(
    val destination: String?,
    /**
     * @return the title or null
     */
    val title: String?
) : Node() {

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun toStringAttributes(): String? {
        return "destination=$destination, title=$title"
    }
}
