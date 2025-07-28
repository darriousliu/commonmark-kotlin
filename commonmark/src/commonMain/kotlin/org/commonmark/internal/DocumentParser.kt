package org.commonmark.internal

import okio.BufferedSource
import okio.IOException
import org.commonmark.internal.util.LineReader
import org.commonmark.internal.util.Parsing
import org.commonmark.node.Block
import org.commonmark.node.BlockQuote
import org.commonmark.node.Document
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.ListBlock
import org.commonmark.node.Paragraph
import org.commonmark.node.SourceSpan
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.InlineParserFactory
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.block.BlockParser
import org.commonmark.parser.block.BlockParserFactory
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.text.Characters
import kotlin.math.min
import kotlin.reflect.KClass

class DocumentParser(
    private val blockParserFactories: List<BlockParserFactory>,
    private val inlineParserFactory: InlineParserFactory,
    private val inlineContentParserFactories: List<InlineContentParserFactory>,
    private val delimiterProcessors: List<DelimiterProcessor>,
    private val linkProcessors: List<LinkProcessor>,
    private val linkMarkers: Set<Char>,
    private val includeSourceSpans: IncludeSourceSpans
) : ParserState {
    private var innerLine: SourceLine? = null
    override val line: SourceLine
        get() = innerLine!!

    /**
     * Line index (0-based)
     */
    private var lineIndex = -1

    /**
     * current index (offset) in input line (0-based)
     */
    override var index: Int = 0
        private set

    /**
     * current column of input line (tab causes column to go to the next 4-space tab stop) (0-based)
     */
    override var column: Int = 0
        private set

    /**
     * if the current column is within a tab character (partially consumed tab)
     */
    private var columnIsInTab = false

    override var nextNonSpaceIndex: Int = 0
        private set
    private var nextNonSpaceColumn = 0

    override var indent: Int = 0
        private set

    override var isBlank: Boolean = false
        private set

    private val documentBlockParser: DocumentBlockParser = DocumentBlockParser()
    private val definitions: Definitions = Definitions()

    private val openBlockParsers: MutableList<OpenBlockParser> = mutableListOf()
    private val allBlockParsers: MutableList<BlockParser> = mutableListOf()

    init {
        activateBlockParser(OpenBlockParser(documentBlockParser, 0))
    }

    /**
     * The main parsing function. Returns a parsed document AST.
     */
    fun parse(input: String): Document {
        var lineStart = 0
        var lineBreak: Int
        while ((Characters.findLineBreak(input, lineStart).also { lineBreak = it }) != -1) {
            val line = input.substring(lineStart, lineBreak)
            parseLine(line, lineStart)
            lineStart =
                if (lineBreak + 1 < input.length && input[lineBreak] == '\r' && input[lineBreak + 1] == '\n') {
                    lineBreak + 2
                } else {
                    lineBreak + 1
                }
        }
        if (!input.isEmpty() && (lineStart == 0 || lineStart < input.length)) {
            val line = input.substring(lineStart)
            parseLine(line, lineStart)
        }

        return finalizeAndProcess()
    }

    @Throws(IOException::class)
    fun parse(input: BufferedSource): Document {
        val lineReader = LineReader(input)
        var inputIndex = 0
        var line: String?
        while (true) {
            line = lineReader.readLine()
            if (line == null) break
            parseLine(line, inputIndex)
            inputIndex += line.length
            val eol = lineReader.lineTerminator
            if (eol != null) {
                inputIndex += eol.length
            }
        }

        return finalizeAndProcess()
    }

    override val activeBlockParser: BlockParser
        get() = openBlockParsers[openBlockParsers.size - 1].blockParser

    /**
     * Analyze a line of text and update the document appropriately. We parse markdown text by calling this on each
     * line of input, then finalizing the document.
     */
    private fun parseLine(ln: String, inputIndex: Int) {
        setLine(ln, inputIndex)

        // For each containing block, try to parse the associated line start.
        // The document will always match, so we can skip the first block parser and start at 1 matches
        var matches = 1
        for (i in 1..<openBlockParsers.size) {
            val openBlockParser = openBlockParsers[i]
            val blockParser = openBlockParser.blockParser
            findNextNonSpace()

            val result = blockParser.tryContinue(this)
            if (result is BlockContinueImpl) {
                val blockContinue = result
                openBlockParser.sourceIndex = this.index
                if (blockContinue.isFinalize) {
                    addSourceSpans()
                    closeBlockParsers(openBlockParsers.size - i)
                    return
                } else {
                    if (blockContinue.newIndex != -1) {
                        setNewIndex(blockContinue.newIndex)
                    } else if (blockContinue.newColumn != -1) {
                        setNewColumn(blockContinue.newColumn)
                    }
                    matches++
                }
            } else {
                break
            }
        }

        var unmatchedBlocks = openBlockParsers.size - matches
        var blockParser = openBlockParsers[matches - 1].blockParser
        var startedNewBlock = false

        var lastIndex = index

        // Unless the last matched container is a code block, try new container starts,
        // adding children to the last matched container:
        var tryBlockStarts = blockParser.block is Paragraph || blockParser.isContainer
        while (tryBlockStarts) {
            lastIndex = index
            findNextNonSpace()

            // this is a little performance optimization:
            if (this.isBlank || (indent < Parsing.CODE_BLOCK_INDENT && Characters.isLetter(
                    this.line.content,
                    this.nextNonSpaceIndex
                ))
            ) {
                setNewIndex(this.nextNonSpaceIndex)
                break
            }

            val blockStart = findBlockStart(blockParser)
            if (blockStart == null) {
                setNewIndex(this.nextNonSpaceIndex)
                break
            }

            startedNewBlock = true
            val sourceIndex = this.index

            // We're starting a new block. If we have any previous blocks that need to be closed, we need to do it now.
            if (unmatchedBlocks > 0) {
                closeBlockParsers(unmatchedBlocks)
                unmatchedBlocks = 0
            }

            if (blockStart.newIndex != -1) {
                setNewIndex(blockStart.newIndex)
            } else if (blockStart.newColumn != -1) {
                setNewColumn(blockStart.newColumn)
            }

            var replacedSourceSpans: List<SourceSpan>? = null
            if (blockStart.replaceParagraphLines >= 1 || blockStart.isReplaceActiveBlockParser) {
                val activeBlockParser = this.activeBlockParser
                if (activeBlockParser is ParagraphParser) {
                    val paragraphParser = activeBlockParser
                    val lines =
                        if (blockStart.isReplaceActiveBlockParser) Int.MAX_VALUE else blockStart.replaceParagraphLines
                    replacedSourceSpans = replaceParagraphLines(lines, paragraphParser)
                } else if (blockStart.isReplaceActiveBlockParser) {
                    replacedSourceSpans = prepareActiveBlockParserForReplacement(activeBlockParser)
                }
            }

            for (newBlockParser in blockStart.getBlockParsers()) {
                addChild(OpenBlockParser(newBlockParser, sourceIndex))
                if (replacedSourceSpans != null) {
                    newBlockParser.block.setSourceSpans(replacedSourceSpans)
                }
                blockParser = newBlockParser
                tryBlockStarts = newBlockParser.isContainer
            }
        }

        // What remains at the offset is a text line. Add the text to the
        // appropriate block.

        // First check for a lazy continuation line
        if (!startedNewBlock && !this.isBlank &&
            this.activeBlockParser.canHaveLazyContinuationLines()
        ) {
            openBlockParsers[openBlockParsers.size - 1].sourceIndex = lastIndex
            // lazy paragraph continuation
            addLine()
        } else {
            // finalize any blocks not matched
            if (unmatchedBlocks > 0) {
                closeBlockParsers(unmatchedBlocks)
            }

            if (!blockParser.isContainer) {
                addLine()
            } else if (!this.isBlank) {
                // create a paragraph container for line
                val paragraphParser = ParagraphParser()
                addChild(OpenBlockParser(paragraphParser, lastIndex))
                addLine()
            } else {
                // This can happen for a list item like this:
                // ```
                // *
                // list item
                // ```
                //
                // The first line does not start a paragraph yet, but we still want to record source positions.
                addSourceSpans()
            }
        }
    }

    private fun setLine(ln: String, inputIndex: Int) {
        lineIndex++
        index = 0
        column = 0
        columnIsInTab = false

        val lineContent = prepareLine(ln)
        var sourceSpan: SourceSpan? = null
        if (includeSourceSpans !== IncludeSourceSpans.NONE) {
            sourceSpan = SourceSpan.of(lineIndex, 0, inputIndex, lineContent.length)
        }
        this.innerLine = SourceLine.of(lineContent, sourceSpan)
    }

    private fun findNextNonSpace() {
        var i = index
        var cols = column

        this.isBlank = true
        val length = line.content.length
        while (i < length) {
            val c = line.content[i]
            when (c) {
                ' ' -> {
                    i++
                    cols++
                    continue
                }

                '\t' -> {
                    i++
                    cols += (4 - (cols % 4))
                    continue
                }
            }
            this.isBlank = false
            break
        }

        this.nextNonSpaceIndex = i
        nextNonSpaceColumn = cols
        indent = nextNonSpaceColumn - column
    }

    private fun setNewIndex(newIndex: Int) {
        if (newIndex >= this.nextNonSpaceIndex) {
            // We can start from here, no need to calculate tab stops again
            index = this.nextNonSpaceIndex
            column = nextNonSpaceColumn
        }
        val length = line.content.length
        while (index < newIndex && index != length) {
            advance()
        }
        // If we're going to an index as opposed to a column, we're never within a tab
        columnIsInTab = false
    }

    private fun setNewColumn(newColumn: Int) {
        if (newColumn >= nextNonSpaceColumn) {
            // We can start from here, no need to calculate tab stops again
            index = this.nextNonSpaceIndex
            column = nextNonSpaceColumn
        }
        val length = line.content.length
        while (column < newColumn && index != length) {
            advance()
        }
        if (column > newColumn) {
            // The last character was a tab and we overshot our target
            index--
            column = newColumn
            columnIsInTab = true
        } else {
            columnIsInTab = false
        }
    }

    private fun advance() {
        val c = line.content[index]
        index++
        if (c == '\t') {
            column += Parsing.columnsToNextTabStop(column)
        } else {
            column++
        }
    }

    /**
     * Add line content to the active block parser. We assume it can accept lines -- that check should be done before
     * calling this.
     */
    private fun addLine() {
        val content: CharSequence?
        if (columnIsInTab) {
            // Our column is in a partially consumed tab. Expand the remaining columns (to the next tab stop) to spaces.
            val afterTab = index + 1
            val rest = line.content.subSequence(afterTab, line.content.length)
            val spaces = Parsing.columnsToNextTabStop(column)
            val sb = StringBuilder(spaces + rest.length)
            repeat(spaces) {
                sb.append(' ')
            }
            sb.append(rest)
            content = sb.toString()
        } else if (index == 0) {
            content = line.content
        } else {
            content = line.content.subSequence(index, line.content.length)
        }
        var sourceSpan: SourceSpan? = null
        if (includeSourceSpans === IncludeSourceSpans.BLOCKS_AND_INLINES && index < line.getSourceSpan()!!.length) {
            // Note that if we're in a partially consumed tab, the length of the source span and the content don't match.
            sourceSpan = line.getSourceSpan()?.subSpan(index)
        }
        this.activeBlockParser.addLine(SourceLine.of(content, sourceSpan))
        addSourceSpans()
    }

    private fun addSourceSpans() {
        if (includeSourceSpans !== IncludeSourceSpans.NONE) {
            // Don't add source spans for the Document itself (it would get the whole source text), so start at 1, not 0
            for (i in 1..<openBlockParsers.size) {
                val openBlockParser = openBlockParsers[i]
                // In case of a lazy continuation line, the index is less than where the block parser would expect the
                // contents to start, so let's use whichever is smaller.
                val blockIndex = min(openBlockParser.sourceIndex, index)
                val length = line.content.length - blockIndex
                if (length != 0) {
                    openBlockParser.blockParser.addSourceSpan(
                        line.getSourceSpan()!!.subSpan(blockIndex)
                    )
                }
            }
        }
    }

    private fun findBlockStart(blockParser: BlockParser): BlockStartImpl? {
        val matchedBlockParser = MatchedBlockParserImpl(blockParser)
        for (blockParserFactory in blockParserFactories) {
            val result = blockParserFactory.tryStart(this, matchedBlockParser)
            if (result is BlockStartImpl) {
                return result
            }
        }
        return null
    }

    /**
     * Walk through a block & children recursively, parsing string content into inline content where appropriate.
     */
    private fun processInlines() {
        val context = InlineParserContextImpl(
            inlineContentParserFactories,
            delimiterProcessors,
            linkProcessors,
            linkMarkers,
            definitions
        )
        val inlineParser = inlineParserFactory.create(context)

        for (blockParser in allBlockParsers) {
            blockParser.parseInlines(inlineParser)
        }
    }

    /**
     * Add a block of type tag as a child of the tip. If the tip can't accept children, close and finalize it and try
     * its parent, and so on until we find a block that can accept children.
     */
    private fun addChild(openBlockParser: OpenBlockParser) {
        while (!this.activeBlockParser.canContain(openBlockParser.blockParser.block)) {
            closeBlockParsers(1)
        }

        this.activeBlockParser.block.appendChild(openBlockParser.blockParser.block)
        activateBlockParser(openBlockParser)
    }

    private fun activateBlockParser(openBlockParser: OpenBlockParser) {
        openBlockParsers.add(openBlockParser)
    }

    private fun deactivateBlockParser(): OpenBlockParser {
        return openBlockParsers.removeAt(openBlockParsers.size - 1)
    }

    private fun replaceParagraphLines(
        lines: Int,
        paragraphParser: ParagraphParser
    ): List<SourceSpan> {
        // Remove lines from the paragraph as the new block is using them.
        // If all lines are used, this also unlinks the Paragraph block.
        val sourceSpans = paragraphParser.removeLines(lines)
        // Close the paragraph block parser, which will finalize it.
        closeBlockParsers(1)
        return sourceSpans
    }

    private fun prepareActiveBlockParserForReplacement(blockParser: BlockParser): List<SourceSpan> {
        // Note that we don't want to parse inlines here, as it's getting replaced.
        deactivateBlockParser()

        // Do this so that source positions are calculated, which we will carry over to the replacing block.
        blockParser.closeBlock()
        blockParser.block.unlink()
        return blockParser.block.getSourceSpans()
    }

    private fun finalizeAndProcess(): Document {
        closeBlockParsers(openBlockParsers.size)
        processInlines()
        return documentBlockParser.block
    }

    private fun closeBlockParsers(count: Int) {
        repeat(count) {
            val blockParser: BlockParser = deactivateBlockParser().blockParser
            finalize(blockParser)
            // Remember for inline parsing. Note that a lot of blocks don't need inline parsing. We could have a
            // separate interface (e.g., BlockParserWithInlines) so that we only have to remember those that actually
            // have inlines to parse.
            allBlockParsers.add(blockParser)
        }
    }

    /**
     * Finalize a block. Close it and do any necessary postprocessing, e.g. setting the content of blocks and
     * collecting link reference definitions from paragraphs.
     */
    private fun finalize(blockParser: BlockParser) {
        addDefinitionsFrom(blockParser)
        blockParser.closeBlock()
    }

    private fun addDefinitionsFrom(blockParser: BlockParser) {
        for (definitionMap in blockParser.definitions) {
            definitions.addDefinitions(definitionMap)
        }
    }

    private class MatchedBlockParserImpl(override val matchedBlockParser: BlockParser) :
        MatchedBlockParser {
        override val paragraphLines: SourceLines
            get() {
                if (matchedBlockParser is ParagraphParser) {
                    return matchedBlockParser.paragraphLines
                }
                return SourceLines.empty()
            }
    }

    private class OpenBlockParser(
        val blockParser: BlockParser,
        var sourceIndex: Int
    )

    companion object {
        private val CORE_FACTORY_TYPES = hashSetOf<KClass<out Block>>(
            BlockQuote::class,
            Heading::class,
            FencedCodeBlock::class,
            HtmlBlock::class,
            ThematicBreak::class,
            ListBlock::class,
            IndentedCodeBlock::class
        )

        private val NODES_TO_CORE_FACTORIES: Map<KClass<out Block>, BlockParserFactory> = hashMapOf(
            BlockQuote::class to BlockQuoteParser.Factory(),
            Heading::class to HeadingParser.Factory(),
            FencedCodeBlock::class to FencedCodeBlockParser.Factory(),
            HtmlBlock::class to HtmlBlockParser.Factory(),
            ThematicBreak::class to ThematicBreakParser.Factory(),
            ListBlock::class to ListBlockParser.Factory(),
            IndentedCodeBlock::class to IndentedCodeBlockParser.Factory()
        )

        val defaultBlockParserTypes: Set<KClass<out Block>>
            get() = CORE_FACTORY_TYPES

        fun calculateBlockParserFactories(
            customBlockParserFactories: List<BlockParserFactory>,
            enabledBlockTypes: Set<KClass<out Block>>
        ): List<BlockParserFactory> {
            val list = mutableListOf<BlockParserFactory>()
            // By having the custom factories come first, extensions are able to change behavior of core syntax.
            list.addAll(customBlockParserFactories)
            for (blockType in enabledBlockTypes) {
                list.add(NODES_TO_CORE_FACTORIES[blockType]!!)
            }
            return list
        }

        fun checkEnabledBlockTypes(enabledBlockTypes: Set<KClass<out Block>>) {
            for (enabledBlockType in enabledBlockTypes) {
                require(NODES_TO_CORE_FACTORIES.containsKey(enabledBlockType)) { "Can't enable block type " + enabledBlockType + ", possible options are: " + NODES_TO_CORE_FACTORIES.keys }
            }
        }

        /**
         * Prepares the input line replacing `\0`
         */
        private fun prepareLine(line: String): String {
            return if (line.indexOf('\u0000') == -1) {
                line
            } else {
                line.replace('\u0000', '\uFFFD')
            }
        }
    }
}
