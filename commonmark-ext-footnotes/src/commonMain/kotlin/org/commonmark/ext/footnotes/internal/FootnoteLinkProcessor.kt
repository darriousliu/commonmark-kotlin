package org.commonmark.ext.footnotes.internal

import org.commonmark.ext.footnotes.FootnoteDefinition
import org.commonmark.ext.footnotes.FootnoteReference
import org.commonmark.ext.footnotes.InlineFootnote
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.parser.InlineParserContext
import org.commonmark.parser.beta.LinkInfo
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.beta.LinkResult
import org.commonmark.parser.beta.Scanner

/**
 * For turning e.g. `[^foo]` into a [FootnoteReference],
 * and `^[foo]` into an [InlineFootnote].
 */
class FootnoteLinkProcessor : LinkProcessor {
    override fun process(
        linkInfo: LinkInfo,
        scanner: Scanner,
        context: InlineParserContext
    ): LinkResult? {
        val marker = linkInfo.marker()
        if (marker != null && marker.literal == "^") {
            // An inline footnote like ^[footnote text]. Note that we only get the marker here if the option is enabled
            // on the extension.
            return LinkResult.wrapTextIn(InlineFootnote(), linkInfo.afterTextBracket())
                .includeMarker()
        }

        if (linkInfo.destination() != null) {
            // If it's an inline link, it can't be a footnote reference
            return LinkResult.none()
        }

        val text = linkInfo.text()
        if (!text.startsWith("^")) {
            // Footnote reference needs to start with [^
            return LinkResult.none()
        }

        val linkInfoLabel = linkInfo.label()
        if (linkInfoLabel != null &&
            context.getDefinition(LinkReferenceDefinition::class, linkInfoLabel) != null
        ) {
            // If there's a label after the text and the label has a definition -> it's a link, and it should take
            // preference, e.g. in `[^foo][bar]` if `[bar]` has a definition, `[^foo]` won't be a footnote reference.
            return LinkResult.none()
        }

        val label = text.substring(1)
        // Check if we have a definition, otherwise ignore (same behavior as for link reference definitions).
        // Note that the definition parser already checked the syntax of the label, we don't need to check again.
        val def = context.getDefinition(FootnoteDefinition::class, label)
        if (def == null) {
            return LinkResult.none()
        }

        // For footnotes, we only ever consume the text part of the link, not the label part (if any)
        val position = linkInfo.afterTextBracket()
        // If the marker is `![`, we don't want to include the `!`, so start from bracket
        return LinkResult.replaceWith(FootnoteReference(label), position)
    }
}
