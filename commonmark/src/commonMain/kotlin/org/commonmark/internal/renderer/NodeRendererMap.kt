package org.commonmark.internal.renderer

import org.commonmark.ext.putIfAbsent
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import kotlin.reflect.KClass

class NodeRendererMap {
    private val nodeRenderers = mutableListOf<NodeRenderer>()
    private val renderers = HashMap<KClass<out Node>, NodeRenderer>(64)

    /**
     * Set the renderer for each [NodeRenderer.nodeTypes], unless there was already a renderer set (first wins).
     */
    fun add(nodeRenderer: NodeRenderer) {
        nodeRenderers.add(nodeRenderer)
        for (nodeType in nodeRenderer.nodeTypes) {
            // The first node renderer for a node type "wins".
            renderers.putIfAbsent(nodeType, nodeRenderer)
        }
    }

    fun render(node: Node) {
        val nodeRenderer = renderers[node::class]
        nodeRenderer?.render(node)
    }

    fun beforeRoot(node: Node) {
        nodeRenderers.forEach { r -> r.beforeRoot(node) }
    }

    fun afterRoot(node: Node) {
        nodeRenderers.forEach { r -> r.afterRoot(node) }
    }
}
