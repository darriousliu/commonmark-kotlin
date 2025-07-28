package org.commonmark.parser

import okio.BufferedSource
import okio.IOException
import org.commonmark.Extension
import org.commonmark.internal.Definitions
import org.commonmark.internal.DocumentParser
import org.commonmark.internal.InlineParserContextImpl
import org.commonmark.internal.InlineParserImpl
import org.commonmark.node.Block
import org.commonmark.node.Node
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.block.BlockParserFactory
import org.commonmark.parser.delimiter.DelimiterProcessor
import kotlin.reflect.KClass

/**
 * Parses input text to a tree of nodes.
 *
 *
 * Start with the [.builder] method, configure the parser and build it. Example:
 * <pre>`
 * Parser parser = Parser.builder().build();
 * Node document = parser.parse("input text");
`</pre> *
 */
class Parser private constructor(builder: Builder) {
    private val blockParserFactories: List<BlockParserFactory> =
        DocumentParser.calculateBlockParserFactories(
            builder.blockParserFactories,
            builder.enabledBlockTypes
        )
    private val inlineContentParserFactories: List<InlineContentParserFactory> =
        builder.inlineContentParserFactories
    private val delimiterProcessors: List<DelimiterProcessor> = builder.delimiterProcessors
    private val linkProcessors: List<LinkProcessor> = builder.linkProcessors
    private val linkMarkers: Set<Char> = builder.linkMarkers
    private val inlineParserFactory: InlineParserFactory = builder.getOrCreateInlineParserFactory()
    private val postProcessors: List<PostProcessor> = builder.postProcessors
    private val includeSourceSpans: IncludeSourceSpans = builder.includeSourceSpans

    init {
        // Try to construct an inline parser. Invalid configuration might result in an exception, which we want to
        // detect as soon as possible.
        val context = InlineParserContextImpl(
            inlineContentParserFactories,
            delimiterProcessors,
            linkProcessors,
            linkMarkers,
            Definitions()
        )
        this.inlineParserFactory.create(context)
    }

    /**
     * Parse the specified input text into a tree of nodes.
     *
     *
     * This method is thread-safe (a new parser state is used for each invocation).
     *
     * @param input the text to parse - must not be null
     * @return the root node
     */
    fun parse(input: String): Node? {
        val documentParser = createDocumentParser()
        val document = documentParser.parse(input)
        return postProcess(document)
    }

    /**
     * Parse the specified reader into a tree of nodes. The caller is responsible for closing the reader.
     * <pre>`
     * Parser parser = Parser.builder().build();
     * try (InputStreamReader reader = new InputStreamReader(new FileInputStream("file.md"), StandardCharsets.UTF_8)) {
     * Node document = parser.parseReader(reader);
     * // ...
     * }
    `</pre> *
     * Note that if you have a file with a byte order mark (BOM), you need to skip it before handing the reader to this
     * library. There's existing classes that do that, e.g. see `BOMInputStream` in Commons IO.
     *
     *
     * This method is thread-safe (a new parser state is used for each invocation).
     *
     * @param input the reader to parse - must not be null
     * @return the root node
     * @throws IOException when reading throws an exception
     */
    @Throws(IOException::class)
    fun parseReader(input: BufferedSource): Node {
        val documentParser: DocumentParser = createDocumentParser()
        val document = documentParser.parse(input)
        return postProcess(document)
    }

    private fun createDocumentParser(): DocumentParser {
        return DocumentParser(
            blockParserFactories, inlineParserFactory, inlineContentParserFactories,
            delimiterProcessors, linkProcessors, linkMarkers, includeSourceSpans
        )
    }

    private fun postProcess(document: Node): Node {
        var document = document
        for (postProcessor in postProcessors) {
            document = postProcessor.process(document)
        }
        return document
    }

    /**
     * Builder for configuring a [Parser].
     */
    class Builder {
        internal val blockParserFactories: MutableList<BlockParserFactory> = mutableListOf()
        internal val inlineContentParserFactories: MutableList<InlineContentParserFactory> =
            mutableListOf()
        internal val delimiterProcessors: MutableList<DelimiterProcessor> = mutableListOf()
        internal val linkProcessors: MutableList<LinkProcessor> = mutableListOf()
        internal val postProcessors: MutableList<PostProcessor> = mutableListOf()
        internal val linkMarkers: MutableSet<Char> = hashSetOf()
        internal var enabledBlockTypes: Set<KClass<out Block>> =
            DocumentParser.defaultBlockParserTypes
        internal var inlineParserFactory: InlineParserFactory? = null
        internal var includeSourceSpans: IncludeSourceSpans = IncludeSourceSpans.NONE

        /**
         * @return the configured [Parser]
         */
        fun build(): Parser {
            return Parser(this)
        }

        /**
         * @param extensions extensions to use on this parser
         * @return `this`
         */
        fun extensions(extensions: Iterable<Extension>): Builder {
            for (extension in extensions) {
                if (extension is ParserExtension) {
                    val parserExtension = extension
                    parserExtension.extend(this)
                }
            }
            return this
        }

