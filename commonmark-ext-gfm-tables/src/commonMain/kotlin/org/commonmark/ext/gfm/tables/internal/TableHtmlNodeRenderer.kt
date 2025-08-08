package org.commonmark.ext.gfm.tables.internal

import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.node.Node
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlWriter

class TableHtmlNodeRenderer(private val context: HtmlNodeRendererContext) : TableNodeRenderer() {
    private val htmlWriter: HtmlWriter = context.writer

    override fun renderBlock(node: TableBlock) {
        htmlWriter.line()
        htmlWriter.tag("table", getAttributes(node, "table"))
        renderChildren(node)
        htmlWriter.tag("/table")
        htmlWriter.line()
    }

    override fun renderHead(node: TableHead) {
        htmlWriter.line()
        htmlWriter.tag("thead", getAttributes(node, "thead"))
        renderChildren(node)
        htmlWriter.tag("/thead")
        htmlWriter.line()
    }

    override fun renderBody(node: TableBody) {
        htmlWriter.line()
        htmlWriter.tag("tbody", getAttributes(node, "tbody"))
        renderChildren(node)
        htmlWriter.tag("/tbody")
        htmlWriter.line()
    }

    override fun renderRow(node: TableRow) {
        htmlWriter.line()
        htmlWriter.tag("tr", getAttributes(node, "tr"))
        renderChildren(node)
        htmlWriter.tag("/tr")
        htmlWriter.line()
    }

    override fun renderCell(node: TableCell) {
        val tagName = if (node.isHeader) "th" else "td"
        htmlWriter.line()
        htmlWriter.tag(tagName, getCellAttributes(node, tagName))
        renderChildren(node)
        htmlWriter.tag("/$tagName")
        htmlWriter.line()
    }

    private fun getAttributes(node: Node, tagName: String): Map<String, String?> {
        return context.extendAttributes(node, tagName, mutableMapOf())
    }

    private fun getCellAttributes(tableCell: TableCell, tagName: String): Map<String, String?> {
        return if (tableCell.alignment != null) {
            context.extendAttributes(
                tableCell,
                tagName,
                mutableMapOf("align" to getAlignValue(tableCell.alignment))
            )
        } else {
            context.extendAttributes(tableCell, tagName, mutableMapOf())
        }
    }

    private fun renderChildren(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            context.render(node)
            node = next
        }
    }

    companion object {
        private fun getAlignValue(alignment: TableCell.Alignment?): String {
            return when (alignment) {
                TableCell.Alignment.LEFT -> "left"
                TableCell.Alignment.CENTER -> "center"
                TableCell.Alignment.RIGHT -> "right"
                else -> throw IllegalStateException("Unknown alignment: $alignment")
            }
        }
    }
}
