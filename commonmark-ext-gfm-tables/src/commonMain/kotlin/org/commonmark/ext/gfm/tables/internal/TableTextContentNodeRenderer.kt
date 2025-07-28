package org.commonmark.ext.gfm.tables.internal

import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.node.Node
import org.commonmark.renderer.text.TextContentNodeRendererContext
import org.commonmark.renderer.text.TextContentWriter

/**
 * The Table node renderer that is needed for rendering GFM tables (GitHub Flavored Markdown) to text content.
 */
class TableTextContentNodeRenderer(private val context: TextContentNodeRendererContext) :
    TableNodeRenderer() {
    private val textContentWriter: TextContentWriter = context.writer

    override fun renderBlock(node: TableBlock) {
        // Render rows tight
        textContentWriter.pushTight(true)
        renderChildren(node)
        textContentWriter.popTight()
        textContentWriter.block()
    }

    override fun renderHead(node: TableHead) {
        renderChildren(node)
    }

    override fun renderBody(node: TableBody) {
        renderChildren(node)
    }

    override fun renderRow(node: TableRow) {
        renderChildren(node)
        textContentWriter.block()
    }

    override fun renderCell(node: TableCell) {
        renderChildren(node)
        // For the last cell in row, don't render the delimiter
        if (node.next != null) {
            textContentWriter.write('|')
            textContentWriter.whitespace()
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
}
