package org.commonmark.internal.inline

import org.commonmark.node.Node
import org.commonmark.parser.beta.ParsedInline
import org.commonmark.parser.beta.Position

class ParsedInlineImpl(
    val node: Node,
    val position: Position
) : ParsedInline
