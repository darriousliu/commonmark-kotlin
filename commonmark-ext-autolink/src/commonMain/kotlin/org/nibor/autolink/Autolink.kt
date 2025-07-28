package org.nibor.autolink

/**
 * Utility class for processing text with links.
 */
object Autolink {
    /**
     * Render the supplied links from the supplied input text using a renderer. The parts of the text outside of links
     * are added to the result without processing.
     *
     * @param input the input text, must not be null
     * @param links the links to render, see [LinkExtractor] to extract them
     * @param linkRenderer the link rendering implementation
     * @return the rendered string
     */
    @Deprecated("use {@link LinkExtractor#extractSpans(CharSequence)} instead")
    fun renderLinks(
        input: CharSequence?,
        links: Iterable<LinkSpan>?,
        linkRenderer: LinkRenderer?
    ): String {
        if (input == null) {
            throw NullPointerException("input must not be null")
        }
        if (links == null) {
            throw NullPointerException("links must not be null")
        }
        if (linkRenderer == null) {
            throw NullPointerException("linkRenderer must not be null")
        }
        val sb = StringBuilder(input.length + 16)
        var lastIndex = 0
        for (link in links) {
            sb.append(input, lastIndex, link.beginIndex)
            linkRenderer.render(link, input, sb)
            lastIndex = link.endIndex
        }
        if (lastIndex < input.length) {
            sb.append(input, lastIndex, input.length)
        }
        return sb.toString()
    }
}
