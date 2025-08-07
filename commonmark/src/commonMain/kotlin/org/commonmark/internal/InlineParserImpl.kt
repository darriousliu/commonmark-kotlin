package org.commonmark.internal

import org.commonmark.ext.computeIfAbsent2
import org.commonmark.internal.inline.AsteriskDelimiterProcessor
import org.commonmark.internal.inline.AutolinkInlineParser
import org.commonmark.internal.inline.BackslashInlineParser
import org.commonmark.internal.inline.BackticksInlineParser
import org.commonmark.internal.inline.CoreLinkProcessor
import org.commonmark.internal.inline.EntityInlineParser
import org.commonmark.internal.inline.HtmlInlineParser
import org.commonmark.internal.inline.LinkResultImpl
import org.commonmark.internal.inline.ParsedInlineImpl
import org.commonmark.internal.inline.UnderscoreDelimiterProcessor
import org.commonmark.internal.util.Escaping
import org.commonmark.internal.util.LinkScanner
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.SourceSpans
import org.commonmark.node.Text
import org.commonmark.parser.InlineParser
import org.commonmark.parser.InlineParserContext
import org.commonmark.parser.SourceLines
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.LinkInfo
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.beta.Position
import org.commonmark.parser.beta.Scanner
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.text.Characters
import org.commonmark.type.BitSetWrapper

