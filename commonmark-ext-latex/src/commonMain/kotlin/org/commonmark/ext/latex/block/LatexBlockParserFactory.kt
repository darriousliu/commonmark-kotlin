package org.commonmark.ext.latex.block

import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState

class LatexBlockParserFactory : AbstractBlockParserFactory() {

    override fun tryStart(state: ParserState, matchedBlockParser: MatchedBlockParser): BlockStart? {
        val line = state.line.content
        val lineStr = line.toString().trimStart()

        // 检查是否是 LaTeX 块级公式的开始标记
        when {
            // \[ 格式的块级公式
            lineStr.startsWith("\\[") -> {
                val content = StringBuilder()
                val afterMarker = lineStr.substring(2).trimStart()
                if (afterMarker.isNotEmpty()) {
                    content.append(afterMarker)
                }
                return BlockStart.of(arrayOf(LatexBlockParser(LatexBlockType.BACKSLASH_BRACKET)))
                    .atIndex(state.index)
            }

            // $$ 格式的块级公式
            lineStr.startsWith("$$") -> {
                val content = StringBuilder()
                val afterMarker = lineStr.substring(2)
                if (afterMarker.isNotEmpty()) {
                    content.append(afterMarker)
                }
                return BlockStart.of(arrayOf(LatexBlockParser(LatexBlockType.DOUBLE_DOLLAR)))
                    .atIndex(state.index)
            }
        }

        return BlockStart.none()
    }
}

