package org.commonmark.internal.inline

import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.parser.InlineParserContext
import org.commonmark.parser.beta.LinkInfo
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.beta.LinkResult
import org.commonmark.parser.beta.Scanner

class CoreLinkProcessor : LinkProcessor {
    override fun process(
        linkInfo: LinkInfo,
        scanner: Scanner,
        context: InlineParserContext
    ): LinkResult? {
        if (linkInfo.destination() != null) {
            // Inline link
            return process(
                linkInfo,
                scanner,
                linkInfo.destination(),
                linkInfo.title()
            )
        }

        val label = linkInfo.label()
        val ref = if (!label.isNullOrEmpty()) label else linkInfo.text()
        val def = context.getDefinition(LinkReferenceDefinition::class, ref)
        if (def != null) {
            // Reference link
            return process(
                linkInfo,
                scanner,
                def.destination,
                def.title
            )
        }
        return LinkResult.none()
    }

    companion object {
        private fun process(
            linkInfo: LinkInfo,
            scanner: Scanner,
            destination: String?,
            title: String?
        ): LinkResult {
            val marker = linkInfo.marker()
            if (marker != null && marker.literal == "!") {
                return LinkResult.wrapTextIn(Image(destination, title), scanner.position())
                    .includeMarker()
            }
            return LinkResult.wrapTextIn(Link(destination, title), scanner.position())
        }
    }
}
