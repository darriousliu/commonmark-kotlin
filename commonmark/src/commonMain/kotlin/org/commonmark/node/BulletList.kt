package org.commonmark.node

/**
 * A bullet list, e.g.:
 * <pre>
 * - One
 * - Two
 * - Three
</pre> *
 *
 *
 * The children are [ListItem] blocks, which contain other blocks (or nested lists).
 *
 * @see [CommonMark Spec: List items](https://spec.commonmark.org/0.31.2/.list-items)
 */
class BulletList : ListBlock() {
    /**
     * @return the bullet list marker that was used, e.g. `-`, `*` or `+`, if available, or null otherwise
     */
    var marker: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    @get:Deprecated("use {@link #marker)} instead")
    @set:Deprecated("use {@link #marker)} instead")
    var bulletMarker: Char
        get() {
            val marker = marker
            return if (!marker.isNullOrEmpty()) marker[0] else '\u0000'
        }
        set(bulletMarker) {
            this.marker = if (bulletMarker != '\u0000') bulletMarker.toString() else null
        }
}
