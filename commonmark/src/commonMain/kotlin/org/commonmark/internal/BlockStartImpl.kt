package org.commonmark.internal

import org.commonmark.parser.block.BlockParser
import org.commonmark.parser.block.BlockStart

class BlockStartImpl(private val blockParsers: Array<BlockParser>) : BlockStart() {
    var newIndex: Int = -1
        private set
    var newColumn: Int = -1
        private set
    var isReplaceActiveBlockParser: Boolean = false
        private set
    var replaceParagraphLines: Int = 0
        private set

    fun getBlockParsers() = blockParsers

    override fun atIndex(newIndex: Int): BlockStart {
        this.newIndex = newIndex
        return this
    }

    override fun atColumn(newColumn: Int): BlockStart {
        this.newColumn = newColumn
        return this
    }

    @Deprecated("use {@link #replaceParagraphLines(int)} instead; please raise an issue if that doesn't work for you\n      for some reason.")
    override fun replaceActiveBlockParser(): BlockStart {
        this.isReplaceActiveBlockParser = true
        return this
    }

    override fun replaceParagraphLines(lines: Int): BlockStart {
        require(lines >= 1) { "Lines must be >= 1" }
        this.replaceParagraphLines = lines
        return this
    }
}
