package org.commonmark.ext.gfm.tables

import org.commonmark.node.CustomNode

/**
 * Table cell of a [TableRow] containing inline nodes.
 */
class TableCell : CustomNode() {
    /**
     * @return whether the cell is a header or not
     */
    var isHeader: Boolean = false

    /**
     * @return the cell alignment or `null` if no specific alignment
     */
    var alignment: Alignment? = null

    /**
     * @return the cell width (the number of dash and colon characters in the delimiter row of the table for this column)
     */
    var width: Int = 0

    /**
     * How the cell is aligned horizontally.
     */
    enum class Alignment {
        LEFT, CENTER, RIGHT
    }
}
