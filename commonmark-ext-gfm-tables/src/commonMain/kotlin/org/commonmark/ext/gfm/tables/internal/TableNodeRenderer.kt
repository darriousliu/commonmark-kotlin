package org.commonmark.ext.gfm.tables.internal

import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class TableNodeRenderer : NodeRenderer {
    override val nodeTypes: Set<KClass<out Node>>
        get() = setOf(
            TableBlock::class,
            TableHead::class,
            TableBody::class,
            TableRow::class,
            TableCell::class
        )

    override fun render(node: Node) {
        when (node) {
            is TableBlock -> renderBlock(node)
            is TableHead -> renderHead(node)
            is TableBody -> renderBody(node)
            is TableRow -> renderRow(node)
            is TableCell -> renderCell(node)
        }
    }

    protected abstract fun renderBlock(node: TableBlock)

    protected abstract fun renderHead(node: TableHead)

    protected abstract fun renderBody(node: TableBody)

    protected abstract fun renderRow(node: TableRow)

    protected abstract fun renderCell(node: TableCell)
}
