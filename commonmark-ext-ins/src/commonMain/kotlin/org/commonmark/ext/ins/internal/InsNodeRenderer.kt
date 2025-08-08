package org.commonmark.ext.ins.internal

import org.commonmark.ext.ins.Ins
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class InsNodeRenderer : NodeRenderer {
    override val nodeTypes: Set<KClass<out Node>>
        get() = setOf(Ins::class)
}
