package org.commonmark.renderer.markdown

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.renderer.NodeRenderer
import org.commonmark.text.AsciiMatcher
import org.commonmark.text.CharMatcher
import org.commonmark.text.Characters
import kotlin.math.max
import kotlin.reflect.KClass

/**
 * The node renderer that renders all the core nodes (comes last in the order of node renderers).
 *
 *
 * Note that while sometimes it would be easier to record what kind of syntax was used on parsing (e.g. ATX vs Setext
 * heading), this renderer is intended to also work for documents that were created by directly creating
 * [Nodes][Node] instead. So in order to support that, it sometimes needs to do a bit more work.
 */
open class CoreMarkdownNodeRenderer(
    protected val context: MarkdownNodeRendererContext
) : AbstractVisitor(), NodeRenderer {
    private val textEscape: AsciiMatcher =
        AsciiMatcher.builder().anyOf("[]<>`*_&\n\\").anyOf(context.specialCharacters).build()
    private val textEscapeInHeading: CharMatcher =
        AsciiMatcher.builder(textEscape).anyOf("#").build()
    private val linkDestinationNeedsAngleBrackets: CharMatcher =
        AsciiMatcher.builder().c(' ').c('(').c(')').c('<').c('>').c('\n').c('\\').build()
    private val linkDestinationEscapeInAngleBrackets: CharMatcher? =
        AsciiMatcher.builder().c('<').c('>').c('\n').c('\\').build()
    private val linkTitleEscapeInQuotes: CharMatcher? =
        AsciiMatcher.builder().c('"').c('\n').c('\\').build()

    private val orderedListMarkerPattern = Regex("^([0-9]{1,9})([.)])")

    private val writer: MarkdownWriter = context.writer

    /**
     * If we're currently within a [BulletList] or [OrderedList], this keeps the context of that list.
     * It has a parent field so that it can represent a stack (for nested lists).
     */
    private var listHolder: ListHolder? = null

    override val nodeTypes: Set<KClass<out Node>> = setOf(
        BlockQuote::class,
        BulletList::class,
        Code::class,
        Document::class,
        Emphasis::class,
        FencedCodeBlock::class,
        HardLineBreak::class,
        Heading::class,
        HtmlBlock::class,
        HtmlInline::class,
        Image::class,
        IndentedCodeBlock::class,
        Link::class,
        ListItem::class,
        OrderedList::class,
        Paragraph::class,
        SoftLineBreak::class,
        StrongEmphasis::class,
        Text::class,
        ThematicBreak::class
    )

    override fun render(node: Node) {
        node.accept(this)
    }

    override fun visit(document: Document) {
        // No rendering itself
        visitChildren(document)
        writer.line()
    }

    override fun visit(thematicBreak: ThematicBreak) {
        var literal = thematicBreak.literal
        if (literal == null) {
            // Let's use ___ as it doesn't introduce ambiguity with * or - list item markers
            literal = "___"
        }
        writer.raw(literal)
        writer.block()
    }

    override fun visit(heading: Heading) {
        if (heading.level <= 2) {
            val lineBreakVisitor = LineBreakVisitor()
            heading.accept(lineBreakVisitor)
            val isMultipleLines = lineBreakVisitor.hasLineBreak()

            if (isMultipleLines) {
                // Setext headings: Can have multiple lines, but only level 1 or 2
                visitChildren(heading)
                writer.line()
                if (heading.level == 1) {
                    // Note that it would be nice to match the length of the contents instead of just using 3, but that's
                    // not easy.
                    writer.raw("===")
                } else {
                    writer.raw("---")
                }
                writer.block()
                return
            }
        }

        // ATX headings: Can't have multiple lines, but up to level 6.
        repeat(heading.level) {
            writer.raw('#')
        }
        writer.raw(' ')
        visitChildren(heading)

        writer.block()
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock) {
        val literal = indentedCodeBlock.literal
        // We need to respect line prefixes, which is why we need to write it line by line (e.g. an indented code block
        // within a block quote)
        writer.writePrefix("    ")
        writer.pushPrefix("    ")
        val lines = getLines(literal)
        for (i in 0..<lines.size) {
            val line = lines[i]
            writer.raw(line)
            if (i != lines.size - 1) {
                writer.line()
            }
        }
        writer.popPrefix()
        writer.block()
    }

    override fun visit(fencedCodeBlock: FencedCodeBlock) {
        val literal = fencedCodeBlock.literal.orEmpty()
        val fenceChar = fencedCodeBlock.fenceCharacter ?: "`"
        // If we have a known fence length, use it
        val openingFenceLength = fencedCodeBlock.getOpeningFenceLength() ?: run {
            // Otherwise, calculate the closing fence length pessimistically, e.g., if the code block itself contains a
            // line with ```, we need to use a fence of length 4. If ``` occurs with non-whitespace characters on a
            // line, we technically don't need a longer fence, but it's not incorrect to do so.
            val fenceCharsInLiteral = findMaxRunLength(fenceChar, literal)
            max(fenceCharsInLiteral + 1, 3)
        }

        val closingFenceLength = fencedCodeBlock.getClosingFenceLength() ?: openingFenceLength

        val openingFence = repeat(fenceChar, openingFenceLength)
        val closingFence = repeat(fenceChar, closingFenceLength)
        val indent: Int = fencedCodeBlock.fenceIndent

        if (indent > 0) {
            val indentPrefix = repeat(" ", indent)
            writer.writePrefix(indentPrefix)
            writer.pushPrefix(indentPrefix)
        }

        writer.raw(openingFence)
        val info = fencedCodeBlock.info
        if (info != null) {
            writer.raw(info)
        }
        writer.line()
        if (!literal.isEmpty()) {
            val lines = getLines(literal)
            for (line in lines) {
                writer.raw(line)
                writer.line()
            }
        }
        writer.raw(closingFence)
        if (indent > 0) {
            writer.popPrefix()
        }
        writer.block()
    }

    override fun visit(htmlBlock: HtmlBlock) {
        val lines = getLines(htmlBlock.literal.orEmpty())
        for (i in 0..<lines.size) {
            val line = lines[i]
            writer.raw(line)
            if (i != lines.size - 1) {
                writer.line()
            }
        }
        writer.block()
    }

    override fun visit(paragraph: Paragraph) {
        visitChildren(paragraph)
        writer.block()
    }

    override fun visit(blockQuote: BlockQuote) {
        writer.writePrefix("> ")
        writer.pushPrefix("> ")
        visitChildren(blockQuote)
        writer.popPrefix()
        writer.block()
    }

    override fun visit(bulletList: BulletList) {
        writer.pushTight(bulletList.isTight)
        val listHolder = BulletListHolder(listHolder, bulletList).also { listHolder = it }
        visitChildren(bulletList)
        this.listHolder = listHolder.parent
        writer.popTight()
        writer.block()
    }

    override fun visit(orderedList: OrderedList) {
        writer.pushTight(orderedList.isTight)
        val listHolder = OrderedListHolder(listHolder, orderedList).also { listHolder = it }
        visitChildren(orderedList)
        this.listHolder = listHolder.parent
        writer.popTight()
        writer.block()
    }

    override fun visit(listItem: ListItem) {
        val markerIndent = listItem.getMarkerIndent() ?: 0
        val marker: String
        when (listHolder) {
            is BulletListHolder -> {
                val bulletListHolder = listHolder as BulletListHolder
                marker = repeat(
                    " ",
                    markerIndent
                ) + bulletListHolder.marker
            }

            is OrderedListHolder -> {
                val orderedListHolder = listHolder as OrderedListHolder
                marker = repeat(
                    " ",
                    markerIndent
                ) + orderedListHolder.number + orderedListHolder.delimiter
                orderedListHolder.number++
            }

            else -> {
                throw IllegalStateException("Unknown list holder type: $listHolder")
            }
        }
        val contentIndent = listItem.getContentIndent()
        val spaces =
            if (contentIndent != null) repeat(
                " ",
                max(contentIndent - marker.length, 1)
            ) else " "
        writer.writePrefix(marker)
        writer.writePrefix(spaces)
        writer.pushPrefix(
            repeat(
                " ",
                marker.length + spaces.length
            )
        )

        if (listItem.firstChild == null) {
            // Empty list item
            writer.block()
        } else {
            visitChildren(listItem)
        }

        writer.popPrefix()
    }

    override fun visit(code: Code) {
        val literal = code.literal
        // If the literal includes backticks, we can surround them by using one more backtick.
        val backticks: Int =
            findMaxRunLength("`", literal)
        repeat(backticks + 1) {
            writer.raw('`')
        }
        // If the literal starts or ends with a backtick, surround it with a single space.
        // If it starts and ends with a space (but is not only spaces), add another space (otherwise they would
        // get removed on parsing).
        val addSpace = literal.startsWith("`") || literal.endsWith("`") ||
                (literal.startsWith(" ") && literal.endsWith(" ") && Characters.hasNonSpace(literal))
        if (addSpace) {
            writer.raw(' ')
        }
        writer.raw(literal)
        if (addSpace) {
            writer.raw(' ')
        }
        repeat(backticks + 1) {
            writer.raw('`')
        }
    }

    override fun visit(emphasis: Emphasis) {
        var delimiter = emphasis.openingDelimiter
        // Use delimiter that was parsed if available
        if (delimiter == null) {
            // When emphasis is nested, a different delimiter needs to be used
            delimiter = if (writer.lastChar == '*') "_" else "*"
        }
        writer.raw(delimiter)
        super.visit(emphasis)
        writer.raw(delimiter)
    }

    override fun visit(strongEmphasis: StrongEmphasis) {
        writer.raw("**")
        super.visit(strongEmphasis)
        writer.raw("**")
    }

    override fun visit(link: Link) {
        writeLinkLike(link.title, link.destination.orEmpty(), link, "[")
    }

    override fun visit(image: Image) {
        writeLinkLike(image.title, image.destination.orEmpty(), image, "![")
    }

    override fun visit(htmlInline: HtmlInline) {
        writer.raw(htmlInline.literal)
    }

    override fun visit(hardLineBreak: HardLineBreak) {
        writer.raw("  ")
        writer.line()
    }

    override fun visit(softLineBreak: SoftLineBreak) {
        writer.line()
    }

    override fun visit(text: Text) {
        // Text is tricky. In Markdown special characters (`-`, `#` etc.) can be escaped (`\-`, `\#` etc.) so that
        // they're parsed as plain text. Currently, whether a character was escaped or not is not recorded in the Node,
        // so here we don't know. If we just wrote out those characters unescaped, the resulting Markdown would change
        // meaning (turn into a list item, heading, etc.).
        // You might say, "Why not store that in the Node when parsing", but that wouldn't work for the use case where
        // nodes are constructed directly instead of via parsing. This renderer needs to work for that too.
        // So currently, when in doubt, we escape. For special characters only occurring at the beginning of a line,
        // we only escape them then (we wouldn't want to escape every `.` for example).
        var literal = text.literal
        if (writer.isAtLineStart && !literal.isEmpty()) {
            val c = literal[0]
            when (c) {
                '-' -> {
                    // Would be ambiguous with a bullet list marker, escape
                    writer.raw("\\-")
                    literal = literal.substring(1)
                }

                '#' -> {
                    // Would be ambiguous with an ATX heading, escape
                    writer.raw("\\#")
                    literal = literal.substring(1)
                }

                '=' -> {
                    // Would be ambiguous with a Setext heading, escape unless it's the first line in the block
                    if (text.previous != null) {
                        writer.raw("\\=")
                        literal = literal.substring(1)
                    }
                }

                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    // Check for ordered list marker
                    val m = orderedListMarkerPattern.find(literal)
                    if (m != null) {
                        writer.raw(m.groupValues[1])
                        writer.raw("\\" + m.groupValues[2])
                        literal = literal.substring(m.range.last + 1)
                    }
                }

                '\t' -> {
                    writer.raw("&#9;")
                    literal = literal.substring(1)
                }

                ' ' -> {
                    writer.raw("&#32;")
                    literal = literal.substring(1)
                }
            }
        }

        val escape = if (text.getParent1() is Heading) textEscapeInHeading else textEscape

        if (literal.endsWith("!") && text.next is Link) {
            // If we wrote the `!` unescaped, it would turn the link into an image instead.
            writer.text(literal.substring(0, literal.length - 1), escape)
            writer.raw("\\!")
        } else {
            writer.text(literal, escape)
        }
    }

    override fun visitChildren(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            context.render(node)
            node = next
        }
    }

    private fun writeLinkLike(title: String?, destination: String, node: Node, opener: String) {
        writer.raw(opener)
        visitChildren(node)
        writer.raw(']')
        writer.raw('(')
        if (contains(
                destination,
                linkDestinationNeedsAngleBrackets
            )
        ) {
            writer.raw('<')
            writer.text(destination, linkDestinationEscapeInAngleBrackets)
            writer.raw('>')
        } else {
            writer.raw(destination)
        }
        if (title != null) {
            writer.raw(' ')
            writer.raw('"')
            writer.text(title, linkTitleEscapeInQuotes)
            writer.raw('"')
        }
        writer.raw(')')
    }

    private open class ListHolder protected constructor(val parent: ListHolder?)

    private class BulletListHolder(parent: ListHolder?, bulletList: BulletList) :
        ListHolder(parent) {
        val marker: String = bulletList.marker ?: "-"
    }

    private class OrderedListHolder(parent: ListHolder?, orderedList: OrderedList) :
        ListHolder(parent) {
        val delimiter: String = orderedList.markerDelimiter ?: "."
        var number: Int = orderedList.markerStartNumber ?: 1
    }

    /**
     * Visits nodes to check if there are any soft or hard line breaks.
     */
    private class LineBreakVisitor : AbstractVisitor() {
        private var lineBreak = false

        fun hasLineBreak(): Boolean {
            return lineBreak
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            super.visit(softLineBreak)
            lineBreak = true
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            super.visit(hardLineBreak)
            lineBreak = true
        }
    }

    companion object {
        private fun findMaxRunLength(needle: String, s: String): Int {
            var maxRunLength = 0
            var pos = 0
            while (pos < s.length) {
                pos = s.indexOf(needle, pos)
                if (pos == -1) {
                    return maxRunLength
                }
                var runLength = 0
                do {
                    pos += needle.length
                    runLength++
                } while (s.startsWith(needle, pos))
                maxRunLength = max(runLength, maxRunLength)
            }
            return maxRunLength
        }

        private fun contains(s: String, charMatcher: CharMatcher): Boolean {
            for (i in 0..<s.length) {
                if (charMatcher.matches(s[i])) {
                    return true
                }
            }
            return false
        }

        // Keep for Android compat (String.repeat only available on Android 12 and later)
        private fun repeat(s: String, count: Int): String {
            val sb = StringBuilder(s.length * count)
            repeat(count) {
                sb.append(s)
            }
            return sb.toString()
        }

        private fun getLines(literal: String): List<String> {
            // Without -1, split would discard all trailing empty strings, which is not what we want, e.g. it would
            // return the same result for "abc", "abc\n" and "abc\n\n".
            // With -1, it returns ["abc"], ["abc", ""] and ["abc", "", ""].
            val parts = literal.split("\n", limit = -1)
            return if (parts[parts.size - 1].isEmpty()) {
                // But we don't want the last empty string, as "\n" is used as a line terminator (not a separator),
                // so return without the last element.
                parts.subList(0, parts.size - 1)
            } else {
                parts
            }
        }
    }
}
