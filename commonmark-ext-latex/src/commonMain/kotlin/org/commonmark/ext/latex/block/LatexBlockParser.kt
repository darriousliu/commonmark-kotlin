package org.commonmark.ext.latex.block

import org.commonmark.parser.InlineParser
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.ParserState

class LatexBlockParser(
    private val blockType: LatexBlockType
) : AbstractBlockParser() {
    private var finished = false
    private val content = StringBuilder() // 用于存储块内容
    private var lineIndex = 0 // 记录当前处理的行数

    override val block = LatexBlock("")

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        if (finished) {
            return BlockContinue.none()
        }

        val line = parserState.line
        val lineContent = line.content.toString()

        // 只有在 lineIndex > 0 时才检查结束标记
        if (lineIndex > 0) {
            // 根据块类型检查结束标记
            val isEndMarker = when (blockType) {
                LatexBlockType.BACKSLASH_BRACKET -> lineContent.trimStart().startsWith("\\]")
                LatexBlockType.DOUBLE_DOLLAR -> lineContent.trimStart().startsWith("$$")
            }

            if (isEndMarker) {
                finished = true
                return BlockContinue.atIndex(parserState.index)
            }
        }
        return BlockContinue.atIndex(parserState.index)
    }

    override fun addLine(line: SourceLine) {
        if (finished) {
            return // 已经结束，不再添加内容
        }

        val lineStr = line.content.toString().trim()
        val endMarker = when (blockType) {
            LatexBlockType.BACKSLASH_BRACKET -> "\\]"
            LatexBlockType.DOUBLE_DOLLAR -> "$$"
        }

        if (lineIndex == 0) {
            // 第一行：处理开始标记后的内容
            val startMarker = when (blockType) {
                LatexBlockType.BACKSLASH_BRACKET -> "\\["
                LatexBlockType.DOUBLE_DOLLAR -> "$$"
            }
            val startIndex = lineStr.indexOf(startMarker)
            if (startIndex >= 0) {
                // 找到开始标记，添加开始标记后的内容
                // 如果第一行结尾有结束标记，则去除
                val hasEndMarker = lineStr.endsWith(endMarker) && lineStr.length > endMarker.length
                val afterStartMarker = lineStr.substring(
                    startIndex = startIndex + startMarker.length,
                    endIndex = if (hasEndMarker) lineStr.length - endMarker.length else lineStr.length
                )
                if (afterStartMarker.trim().isNotEmpty()) {
                    content.append(afterStartMarker)
                }
                finished = hasEndMarker // 如果第一行包含结束标记，则标记为结束
            }
        } else {
            // 非第一行：检查是否包含结束标记
            val trimmedLine = lineStr.trimStart()
            if (trimmedLine.startsWith(endMarker)) {
                // 这行包含结束标记
                val endIndex = lineStr.indexOf(endMarker)
                if (endIndex > 0) {
                    // 结束标记前有内容，添加这部分内容
                    val contentBeforeEnd = lineStr.substring(0, endIndex)
                    if (contentBeforeEnd.trim().isNotEmpty()) {
                        if (content.isNotEmpty()) {
                            content.append('\n')
                        }
                        content.append(contentBeforeEnd)
                    }
                }
                // 标记为结束
                finished = true
            } else {
                // 没有结束标记，添加整行内容
                if (content.isNotEmpty()) {
                    content.append('\n')
                }
                content.append(lineStr.trimEnd())
            }
        }
        lineIndex++
    }

    override fun closeBlock() {
        block.latex = content.toString()
        content.setLength(0)
    }


    override fun parseInlines(inlineParser: InlineParser) {
        // 块级 LaTeX 不需要进一步的内联解析
    }
}