class InlineParserImpl(
    private val context: InlineParserContext
) : InlineParser, InlineParserState {
    private val inlineContentParserFactories: List<InlineContentParserFactory>
    private val delimiterProcessors: Map<Char, DelimiterProcessor?>
    private val linkProcessors: List<LinkProcessor>
    private val specialCharacters: BitSetWrapper
    private val linkMarkers: BitSetWrapper

    private lateinit var inlineParsers: Map<Char, List<InlineContentParser>>
    private lateinit var scanner: Scanner
    private var includeSourceSpans = false
    private var trailingSpaces = 0

    /**
     * Top delimiter (emphasis, strong emphasis or custom emphasis). (Brackets are on a separate stack, different
     * from the algorithm described in the spec.)
     */
    private var lastDelimiter: Delimiter? = null

    /**
     * Top opening bracket (`[` or `![)`).
     */
    private var lastBracket: Bracket? = null

    init {
        this.inlineContentParserFactories =
            calculateInlineContentParserFactories(context.customInlineContentParserFactories)
        this.delimiterProcessors = calculateDelimiterProcessors(context.customDelimiterProcessors)
        this.linkProcessors = calculateLinkProcessors(context.customLinkProcessors)
        this.linkMarkers = calculateLinkMarkers(context.customLinkMarkers)
        this.specialCharacters = calculateSpecialCharacters(
            linkMarkers,
            this.delimiterProcessors.keys,
            this.inlineContentParserFactories
        )
    }

    private fun calculateInlineContentParserFactories(customFactories: List<InlineContentParserFactory>): List<InlineContentParserFactory> {
        // Custom parsers can override built-in parsers if they want, so make sure they are tried first
        val list = buildList {
            addAll(customFactories)
            add(BackslashInlineParser.Factory())
            add(BackticksInlineParser.Factory())
            add(EntityInlineParser.Factory())
            add(AutolinkInlineParser.Factory())
            add(HtmlInlineParser.Factory())
        }
        return list
    }

    private fun calculateLinkProcessors(linkProcessors: List<LinkProcessor>): List<LinkProcessor> {
        // Custom link processors can override the built-in behavior, so make sure they are tried first
        val list = buildList {
            addAll(linkProcessors)
            add(CoreLinkProcessor())
        }
        return list
    }

    private fun createInlineContentParsers(): Map<Char, List<InlineContentParser>> {
        val map = hashMapOf<Char, MutableList<InlineContentParser>>()
        for (factory in inlineContentParserFactories) {
            val parser = factory.create()
            for (c in factory.triggerCharacters) {
                map.computeIfAbsent2(c) { k -> ArrayList() }.add(parser)
            }
        }
        return map
    }

    override fun scanner(): Scanner {
        return scanner
    }

    /**
     * Parse content in the block into inline children, appending them to the block node.
     */
    override fun parse(lines: SourceLines, block: Node) {
        reset(lines)

        while (true) {
            val nodes = parseInline()
            if (nodes == null) {
                break
            }
            for (node in nodes) {
                block.appendChild(node)
            }
        }

        processDelimiters(null)
        mergeChildTextNodes(block)
    }

    fun reset(lines: SourceLines) {
        this.scanner = Scanner.of(lines)
        this.includeSourceSpans = !lines.sourceSpans.isEmpty()
        this.trailingSpaces = 0
        this.lastDelimiter = null
        this.lastBracket = null
        this.inlineParsers = createInlineContentParsers()
    }

    private fun text(sourceLines: SourceLines): Text {
        val text = Text(sourceLines.content)
        text.setSourceSpans(sourceLines.sourceSpans)
        return text
    }

    /**
     * Parse the next inline element in the subject, advancing our position.
     * On success, return the new inline node.
     * On failure, return null.
     */
    private fun parseInline(): List<Node>? {
        val c = scanner.peek()

        when (c) {
            '[' -> return listOf(parseOpenBracket())
            ']' -> return listOf(parseCloseBracket())
            '\n' -> return listOf(parseLineBreak())
            Scanner.END -> return null
        }

        if (linkMarkers[c.code]) {
            val markerPosition = scanner.position()
            val nodes = parseLinkMarker()
            if (nodes != null) {
                return nodes
            }
            // Reset and try other things (e.g., inline parsers below)
            scanner.setPosition(markerPosition)
        }

        // No inline parser, delimiter or another special handling.
        if (!specialCharacters[c.code]) {
            return listOf(parseText())
        }

        val inlineParsers = this.inlineParsers[c]
        if (inlineParsers != null) {
            val position = scanner.position()
            for (inlineParser in inlineParsers) {
                val parsedInline = inlineParser.tryParse(this)
                if (parsedInline is ParsedInlineImpl) {
                    val parsedInlineImpl = parsedInline
                    val node = parsedInlineImpl.node
                    scanner.setPosition(parsedInlineImpl.position)
                    if (includeSourceSpans && node.getSourceSpans().isEmpty()) {
                        node.setSourceSpans(
                            scanner.getSource(
                                position,
                                scanner.position()
                            ).sourceSpans
                        )
                    }
                    return listOf(node)
                } else {
                    // Reset position
                    scanner.setPosition(position)
                }
            }
        }

        val delimiterProcessor = delimiterProcessors[c]
        if (delimiterProcessor != null) {
            val nodes = parseDelimiters(delimiterProcessor, c)
            if (nodes != null) {
                return nodes
            }
        }

        // If we get here, even for a special/delimiter character, we will just treat it as text.
        return listOf(parseText())
    }

    /**
     * Attempt to parse delimiters like emphasis, strong emphasis or custom delimiters.
     */
    private fun parseDelimiters(
        delimiterProcessor: DelimiterProcessor,
        delimiterChar: Char
    ): List<Node>? {
        val res = scanDelimiters(delimiterProcessor, delimiterChar)
        if (res == null) {
            return null
        }

        val characters = res.characters

        // Add entry to stack for this opener
        val lastDelimiter =
            Delimiter(characters, delimiterChar, res.canOpen, res.canClose, lastDelimiter)
                .also { lastDelimiter = it }
        val previous = lastDelimiter.previous
        if (previous != null) {
            previous.next = lastDelimiter
        }

        return characters
    }

    /**
     * Add open bracket to delimiter stack and add a text node to block's children.
     */
    private fun parseOpenBracket(): Node {
        val start = scanner.position()
        scanner.next()
        val contentPosition = scanner.position()

        val node: Text = text(scanner.getSource(start, contentPosition))

        // Add entry to stack for this opener
        addBracket(Bracket.link(node, start, contentPosition, lastBracket, lastDelimiter))

        return node
    }

    /**
     * If next character is `[`, add a bracket to the stack.
     * Otherwise, return null.
     */
    private fun parseLinkMarker(): List<Node>? {
        val markerPosition = scanner.position()
        scanner.next()
        val bracketPosition = scanner.position()
        if (scanner.next('[')) {
            val contentPosition = scanner.position()
            val bangNode = text(scanner.getSource(markerPosition, bracketPosition))
            val bracketNode = text(scanner.getSource(bracketPosition, contentPosition))

            // Add entry to stack for this opener
            addBracket(
                Bracket.withMarker(
                    bangNode,
                    markerPosition,
                    bracketNode,
                    bracketPosition,
                    contentPosition,
                    lastBracket,
                    lastDelimiter
                )
            )
            return listOf(bangNode, bracketNode)
        } else {
            return null
        }
    }

    /**
     * Try to match close bracket against an opening in the delimiter stack. Return either a link or image, or a
     * plain [ character. If there is a matching delimiter, remove it from the delimiter stack.
     */
    private fun parseCloseBracket(): Node {
        val beforeClose = scanner.position()
        scanner.next()
        val afterClose = scanner.position()

        // Get previous `[` or `![`
        val opener = lastBracket
        if (opener == null) {
            // No matching opener, return a literal.
            return text(scanner.getSource(beforeClose, afterClose))
        }

        if (!opener.allowed) {
            // Matching opener, but it's not allowed, just return a literal.
            removeLastBracket()
            return text(scanner.getSource(beforeClose, afterClose))
        }

        val linkOrImage = parseLinkOrImage(opener, beforeClose)
        if (linkOrImage != null) {
            return linkOrImage
        }
        scanner.setPosition(afterClose)

        // Nothing parsed, just parse the bracket as text and continue
        removeLastBracket()
        return text(scanner.getSource(beforeClose, afterClose))
    }

    private fun parseLinkOrImage(opener: Bracket, beforeClose: Position): Node? {
        val linkInfo = parseLinkInfo(opener, beforeClose)
        if (linkInfo == null) {
            return null
        }
        val processorStartPosition = scanner.position()

        for (linkProcessor in linkProcessors) {
            val linkResult = linkProcessor.process(linkInfo, scanner, context)
            if (linkResult !is LinkResultImpl) {
                // Reset position in case the processor used the scanner, and it didn't work out.
                scanner.setPosition(processorStartPosition)
                continue
            }

            val result = linkResult
            val node = result.getNode()
            val position = result.getPosition()
            val includeMarker = result.isIncludeMarker

            when (result.type) {
                LinkResultImpl.Type.WRAP -> {
                    scanner.setPosition(position)
                    return wrapBracket(opener, node, includeMarker)
                }

                LinkResultImpl.Type.REPLACE -> {
                    scanner.setPosition(position)
                    return replaceBracket(opener, node, includeMarker)
                }
            }
        }

        return null
    }

    private fun parseLinkInfo(opener: Bracket, beforeClose: Position): LinkInfo? {
        // Check to see if we have a link (or image, with a ! in front). The different types:
        // - Inline:       `[foo](/uri)` or with optional title `[foo](/uri "title")`
        // - Reference links
        //   - Full:      `[foo][bar]` (foo is the text and bar is the label that needs to match a reference)
        //   - Collapsed: `[foo][]`    (foo is both the text and label)
        //   - Shortcut:  `[foo]`      (foo is both the text and label)

        val text = scanner.getSource(opener.contentPosition, beforeClose).content

        // Starting position is after the closing `]`
        val afterClose = scanner.position()

        // Maybe an inline link/image
        val destinationTitle = parseInlineDestinationTitle(scanner)
        if (destinationTitle != null) {
            return LinkInfoImpl(
                opener.markerNode,
                opener.bracketNode,
                text,
                null,
                destinationTitle.destination,
                destinationTitle.title,
                afterClose
            )
        }
        // Not an inline link/image, rewind back to after `]`.
        scanner.setPosition(afterClose)

        // Maybe a reference link/image like `[foo][bar]`, `[foo][]` or `[foo]`.
        // Note that even `[foo](` could be a valid link if foo is a reference, which is why we try this even if the `(`
        // failed to be parsed as an inline link/image before.

        // See if there's a link label like `[bar]` or `[]`
        val label = parseLinkLabel(scanner)
        if (label == null) {
            // No label, rewind back
            scanner.setPosition(afterClose)
        }
        val textIsReference = label == null || label.isEmpty()
        if (opener.bracketAfter && textIsReference && opener.markerNode == null) {
            // In case of shortcut or collapsed links, the text is used as the reference. But the reference is not allowed to
            // contain an unescaped bracket, so if that's the case, we don't need to continue. This is an optimization.
            return null
        }

        return LinkInfoImpl(
            opener.markerNode,
            opener.bracketNode,
            text,
            label,
            null,
            null,
            afterClose
        )
    }

    private fun wrapBracket(opener: Bracket, wrapperNode: Node, includeMarker: Boolean): Node {
        // Add all nodes between the opening bracket and now (closing bracket) as child nodes of the link
        var n = opener.bracketNode?.next
        while (n != null) {
            val next = n.next
            wrapperNode.appendChild(n)
            n = next
        }

        if (includeSourceSpans) {
            val startPosition =
                if (includeMarker && opener.markerPosition != null) opener.markerPosition else opener.bracketPosition
            wrapperNode.setSourceSpans(
                scanner.getSource(
                    startPosition,
                    scanner.position()
                ).sourceSpans
            )
        }

        // Process delimiters such as emphasis inside a link / image
        processDelimiters(opener.previousDelimiter)
        mergeChildTextNodes(wrapperNode)
        // We don't need the corresponding text node anymore, we turned it into a link/image node
        if (includeMarker && opener.markerNode != null) {
            opener.markerNode.unlink()
        }
        opener.bracketNode?.unlink()
        removeLastBracket()

        // Links within links are not allowed. We found this link, so there can be no other link around it.
        if (opener.markerNode == null) {
            var bracket = lastBracket
            while (bracket != null) {
                if (bracket.markerNode == null) {
                    // Disallow link opener. It will still get matched, but will not result in a link.
                    bracket.allowed = false
                }
                bracket = bracket.previous
            }
        }

        return wrapperNode
    }

    private fun replaceBracket(opener: Bracket, node: Node, includeMarker: Boolean): Node {
        // Remove delimiters (but keep text nodes)
        while (lastDelimiter != null && lastDelimiter !== opener.previousDelimiter) {
            removeDelimiterKeepNode(lastDelimiter!!)
        }

        if (includeSourceSpans) {
            val startPosition =
                if (includeMarker && opener.markerPosition != null) opener.markerPosition else opener.bracketPosition
            node.setSourceSpans(scanner.getSource(startPosition, scanner.position()).sourceSpans)
        }

        removeLastBracket()

        // Remove nodes that we've added since the opener, because we're replacing them
        var n: Node? =
            if (includeMarker && opener.markerNode != null) opener.markerNode else opener.bracketNode
        while (n != null) {
            val next = n.next
            n.unlink()
            n = next
        }
        return node
    }

    private fun addBracket(bracket: Bracket?) {
        val lastBracket = lastBracket
        if (lastBracket != null) {
            lastBracket.bracketAfter = true
        }
        this.lastBracket = bracket
    }

    private fun removeLastBracket() {
        lastBracket = lastBracket!!.previous
    }

    private fun parseLineBreak(): Node {
        scanner.next()

        return if (trailingSpaces >= 2) {
            HardLineBreak()
        } else {
            SoftLineBreak()
        }
    }

    /**
     * Parse the next character as plain text, and possibly more if the following characters are non-special.
     */
    private fun parseText(): Node {
        val start = scanner.position()
        scanner.next()
        var c: Char
        while (true) {
            c = scanner.peek()
            if (c == Scanner.END || specialCharacters[c.code]) {
                break
            }
            scanner.next()
        }

        val source = scanner.getSource(start, scanner.position())
        var content = source.content

        if (c == '\n') {
            // We parsed until the end of the line. Trim any trailing spaces and remember them (for hard line breaks).
            val end = Characters.skipBackwards(' ', content, content.length - 1, 0) + 1
            trailingSpaces = content.length - end
            content = content.substring(0, end)
        } else if (c == Scanner.END) {
            // For the last line, both tabs and spaces are trimmed for some reason (checked with commonmark.js).
            val end = Characters.skipSpaceTabBackwards(content, content.length - 1, 0) + 1
            content = content.substring(0, end)
        }

        val text = Text(content)
        text.setSourceSpans(source.sourceSpans)
        return text
    }

    /**
     * Scan a sequence of characters with code delimiterChar and return information about the number of delimiters
     * and whether they are positioned such that they can open and/or close emphasis or strong emphasis.
     *
     * @return information about delimiter run, or `null`
     */
    private fun scanDelimiters(
        delimiterProcessor: DelimiterProcessor,
        delimiterChar: Char
    ): DelimiterData? {
        val before = scanner.peekPreviousCodePoint()
        val start = scanner.position()

        // Quick check to see if we have enough delimiters.
        val delimiterCount: Int = scanner.matchMultiple(delimiterChar)
        if (delimiterCount < delimiterProcessor.minLength) {
            scanner.setPosition(start)
            return null
        }

        // We do have enough, extract a text node for each delimiter character.
        val delimiters = mutableListOf<Text>()
        scanner.setPosition(start)
        var positionBefore = start
        while (scanner.next(delimiterChar)) {
            delimiters.add(text(scanner.getSource(positionBefore, scanner.position())))
            positionBefore = scanner.position()
        }

        val after: Int = scanner.peekCodePoint()

        // We could be more lazy here, in most cases we don't need to do every match case.
        val beforeIsPunctuation =
            before == Scanner.END.code || Characters.isPunctuationCodePoint(before)
        val beforeIsWhitespace =
            before == Scanner.END.code || Characters.isWhitespaceCodePoint(before)
        val afterIsPunctuation =
            after == Scanner.END.code || Characters.isPunctuationCodePoint(after)
        val afterIsWhitespace = after == Scanner.END.code || Characters.isWhitespaceCodePoint(after)

        val leftFlanking =
            !afterIsWhitespace && (!afterIsPunctuation || beforeIsWhitespace || beforeIsPunctuation)
        val rightFlanking =
            !beforeIsWhitespace && (!beforeIsPunctuation || afterIsWhitespace || afterIsPunctuation)
        val canOpen: Boolean
        val canClose: Boolean
        if (delimiterChar == '_') {
            canOpen = leftFlanking && (!rightFlanking || beforeIsPunctuation)
            canClose = rightFlanking && (!leftFlanking || afterIsPunctuation)
        } else {
            canOpen = leftFlanking && delimiterChar == delimiterProcessor.openingCharacter
            canClose = rightFlanking && delimiterChar == delimiterProcessor.closingCharacter
        }

        return DelimiterData(delimiters, canOpen, canClose)
    }

    private fun processDelimiters(stackBottom: Delimiter?) {
        val openersBottom = hashMapOf<Char, Delimiter?>()

        // find first closer above stackBottom:
        var closer = lastDelimiter
        while (closer != null && closer.previous !== stackBottom) {
            closer = closer.previous
        }
        // move forward, looking for closers and handling each
        while (closer != null) {
            val delimiterChar = closer.delimiterChar

            val delimiterProcessor = delimiterProcessors[delimiterChar]
            if (!closer.canClose() || delimiterProcessor == null) {
                closer = closer.next
                continue
            }

            val openingDelimiterChar = delimiterProcessor.openingCharacter

            // Found delimiter closer. Now look back for the first matching opener.
            var usedDelims = 0
            var openerFound = false
            var potentialOpenerFound = false
            var opener = closer.previous
            while (opener != null && opener !== stackBottom && opener !== openersBottom[delimiterChar]) {
                if (opener.canOpen() && opener.delimiterChar == openingDelimiterChar) {
                    potentialOpenerFound = true
                    usedDelims = delimiterProcessor.process(opener, closer)
                    if (usedDelims > 0) {
                        openerFound = true
                        break
                    }
                }
                opener = opener.previous
            }

            if (!openerFound) {
                if (!potentialOpenerFound) {
                    // Set a lower bound for future searches for openers.
                    // Only do this when we didn't even have a potential
                    // opener (one that matches the character and can open).
                    // If an opener was rejected because of the number of
                    // delimiters (e.g., because of the "multiple of 3" rule),
                    // we want to consider it next time because the number
                    // of delimiters can change as we continue processing.
                    openersBottom.put(delimiterChar, closer.previous)
                    if (!closer.canOpen()) {
                        // We can remove a closer that can't be an opener,
                        // once we've seen there's no matching opener:
                        removeDelimiterKeepNode(closer)
                    }
                }
                closer = closer.next
                continue
            }

            // Remove the number of used delimiters nodes.
            repeat(usedDelims) {
                val delimiter = opener!!.characters.removeAt(opener.characters.size - 1)
                delimiter.unlink()
            }
            repeat(usedDelims) {
                val delimiter = closer.characters.removeAt(0)
                delimiter.unlink()
            }

            removeDelimitersBetween(opener, closer)

            // No delimiter characters left to process, so we can remove the delimiter and the now empty node.
            if (opener!!.length() == 0) {
                removeDelimiterAndNodes(opener)
            }

            if (closer.length() == 0) {
                val next = closer.next
                removeDelimiterAndNodes(closer)
                closer = next
            }
        }

        // remove all delimiters
        while (lastDelimiter != null && lastDelimiter !== stackBottom) {
            removeDelimiterKeepNode(lastDelimiter!!)
        }
    }

    private fun removeDelimitersBetween(opener: Delimiter?, closer: Delimiter) {
        var delimiter = closer.previous
        while (delimiter != null && delimiter !== opener) {
            val previousDelimiter = delimiter.previous
            removeDelimiterKeepNode(delimiter)
            delimiter = previousDelimiter
        }
    }

    /**
     * Remove the delimiter and the corresponding text node. For used delimiters, e.g. `*` in `*foo*`.
     */
    private fun removeDelimiterAndNodes(delim: Delimiter) {
        removeDelimiter(delim)
    }

    /**
     * Remove the delimiter but keep the corresponding node as text. For unused delimiters such as `_` in `foo_bar`.
     */
    private fun removeDelimiterKeepNode(delim: Delimiter) {
        removeDelimiter(delim)
    }

    private fun removeDelimiter(delim: Delimiter) {
        val previous = delim.previous
        if (previous != null) {
            previous.next = delim.next
        }
        val next = delim.next
        if (next == null) {
            // top of stack
            lastDelimiter = delim.previous
        } else {
            delim.next!!.previous = delim.previous
        }
    }

    private fun mergeChildTextNodes(node: Node) {
        // No children, no need for merging
        if (node.firstChild == null) {
            return
        }

        mergeTextNodesInclusive(node.firstChild, node.lastChild)
    }

    private fun mergeTextNodesInclusive(fromNode: Node?, toNode: Node?) {
        var first: Text? = null
        var last: Text? = null
        var length = 0

        var node = fromNode
        while (node != null) {
            if (node is Text) {
                val text = node
                if (first == null) {
                    first = text
                }
                length += text.literal.length
                last = text
            } else {
                mergeIfNeeded(first, last, length)
                first = null
                last = null
                length = 0

                mergeChildTextNodes(node)
            }
            if (node === toNode) {
                break
            }
            node = node.next
        }

        mergeIfNeeded(first, last, length)
    }

    private fun mergeIfNeeded(first: Text?, last: Text?, textLength: Int) {
        if (first != null && last != null && first !== last) {
            val sb = StringBuilder(textLength)
            sb.append(first.literal)
            var sourceSpans: SourceSpans? = null
            if (includeSourceSpans) {
                sourceSpans = SourceSpans()
                sourceSpans.addAll(first.getSourceSpans())
            }
            var node = first.next
            val stop = last.next
            while (node !== stop) {
                sb.append((node as Text).literal)
                sourceSpans?.addAll(node.getSourceSpans())

                val unlink = node
                node = node.next
                unlink.unlink()
            }
            val literal = sb.toString()
            first.literal = literal
            if (sourceSpans != null) {
                first.setSourceSpans(sourceSpans.getSourceSpans())
            }
        }
    }

    private class DelimiterData(
        val characters: MutableList<Text>,
        val canOpen: Boolean, val
        canClose: Boolean
    )

    /**
     * A destination and optional title for a link or image.
     */
    private class DestinationTitle(val destination: String?, val title: String?)

    private class LinkInfoImpl(
        private val marker: Text?,
        private val openingBracket: Text?,
        private val text: String,
        private val label: String?,
        private val destination: String?,
        private val title: String?,
        private val afterTextBracket: Position
    ) : LinkInfo {

        override fun marker(): Text? {
            return marker
        }

        override fun openingBracket(): Text? {
            return openingBracket
        }

        override fun text(): String {
            return text
        }

        override fun label(): String? {
            return label
        }

        override fun destination(): String? {
            return destination
        }

        override fun title(): String? {
            return title
        }

        override fun afterTextBracket(): Position {
            return afterTextBracket
        }
    }

    companion object {
        private fun calculateDelimiterProcessors(delimiterProcessors: List<DelimiterProcessor>): Map<Char, DelimiterProcessor> {
            val map = hashMapOf<Char, DelimiterProcessor>()
            addDelimiterProcessors(
                listOf(
                    AsteriskDelimiterProcessor(),
                    UnderscoreDelimiterProcessor()
                ), map
            )
            addDelimiterProcessors(delimiterProcessors, map)
            return map
        }

        private fun addDelimiterProcessors(
            delimiterProcessors: Iterable<DelimiterProcessor>,
            map: MutableMap<Char, DelimiterProcessor>
        ) {
            for (delimiterProcessor in delimiterProcessors) {
                val opening = delimiterProcessor.openingCharacter
                val closing = delimiterProcessor.closingCharacter
                if (opening == closing) {
                    val old = map[opening]
                    if (old != null && old.openingCharacter == old.closingCharacter) {
                        val s: StaggeredDelimiterProcessor?
                        if (old is StaggeredDelimiterProcessor) {
                            s = old
                        } else {
                            s = StaggeredDelimiterProcessor(opening)
                            s.add(old)
                        }
                        s.add(delimiterProcessor)
                        map.put(opening, s)
                    } else {
                        addDelimiterProcessorForChar(
                            opening,
                            delimiterProcessor,
                            map
                        )
                    }
                } else {
                    addDelimiterProcessorForChar(
                        opening,
                        delimiterProcessor,
                        map
                    )
                    addDelimiterProcessorForChar(
                        closing,
                        delimiterProcessor,
                        map
                    )
                }
            }
        }

        private fun addDelimiterProcessorForChar(
            delimiterChar: Char,
            toAdd: DelimiterProcessor,
            delimiterProcessors: MutableMap<Char, DelimiterProcessor>
        ) {
            val existing = delimiterProcessors.put(delimiterChar, toAdd)
            require(existing == null) { "Delimiter processor conflict with delimiter char '$delimiterChar'" }
        }

        private fun calculateLinkMarkers(linkMarkers: Set<Char>): BitSetWrapper {
            val bitSet = BitSetWrapper()
            for (c in linkMarkers) {
                bitSet.set(c.code)
            }
            bitSet.set('!'.code)
            return bitSet
        }

        private fun calculateSpecialCharacters(
            linkMarkers: BitSetWrapper,
            delimiterCharacters: Set<Char>,
            inlineContentParserFactories: List<InlineContentParserFactory>
        ): BitSetWrapper {
            val bitSet = linkMarkers.clone()
            for (c in delimiterCharacters) {
                bitSet.set(c.code)
            }
            for (factory in inlineContentParserFactories) {
                for (c in factory.triggerCharacters) {
                    bitSet.set(c.code)
                }
            }
            bitSet.set('['.code)
            bitSet.set(']'.code)
            bitSet.set('!'.code)
            bitSet.set('\n'.code)
            return bitSet
        }

        /**
         * Try to parse the destination and an optional title for an inline link/image.
         */
        private fun parseInlineDestinationTitle(scanner: Scanner): DestinationTitle? {
            if (!scanner.next('(')) {
                return null
            }

            scanner.whitespace()
            val dest = parseLinkDestination(scanner)
            if (dest == null) {
                return null
            }

            var title: String? = null
            val whitespace = scanner.whitespace()
            // title needs whitespace before
            if (whitespace >= 1) {
                title = parseLinkTitle(scanner)
                scanner.whitespace()
            }
            if (!scanner.next(')')) {
                // Don't have a closing `)`, so it's not a destination and title.
                // Note that something like `[foo](` could still be valid later, `(` will just be text.
                return null
            }
            return DestinationTitle(dest, title)
        }

        /**
         * Attempt to parse link destination, returning the string or null if no match.
         */
        private fun parseLinkDestination(scanner: Scanner): String? {
            val delimiter = scanner.peek()
            val start = scanner.position()
            if (!LinkScanner.scanLinkDestination(scanner)) {
                return null
            }

            val dest: String?
            if (delimiter == '<') {
                // chop off surrounding <..>:
                val rawDestination: String = scanner.getSource(start, scanner.position()).content
                dest = rawDestination.substring(1, rawDestination.length - 1)
            } else {
                dest = scanner.getSource(start, scanner.position()).content
            }

            return Escaping.unescapeString(dest)
        }

        /**
         * Attempt to parse link title (sans quotes), returning the string or null if no match.
         */
        private fun parseLinkTitle(scanner: Scanner): String? {
            val start = scanner.position()
            if (!LinkScanner.scanLinkTitle(scanner)) {
                return null
            }

            // chop off ', " or parens
            val rawTitle: String = scanner.getSource(start, scanner.position()).content
            val title = rawTitle.substring(1, rawTitle.length - 1)
            return Escaping.unescapeString(title)
        }

        /**
         * Attempt to parse a link label, returning the label between the brackets or null.
         */
        fun parseLinkLabel(scanner: Scanner): String? {
            if (!scanner.next('[')) {
                return null
            }

            val start = scanner.position()
            if (!LinkScanner.scanLinkLabelContent(scanner)) {
                return null
            }
            val end = scanner.position()

            if (!scanner.next(']')) {
                return null
            }

            val content: String = scanner.getSource(start, end).content
            // spec: A link label can have at most 999 characters inside the square brackets.
            if (content.length > 999) {
                return null
            }

            return content
        }
    }
}
