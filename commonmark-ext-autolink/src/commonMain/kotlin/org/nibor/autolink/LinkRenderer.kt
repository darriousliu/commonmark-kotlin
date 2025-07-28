package org.nibor.autolink

/**
 * Renderer for a link
 *
 */
@Deprecated("use {@link LinkExtractor#extractSpans(CharSequence)} instead.")
interface LinkRenderer {
    /**
     * Render the supplied link of the input text to the supplied output.
     *
     * @param link the link span of the link to render
     * @param input the input text where the link occurs
     * @param output the output to write the link to
     */
    fun render(link: LinkSpan?, input: CharSequence?, output: StringBuilder?)
}
