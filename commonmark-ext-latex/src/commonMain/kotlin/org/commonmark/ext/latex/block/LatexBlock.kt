package org.commonmark.ext.latex.block

import org.commonmark.node.CustomBlock

class LatexBlock(
    var latex: String,
) : CustomBlock()