package org.commonmark.node

/**
 * An ordered list, e.g.:
 * <pre>`
 * 1. One
 * 2. Two
 * 3. Three
`</pre> *
 *
 *
 * The children are [ListItem] blocks, which contain other blocks (or nested lists).
 *
 * @see [CommonMark Spec: List items](https://spec.commonmark.org/0.31.2/.list-items)
 */
class OrderedList : ListBlock() {
    /**
     * @return the delimiter used in the marker, e.g. `.` or `)`, if available, or null otherwise
     */
    var markerDelimiter: String? = null
    var markerStartNumber: Int? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

//    /**
//     * @return the start number used in the marker, e.g. `1`, if available, or null otherwise
//     */
//    fun getMarkerStartNumber(): Int? {
//        return markerStartNumber
//    }
//
//    fun setMarkerStartNumber(markerStartNumber: Int?) {
//        this.markerStartNumber = markerStartNumber
//    }

    @get:Deprecated("use {@link #getMarkerStartNumber()} instead")
    @set:Deprecated("use {@link #setMarkerStartNumber} instead")
    var startNumber: Int
        get() = markerStartNumber ?: 0
        set(startNumber) {
            this.markerStartNumber = if (startNumber != 0) startNumber else null
        }

    @get:Deprecated("use {@link #getMarkerDelimiter()} instead")
    @set:Deprecated("use {@link #setMarkerDelimiter} instead")
    var delimiter: Char
        get() {
            val markerDelimiter = markerDelimiter
            return if (!markerDelimiter.isNullOrEmpty()) markerDelimiter[0] else '\u0000'
        }
        set(delimiter) {
            this.markerDelimiter = if (delimiter != '\u0000') delimiter.toString() else null
        }
}
