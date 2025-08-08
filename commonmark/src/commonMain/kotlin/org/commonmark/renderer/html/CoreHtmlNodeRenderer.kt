package org.commonmark.renderer.html

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
import org.commonmark.node.ListBlock
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
open class CoreHtmlNodeRenderer(protected val context: HtmlNodeRendererContext) : AbstractVisitor(),
    NodeRenderer {
    private val html: HtmlWriter = context.writer

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

    override fun visit(heading: Heading) {
        val htag = "h" + heading.level
        html.line()
        html.tag(htag, getAttrs(heading, htag))
        visitChildren(heading)
        html.tag("/$htag")
        html.line()
    }

    override fun visit(paragraph: Paragraph) {
        val omitP = isInTightList(paragraph) ||  //
                (context.shouldOmitSingleParagraphP() && paragraph.getParent1() is Document &&  //
                        paragraph.previous == null && paragraph.next == null)
        if (!omitP) {
            html.line()
            html.tag("p", getAttrs(paragraph, "p"))
        }
        visitChildren(paragraph)
        if (!omitP) {
            html.tag("/p")
            html.line()
        }
    }

    override fun visit(blockQuote: BlockQuote) {
        html.line()
        html.tag("blockquote", getAttrs(blockQuote, "blockquote"))
        html.line()
        visitChildren(blockQuote)
        html.line()
        html.tag("/blockquote")
        html.line()
    }

    override fun visit(bulletList: BulletList) {
        renderListBlock(bulletList, "ul", getAttrs(bulletList, "ul"))
    }

    override fun visit(fencedCodeBlock: FencedCodeBlock) {
        val literal = fencedCodeBlock.literal
        val attributes = linkedMapOf<String, String?>()
        val info = fencedCodeBlock.info
        if (info != null && !info.isEmpty()) {
            val space = info.indexOf(" ")
            val language = if (space == -1) {
                info
            } else {
                info.substring(0, space)
            }
            attributes.put("class", "language-$language")
        }
        renderCodeBlock(literal.orEmpty(), fencedCodeBlock, attributes)
    }

    override fun visit(htmlBlock: HtmlBlock) {
        html.line()
        val literal = htmlBlock.literal.orEmpty()
        if (context.shouldEscapeHtml()) {
            html.tag("p", getAttrs(htmlBlock, "p"))
            html.text(literal)
            html.tag("/p")
        } else {
            html.raw(literal)
        }
        html.line()
    }

    override fun visit(thematicBreak: ThematicBreak) {
        html.line()
        html.tag("hr", getAttrs(thematicBreak, "hr"), true)
        html.line()
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock) {
        renderCodeBlock(indentedCodeBlock.literal, indentedCodeBlock, mutableMapOf())
    }

    override fun visit(link: Link) {
        val attrs = LinkedHashMap<String, String?>()
        var url = link.destination.orEmpty()

        if (context.shouldSanitizeUrls()) {
            url = context.urlSanitizer().sanitizeLinkUrl(url)
            attrs.put("rel", "nofollow")
        }

        url = context.encodeUrl(url)
        attrs.put("href", url)
        if (link.title != null) {
            attrs.put("title", link.title)
        }
        html.tag("a", getAttrs(link, "a", attrs))
        visitChildren(link)
        html.tag("/a")
    }

    override fun visit(listItem: ListItem) {
        html.tag("li", getAttrs(listItem, "li"))
        visitChildren(listItem)
        html.tag("/li")
        html.line()
    }

    override fun visit(orderedList: OrderedList) {
        val start = orderedList.markerStartNumber ?: 1
        val attrs = LinkedHashMap<String, String?>()
        if (start != 1) {
            attrs.put("start", start.toString())
        }
        renderListBlock(orderedList, "ol", getAttrs(orderedList, "ol", attrs))
    }

    override fun visit(image: Image) {
        var url = image.destination.orEmpty()

        val altTextVisitor = AltTextVisitor()
        image.accept(altTextVisitor)
        val altText = altTextVisitor.altText

        val attrs = linkedMapOf<String, String?>()
        if (context.shouldSanitizeUrls()) {
            url = context.urlSanitizer().sanitizeImageUrl(url)
        }

        attrs.put("src", context.encodeUrl(url))
        attrs.put("alt", altText)
        if (image.title != null) {
            attrs.put("title", image.title)
        }

        html.tag("img", getAttrs(image, "img", attrs), true)
    }

    override fun visit(emphasis: Emphasis) {
        html.tag("em", getAttrs(emphasis, "em"))
        visitChildren(emphasis)
        html.tag("/em")
    }

    override fun visit(strongEmphasis: StrongEmphasis) {
        html.tag("strong", getAttrs(strongEmphasis, "strong"))
        visitChildren(strongEmphasis)
        html.tag("/strong")
    }

    override fun visit(text: Text) {
        html.text(text.literal)
    }

    override fun visit(code: Code) {
        html.tag("code", getAttrs(code, "code"))
        html.text(code.literal)
        html.tag("/code")
    }

    override fun visit(htmlInline: HtmlInline) {
        if (context.shouldEscapeHtml()) {
            html.text(htmlInline.literal)
        } else {
            html.raw(htmlInline.literal)
        }
    }

    override fun visit(softLineBreak: SoftLineBreak) {
        html.raw(context.softbreak)
    }

    override fun visit(hardLineBreak: HardLineBreak) {
        html.tag("br", getAttrs(hardLineBreak, "br"), true)
        html.line()
    }

    override fun visitChildren(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            context.render(node)
            node = next
        }
    }

    private fun renderCodeBlock(
        literal: String,
        node: Node,
        attributes: MutableMap<String, String?>
    ) {
        html.line()
        html.tag("pre", getAttrs(node, "pre"))
        html.tag("code", getAttrs(node, "code", attributes))
        html.text(literal)
        html.tag("/code")
        html.tag("/pre")
        html.line()
    }

    private fun renderListBlock(
        listBlock: ListBlock,
        tagName: String,
        attributes: Map<String, String?>?
    ) {
        html.line()
        html.tag(tagName, attributes)
        html.line()
        visitChildren(listBlock)
        html.line()
        html.tag("/$tagName")
        html.line()
    }

    private fun isInTightList(paragraph: Paragraph): Boolean {
        val parent = paragraph.getParent1()
        if (parent != null) {
            val gramps = parent.getParent1()
            if (gramps is ListBlock) {
                val list = gramps
                return list.isTight
            }
        }
        return false
    }

    private fun getAttrs(node: Node, tagName: String): Map<String, String?> {
        return getAttrs(node, tagName, mutableMapOf())
    }

    private fun getAttrs(
        node: Node,
        tagName: String,
        defaultAttributes: MutableMap<String, String?>
    ): Map<String, String?> {
        return context.extendAttributes(node, tagName, defaultAttributes)
    }

    private class AltTextVisitor : AbstractVisitor() {
        private val sb = StringBuilder()

        val altText: String
            get() = sb.toString()

        override fun visit(text: Text) {
            sb.append(text.literal)
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            sb.append('\n')
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            sb.append('\n')
        }
    }
}
