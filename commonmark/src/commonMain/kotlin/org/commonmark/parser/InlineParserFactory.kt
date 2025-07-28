package org.commonmark.parser

/**
 * Factory for custom inline parser.
 */
interface InlineParserFactory {
    /**
     * Create an [InlineParser] to use for parsing inlines. This is called once per parsed document.
     */
    fun create(inlineParserContext: InlineParserContext): InlineParser
}
