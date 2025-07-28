package org.commonmark.node

/**
 * An image, e.g.:
 * <pre>
 * ![foo](/url "title")
</pre> *
 *
 * @see [CommonMark Spec](https://spec.commonmark.org/0.31.2/.images)
 */
class Image(
    val destination: String?,
    val title: String?
) : Node() {

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun toStringAttributes(): String? {
        return "destination=$destination, title=$title"
    }
}
