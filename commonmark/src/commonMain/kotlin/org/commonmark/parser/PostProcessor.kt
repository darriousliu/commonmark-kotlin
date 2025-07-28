package org.commonmark.parser

import org.commonmark.node.Node

interface PostProcessor {
    /**
     * @param node the node to post-process
     * @return the result of post-processing, may be a modified `node` argument
     */
    fun process(node: Node): Node
}
