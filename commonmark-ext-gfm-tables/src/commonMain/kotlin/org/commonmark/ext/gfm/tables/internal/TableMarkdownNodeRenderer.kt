package org.commonmark.ext.gfm.tables.internal

import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.node.Node
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownWriter
import org.commonmark.text.AsciiMatcher

/**
 * The Table node renderer that is needed for rendering GFM tables (GitHub Flavored Markdown) to text content.
 */
class TableMarkdownNodeRenderer(private val context: MarkdownNodeRendererContext) :
    TableNodeRenderer() {
    private val writer: MarkdownWriter = context.writer

    private val pipe: AsciiMatcher = AsciiMatcher.builder().c('|').build()

    private val columns: MutableList<TableCell.Alignment?> = mutableListOf()

    override fun renderBlock(node: TableBlock) {
        columns.clear()
        writer.pushTight(true)
        renderChildren(node)
        writer.popTight()
        writer.block()
    }

    override fun renderHead(node: TableHead) {
        renderChildren(node)
        for (columnAlignment in columns) {
            writer.raw('|')
            when (columnAlignment) {
                TableCell.Alignment.LEFT -> writer.raw(":---")
                TableCell.Alignment.RIGHT -> writer.raw("---:")
                TableCell.Alignment.CENTER -> writer.raw(":---:")
                else -> writer.raw("---")
            }
        }
        writer.raw("|")
        writer.block()
    }

    override fun renderBody(node: TableBody) {
        renderChildren(node)
    }

    override fun renderRow(node: TableRow) {
        renderChildren(node)
        // Trailing | at the end of the line
        writer.raw("|")
        writer.block()
    }

    override fun renderCell(node: TableCell) {
        if (node.parent() != null && node.parent()?.parent() is TableHead) {
            columns.add(node.alignment)
        }
        writer.raw("|")
        writer.pushRawEscape(pipe)
        renderChildren(node)
        writer.popRawEscape()
    }

    private fun renderChildren(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            context.render(node)
            node = next
        }
    }
}
