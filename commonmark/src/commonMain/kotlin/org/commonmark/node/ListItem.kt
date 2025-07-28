package org.commonmark.node

/**
 * A child of a [ListBlock], containing other blocks (e.g. [Paragraph], other lists, etc.).
 *
 * @see [CommonMark Spec: List items](https://spec.commonmark.org/0.31.2/.list-items)
 */
class ListItem : Block() {
    private var markerIndent: Int? = null
    private var contentIndent: Int? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    /**
     * Returns the indent of the marker such as "-" or "1." in columns (spaces or tab stop of 4) if available, or null
     * otherwise.
     *
     *
     * Some examples and their marker indent:
     * <pre>- Foo</pre>
     * Marker indent: 0
     * <pre> - Foo</pre>
     * Marker indent: 1
     * <pre> 1. Foo</pre>
     * Marker indent: 2
     */
    fun getMarkerIndent(): Int? {
        return markerIndent
    }

    fun setMarkerIndent(markerIndent: Int?) {
        this.markerIndent = markerIndent
    }

    /**
     * Returns the indent of the content in columns (spaces or tab stop of 4) if available, or null otherwise.
     * The content indent is counted from the beginning of the line and includes the marker on the first line.
     *
     *
     * Some examples and their content indent:
     * <pre>- Foo</pre>
     * Content indent: 2
     * <pre> - Foo</pre>
     * Content indent: 3
     * <pre> 1. Foo</pre>
     * Content indent: 5
     *
     *
     * Note that subsequent lines in the same list item need to be indented by at least the content indent to be counted
     * as part of the list item.
     */
    fun getContentIndent(): Int? {
        return contentIndent
    }

    fun setContentIndent(contentIndent: Int?) {
        this.contentIndent = contentIndent
    }
}
