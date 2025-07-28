package org.commonmark.node

/**
 * A link reference definition, e.g.:
 * <pre>`
 * [foo]: /url "title"
`</pre> *
 *
 *
 * They can be referenced anywhere else in the document to produce a link using `[foo]`. The definitions
 * themselves are usually not rendered in the final output.
 *
 * @see [CommonMark Spec](https://spec.commonmark.org/0.31.2/.link-reference-definition)
 */
class LinkReferenceDefinition(
    val label: String,
    val destination: String?,
    val title: String?,
) : Block() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