        /**
         * Describe the list of markdown features the parser will recognize and parse.
         *
         *
         * By default, CommonMark will recognize and parse the following set of "block" elements:
         *
         *  * [org.commonmark.node.Heading] (`#`)
         *  * [org.commonmark.node.HtmlBlock] (`<html></html>`)
         *  * [org.commonmark.node.ThematicBreak] (Horizontal Rule) (`---`)
         *  * [org.commonmark.node.FencedCodeBlock] (`` ``` ``)
         *  * [org.commonmark.node.IndentedCodeBlock]
         *  * [org.commonmark.node.BlockQuote] (`>`)
         *  * [org.commonmark.node.ListBlock] (Ordered / Unordered List) (`1. / *`)
         *
         *
         *
         * To parse only a subset of the features listed above, pass a list of each feature's associated [Block] class.
         *
         *
         * E.g., to only parse headings and lists:
         * <pre>
         * `Parser.builder().enabledBlockTypes(setOf (Heading.class, ListBlock.class));
        ` *
        </pre> *
         *
         * @param enabledBlockTypes A list of block nodes the parser will parse.
         * If this list is empty, the parser will not recognize any CommonMark core features.
         * @return `this`
         */
        fun enabledBlockTypes(enabledBlockTypes: Set<KClass<out Block>>): Builder {
            DocumentParser.checkEnabledBlockTypes(enabledBlockTypes)
            this.enabledBlockTypes = enabledBlockTypes
            return this
        }

        /**
         * Whether to calculate source positions for parsed [Nodes][Node], see [Node.getSourceSpans].
         *
         *
         * By default, source spans are disabled.
         *
         * @param includeSourceSpans which kind of source spans should be included
         * @return `this`
         * @since 0.16.0
         */
        fun includeSourceSpans(includeSourceSpans: IncludeSourceSpans): Builder {
            this.includeSourceSpans = includeSourceSpans
            return this
        }

        /**
         * Add a custom block parser factory.
         *
         *
         * Note that custom factories are applied *before* the built-in factories. This is so that
         * extensions can change how some syntax is parsed that would otherwise be handled by built-in factories.
         * "With great power comes great responsibility."
         *
         * @param blockParserFactory a block parser factory implementation
         * @return `this`
         */
        fun customBlockParserFactory(blockParserFactory: BlockParserFactory): Builder {
            blockParserFactories.add(blockParserFactory)
            return this
        }

        /**
         * Add a factory for a custom inline content parser, for extending inline parsing or overriding built-in parsing.
         *
         *
         * Note that parsers are triggered based on a special character as specified by
         * [InlineContentParserFactory.triggerCharacters]. It is possible to register multiple parsers for the same
         * character, or even for some built-in special character such as `` ` ``. The custom parsers are tried first
         * in the order in which they are registered and then the built-in ones.
         */
        fun customInlineContentParserFactory(inlineContentParserFactory: InlineContentParserFactory): Builder {
            inlineContentParserFactories.add(inlineContentParserFactory)
            return this
        }

        /**
         * Add a custom delimiter processor for inline parsing.
         *
         *
         * Note that multiple delimiter processors with the same characters can be added, as long as they have a
         * different minimum length. In that case, the processor with the shortest matching length is used. Adding more
         * than one delimiter processor with the same character and minimum length is invalid.
         *
         *
         * If you want more control over how parsing is done, you might want to use
         * [.customInlineContentParserFactory] instead.
         *
         * @param delimiterProcessor a delimiter processor implementation
         * @return `this`
         */
        fun customDelimiterProcessor(delimiterProcessor: DelimiterProcessor): Builder {
            delimiterProcessors.add(delimiterProcessor)
            return this
        }

        /**
         * Add a custom link/image processor for inline parsing.
         *
         *
         * Multiple link processors can be added, and will be tried in order in which they were added. If no link
         * processor applies, the normal behavior applies. That means these can override built-in link parsing.
         *
         * @param linkProcessor a link processor implementation
         * @return `this`
         */
        fun linkProcessor(linkProcessor: LinkProcessor): Builder {
            linkProcessors.add(linkProcessor)
            return this
        }

        /**
         * Add a custom link marker for link processing. A link marker is a character like `!` which, if it
         * appears before the `[` of a link, changes the meaning of the link.
         *
         *
         * If a link marker followed by a valid link is parsed, the [org.commonmark.parser.beta.LinkInfo]
         * that is passed to [LinkProcessor] will have its [org.commonmark.parser.beta.LinkInfo.marker] set. A link processor should
         * check the [org.commonmark.node.Text.literal] and then do any processing, and will probably want to use [org.commonmark.parser.beta.LinkResult.includeMarker].
         *
         * @param linkMarker a link marker character
         * @return `this`
         */
        fun linkMarker(linkMarker: Char): Builder {
            linkMarkers.add(linkMarker)
            return this
        }

        fun postProcessor(postProcessor: PostProcessor): Builder {
            postProcessors.add(postProcessor)
            return this
        }

        /**
         * Overrides the parser used for inline markdown processing.
         *
         *
         * Provide an implementation of InlineParserFactory which provides a custom inline parser
         * to modify how the following are parsed:
         * bold (**)
         * italic (*)
         * strikethrough (~~)
         * backtick quote (`)
         * link ([title](http://))
         * image (![alt](http://))
         *
         *
         * Note that if this method is not called or the inline parser factory is set to null, then the default
         * implementation will be used.
         *
         * @param inlineParserFactory an inline parser factory implementation
         * @return `this`
         */
        fun inlineParserFactory(inlineParserFactory: InlineParserFactory?): Builder {
            this.inlineParserFactory = inlineParserFactory
            return this
        }

        internal fun getOrCreateInlineParserFactory(): InlineParserFactory {
            return inlineParserFactory ?: object : InlineParserFactory {
                override fun create(inlineParserContext: InlineParserContext): InlineParser {
                    return InlineParserImpl(inlineParserContext)
                }
            }
        }
    }

    /**
     * Extension for [Parser].
     */
    interface ParserExtension : Extension {
        fun extend(parserBuilder: Builder)
    }

    companion object {
        /**
         * Create a new builder for configuring a [Parser].
         *
         * @return a builder
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}
