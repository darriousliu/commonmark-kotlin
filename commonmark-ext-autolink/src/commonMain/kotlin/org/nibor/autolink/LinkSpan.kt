package org.nibor.autolink

/**
 * Information for an extracted link.
 */
interface LinkSpan : Span {
    /**
     * @return the type of link
     */
    val type: LinkType
}
