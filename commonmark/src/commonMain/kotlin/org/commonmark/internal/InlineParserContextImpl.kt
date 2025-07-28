package org.commonmark.internal

import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.parser.InlineParserContext
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.delimiter.DelimiterProcessor
import kotlin.reflect.KClass

class InlineParserContextImpl(
    private val inlineContentParserFactories: List<InlineContentParserFactory>,
    private val delimiterProcessors: List<DelimiterProcessor>,
    private val linkProcessors: List<LinkProcessor>,
    private val linkMarkers: Set<Char>,
    private val definitions: Definitions
) : InlineParserContext {

    override val customInlineContentParserFactories: List<InlineContentParserFactory>
        get() = inlineContentParserFactories

    override val customDelimiterProcessors: List<DelimiterProcessor>
        get() = delimiterProcessors

    override val customLinkProcessors: List<LinkProcessor>
        get() = linkProcessors

    override val customLinkMarkers: Set<Char>
        get() = linkMarkers

    @Deprecated("use {@link #getDefinition} with {@link LinkReferenceDefinition} instead")
    override fun getLinkReferenceDefinition(label: String): LinkReferenceDefinition? {
        return definitions.getDefinition(LinkReferenceDefinition::class, label)
    }

    override fun <D : Any> getDefinition(type: KClass<D>, label: String): D? {
        return definitions.getDefinition(type, label)
    }
}
