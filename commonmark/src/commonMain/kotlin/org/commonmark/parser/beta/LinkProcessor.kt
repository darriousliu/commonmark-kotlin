package org.commonmark.parser.beta

import org.commonmark.parser.InlineParserContext

/**
 * An interface to decide how links/images are handled.
 *
 *
 * Implementations need to be registered with a parser via [org.commonmark.parser.Parser.Builder.linkProcessor].
 * Then, when inline parsing is run, each parsed link/image is passed to the processor. This includes links like these:
 *
 *
 * <pre>`
 * [text](destination)
 * [text]
 * [text][]
 * [text][label]
`</pre> *
 * And images:
 * <pre>`
 * ![text](destination)
 * ![text]
 * ![text][]
 * ![text][label]
`</pre> *
 * See [LinkInfo] for accessing various parts of the parsed link/image.
 *
 *
 * The processor can then inspect the link/image and decide what to do with it by returning the appropriate
 * [LinkResult]. If it returns [LinkResult.none], the next registered processor is tried. If none of them
 * apply, the link is handled as it normally would.
 */
interface LinkProcessor {
    /**
     * @param linkInfo information about the parsed link/image
     * @param scanner  the scanner at the current position after the parsed link/image
     * @param context  context for inline parsing
     * @return what to do with the link/image, e.g. do nothing (try the next processor), wrap the text in a node, or
     * replace the link/image with a node
     */
    fun process(linkInfo: LinkInfo, scanner: Scanner, context: InlineParserContext): LinkResult?
}
