package org.commonmark.ext.gfm.strikethrough.internal

import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class StrikethroughNodeRenderer : NodeRenderer {
    override val nodeTypes: Set<KClass<out Node>>
        get() = setOf(Strikethrough::class)
}
