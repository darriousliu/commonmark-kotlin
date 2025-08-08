package org.commonmark.ext.front.matter

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.CustomNode

class YamlFrontMatterVisitor : AbstractVisitor() {
    val data = LinkedHashMap<String?, List<String>>()

    override fun visit(customNode: CustomNode) {
        if (customNode is YamlFrontMatterNode) {
            data.put(customNode.key, customNode.values)
        } else {
            super.visit(customNode)
        }
    }
}
