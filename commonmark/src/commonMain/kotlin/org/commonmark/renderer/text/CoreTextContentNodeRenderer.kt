package org.commonmark.renderer.text

import org.commonmark.internal.renderer.text.BulletListHolder
import org.commonmark.internal.renderer.text.ListHolder
import org.commonmark.internal.renderer.text.OrderedListHolder
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
import kotlin.reflect.KClass

/**
 * The node renderer that renders all the core nodes (comes last in the order of node renderers).
 */
open class CoreTextContentNodeRenderer(protected val context: TextContentNodeRendererContext) :
    AbstractVisitor(), NodeRenderer {
    private val textContent: TextContentWriter = context.writer

    private var listHolder: ListHolder? = null

    override val nodeTypes: Set<KClass<out Node>> = setOf(
        Document::class,
        Heading::class,
        Paragraph::class,
        BlockQuote::class,
        BulletList::class,
        FencedCodeBlock::class,
        HtmlBlock::class,
        ThematicBreak::class,
        IndentedCodeBlock::class,
        Link::class,
        ListItem::class,
        OrderedList::class,
        Image::class,
        Emphasis::class,
        StrongEmphasis::class,
        Text::class,
        Code::class,
        HtmlInline::class,
        SoftLineBreak::class,
        HardLineBreak::class
    )

    override fun render(node: Node) {
        node.accept(this)
    }

    override fun visit(document: Document) {
        // No rendering itself
        visitChildren(document)
    }

    override fun visit(blockQuote: BlockQuote) {
        // LEFT-POINTING DOUBLE ANGLE QUOTATION MARK
        textContent.write('\u00AB')
        visitChildren(blockQuote)
        textContent.resetBlock()
        // RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK
        textContent.write('\u00BB')

        textContent.block()
    }

    override fun visit(bulletList: BulletList) {
        textContent.pushTight(bulletList.isTight)
        val listHolder = BulletListHolder(listHolder, bulletList).also { listHolder = it }
        visitChildren(bulletList)
        textContent.popTight()
        textContent.block()
        this.listHolder = listHolder.parent
    }

    override fun visit(code: Code) {
        textContent.write('\"')
        textContent.write(code.literal)
        textContent.write('\"')
    }

    override fun visit(fencedCodeBlock: FencedCodeBlock) {
        val literal = stripTrailingNewline(fencedCodeBlock.literal.orEmpty())
        if (stripNewlines()) {
            textContent.writeStripped(literal)
        } else {
            textContent.write(literal)
        }
        textContent.block()
    }

    override fun visit(hardLineBreak: HardLineBreak) {
        if (stripNewlines()) {
            textContent.whitespace()
        } else {
            textContent.line()
        }
    }

    override fun visit(heading: Heading) {
        visitChildren(heading)
        if (stripNewlines()) {
            textContent.write(": ")
        } else {
            textContent.block()
        }
    }

    override fun visit(thematicBreak: ThematicBreak) {
        if (!stripNewlines()) {
            textContent.write("***")
        }
        textContent.block()
    }

    override fun visit(htmlInline: HtmlInline) {
        writeText(htmlInline.literal)
    }

    override fun visit(htmlBlock: HtmlBlock) {
        writeText(htmlBlock.literal.orEmpty())
    }

    override fun visit(image: Image) {
        writeLink(image, image.title, image.destination)
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock) {
        val literal = stripTrailingNewline(indentedCodeBlock.literal)
        if (stripNewlines()) {
            textContent.writeStripped(literal)
        } else {
            textContent.write(literal)
        }
        textContent.block()
    }

    override fun visit(link: Link) {
        writeLink(link, link.title, link.destination)
    }

    override fun visit(listItem: ListItem) {
        if (listHolder != null && listHolder is OrderedListHolder) {
            val orderedListHolder = listHolder as OrderedListHolder
            val indent = if (stripNewlines()) "" else orderedListHolder.indent
            textContent.write((indent + orderedListHolder.counter + orderedListHolder.delimiter) + " ")
            visitChildren(listItem)
            textContent.block()
            orderedListHolder.increaseCounter()
        } else if (listHolder != null && listHolder is BulletListHolder) {
            val bulletListHolder: BulletListHolder = listHolder as BulletListHolder
            if (!stripNewlines()) {
                textContent.write((bulletListHolder.indent + bulletListHolder.marker) + " ")
            }
            visitChildren(listItem)
            textContent.block()
        }
    }

    override fun visit(orderedList: OrderedList) {
        textContent.pushTight(orderedList.isTight)
        val listHolder = OrderedListHolder(listHolder, orderedList).also { listHolder = it }
        visitChildren(orderedList)
        textContent.popTight()
        textContent.block()
        this.listHolder = listHolder.parent
    }

    override fun visit(paragraph: Paragraph) {
        visitChildren(paragraph)
        textContent.block()
    }

    override fun visit(softLineBreak: SoftLineBreak) {
        if (stripNewlines()) {
            textContent.whitespace()
        } else {
            textContent.line()
        }
    }

    override fun visit(text: Text) {
        writeText(text.literal)
    }

    override fun visitChildren(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            context.render(node)
            node = next
        }
    }

    private fun writeText(text: String) {
        if (stripNewlines()) {
            textContent.writeStripped(text)
        } else {
            textContent.write(text)
        }
    }

    private fun writeLink(node: Node, title: String?, destination: String?) {
        val hasChild = node.firstChild != null
        val hasTitle = title != null && title != destination
        val hasDestination = destination != null && destination != ""

        if (hasChild) {
            textContent.write('"')
            visitChildren(node)
            textContent.write('"')
            if (hasTitle || hasDestination) {
                textContent.whitespace()
                textContent.write('(')
            }
        }

        if (hasTitle) {
            textContent.write(title)
            if (hasDestination) {
                textContent.colon()
                textContent.whitespace()
            }
        }

        if (hasDestination) {
            textContent.write(destination)
        }

        if (hasChild && (hasTitle || hasDestination)) {
            textContent.write(')')
        }
    }

    private fun stripNewlines(): Boolean {
        return context.lineBreakRendering() === LineBreakRendering.STRIP
    }

    companion object {
        private fun stripTrailingNewline(s: String): String {
            return if (s.endsWith("\n")) {
                s.substring(0, s.length - 1)
            } else {
                s
            }
        }
    }
}
