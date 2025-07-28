package org.commonmark.parser.beta

import org.commonmark.node.Text

/**
 * A parsed link/image. There are different types of links.
 *
 *
 * Inline links:
 * <pre>
 * [text](destination)
 * [text](destination "title")
</pre> *
 *
 *
 * Reference links, which have different subtypes. Full::
 * <pre>
 * [text][label]
</pre> *
 * Collapsed (label is ""):
 * <pre>
 * [text][]
</pre> *
 * Shortcut (label is null):
 * <pre>
 * [text]
</pre> *
 * Images use the same syntax as links but with a `!` [.marker] front, e.g. `![text](destination)`.
 */
interface LinkInfo {
    /**
     * The marker if present, or null. A marker is e.g. `!` for an image, or a custom marker as specified in
     * [org.commonmark.parser.Parser.Builder.linkMarker].
     */
    fun marker(): Text?

    /**
     * The text node of the opening bracket `[`.
     */
    fun openingBracket(): Text?

    /**
     * The text between the first brackets, e.g. `foo` in `[foo][bar]`.
     */
    fun text(): String

    /**
     * The label, or null for inline links or for shortcut links (in which case [.text] should be used as the label).
     */
    fun label(): String?

    /**
     * The destination if available, e.g. in `[foo](destination)`, or null
     */
    fun destination(): String?

    /**
     * The title if available, e.g. in `[foo](destination "title")`, or null
     */
    fun title(): String?

    /**
     * The position after the closing text bracket, e.g.:
     * <pre>
     * [foo][bar]
     * ^
    </pre> *
     */
    fun afterTextBracket(): Position
}
