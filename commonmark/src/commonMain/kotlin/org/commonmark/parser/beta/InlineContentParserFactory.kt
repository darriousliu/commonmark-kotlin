package org.commonmark.parser.beta

/**
 * A factory for extending inline content parsing.
 *
 *
 * See [org.commonmark.parser.Parser.Builder.customInlineContentParserFactory] for how to register it.
 */
interface InlineContentParserFactory {
    /**
     * An inline content parser needs to have a special "trigger" character which activates it. When this character is
     * encountered during inline parsing, [InlineContentParser.tryParse] is called with the current parser state.
     * It can also register for more than one trigger character.
     */
    val triggerCharacters: Set<Char>

    /**
     * Create an [InlineContentParser] that will do the parsing. Create is called once per text snippet of inline
     * content inside block structures, and then called each time a trigger character is encountered.
     */
    fun create(): InlineContentParser
}
