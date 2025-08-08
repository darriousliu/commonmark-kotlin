package org.commonmark.ext.front.matter

import org.commonmark.node.CustomNode

class YamlFrontMatterNode(var key: String, var values: List<String>) : CustomNode()
