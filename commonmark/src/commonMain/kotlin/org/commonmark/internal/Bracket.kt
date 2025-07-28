package org.commonmark.internal

import org.commonmark.node.Text
import org.commonmark.parser.beta.Position

/**
 * Opening bracket for links (`[`), images (`![`), or links with other markers.
 */
class Bracket private constructor(
    /**
     * The node of a marker such as `!` if present, null otherwise.
     */
    val markerNode: Text?,
    /**
     * The position of the marker if present, null otherwise.
     */
    val markerPosition: Position?,
    /**
     * The node of `[`.
     */
    val bracketNode: Text?,
    /**
     * The position of `[`.
     */
    val bracketPosition: Position,
    /**
     * The position of the content (after the opening bracket)
     */
    val contentPosition: Position,
    /**
     * Previous bracket.
     */
    val previous: Bracket?,
    /**
     * Previous delimiter (emphasis, etc) before this bracket.
     */
    val previousDelimiter: Delimiter?
) {
    /**
     * Whether this bracket is allowed to form a link/image (also known as "active").
     */
    var allowed: Boolean = true

    /**
     * Whether there is an unescaped bracket (opening or closing) after this opening bracket in the text parsed so far.
     */
    var bracketAfter: Boolean = false

    companion object {
        fun link(
            bracketNode: Text,
            bracketPosition: Position,
            contentPosition: Position,
            previous: Bracket?,
            previousDelimiter: Delimiter?
        ): Bracket {
            return Bracket(
                null,
                null,
                bracketNode,
                bracketPosition,
                contentPosition,
                previous,
                previousDelimiter
            )
        }

        fun withMarker(
            markerNode: Text?,
            markerPosition: Position?,
            bracketNode: Text,
            bracketPosition: Position,
            contentPosition: Position,
            previous: Bracket?,
            previousDelimiter: Delimiter?
        ): Bracket {
            return Bracket(
                markerNode,
                markerPosition,
                bracketNode,
                bracketPosition,
                contentPosition,
                previous,
                previousDelimiter
            )
        }
    }
}
