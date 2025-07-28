package org.commonmark.parser

import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.delimiter.DelimiterProcessor
import kotlin.reflect.KClass

/**
 * Context for inline parsing.
 */
interface InlineParserContext {
    /**
     * @return custom inline content parsers that have been configured with
     * [Parser.Builder.customInlineContentParserFactory]
     */
    val customInlineContentParserFactories: List<InlineContentParserFactory>

    /**
     * @return custom delimiter processors that have been configured with
     * [Parser.Builder.customDelimiterProcessor]
     */
    val customDelimiterProcessors: List<DelimiterProcessor>

    /**
     * @return custom link processors that have been configured with [Parser.Builder.linkProcessor].
     */
    val customLinkProcessors: List<LinkProcessor>

    /**
     * @return custom link markers that have been configured with [Parser.Builder.linkMarker].
     */
    val customLinkMarkers: Set<Char>

    /**
     * Look up a [LinkReferenceDefinition] for a given label.
     *
     *
     * Note that the passed in label does not need to be normalized; implementations are responsible for doing the
     * normalization before lookup.
     *
     * @param label the link label to look up
     * @return the definition if one exists, `null` otherwise
     */
    @Deprecated("use {@link #getDefinition} with {@link LinkReferenceDefinition} instead")
    fun getLinkReferenceDefinition(label: String): LinkReferenceDefinition?

    /**
     * Look up a definition of a type for a given label.
     *
     *
     * Note that the passed in label does not need to be normalized; implementations are responsible for doing the
     * normalization before lookup.
     *
     * @return the definition if one exists, null otherwise
     */
    fun <D : Any> getDefinition(type: KClass<D>, label: String): D?
}
