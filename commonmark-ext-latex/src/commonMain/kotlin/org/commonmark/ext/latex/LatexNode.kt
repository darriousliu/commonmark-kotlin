package org.commonmark.ext.latex

import org.commonmark.node.CustomNode
import org.commonmark.node.Delimited

class LatexNode(
    val latex: String,
    override val openingDelimiter: String = "\\(",
    override val closingDelimiter: String = "\\)"
) : CustomNode(), Delimited