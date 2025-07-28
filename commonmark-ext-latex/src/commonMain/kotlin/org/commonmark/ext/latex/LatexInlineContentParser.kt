package org.commonmark.ext.latex

import org.commonmark.parser.beta.*

class LatexInlineContentParser : InlineContentParser {
    companion object {
        private const val DOLLAR_CHAR = '$'
        private const val BACKSLASH_CHAR = '\\'
    }

    override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
        val scanner = inlineParserState.scanner()
        val start = scanner.position()
        val currentChar = scanner.peek()


        return when (currentChar) {
            DOLLAR_CHAR -> parseDollarDelimitedLatex(scanner)
            BACKSLASH_CHAR -> parseBackslashDelimitedLatex(scanner, start)
            else -> ParsedInline.none()
        }
    }

    private fun parseDollarDelimitedLatex(scanner: Scanner): ParsedInline? {
        val firstDollar = scanner.position()
        scanner.next() // 消费第一个 $

        // 检查是否是双美元符号 $$，如果是则回退（让 LatexBlock 处理）
        if (scanner.peek() == DOLLAR_CHAR) {
            scanner.setPosition(firstDollar)
            return ParsedInline.none()
        }

        val latexContent = StringBuilder()
        while (scanner.hasNext()) {
            val c = scanner.peek()
            if (c == DOLLAR_CHAR) {
                // 对于内联，一个 $ 就够了
                scanner.next() // 消费结束的 $
                return ParsedInline.of(
                    LatexNode(
                        latexContent.toString().trim(),
                        DOLLAR_CHAR.toString(),
                        DOLLAR_CHAR.toString()
                    ),
                    scanner.position()
                )
            } else if (c == BACKSLASH_CHAR) {
                // 处理转义字符
                scanner.next() // 消费反斜杠
                latexContent.append(BACKSLASH_CHAR)
                if (scanner.hasNext()) {
                    val nextChar = scanner.peek()
                    latexContent.append(nextChar)
                    scanner.next()
                }
            } else {
                latexContent.append(c)
                scanner.next() // 消费当前字符
            }
        }
        // 没有找到结束分隔符，返回原始文本
        scanner.setPosition(firstDollar)
        return ParsedInline.none()
    }

    private fun parseBackslashDelimitedLatex(scanner: Scanner, start: Position): ParsedInline? {
        val originalPosition = scanner.position()
        scanner.next() // 消费反斜杠
        when (scanner.peek()) {
            '(' -> {
                scanner.next() // 消费 (
                return parseUntilClosing(scanner, originalPosition)
            }

            '[' -> {
                // \[...\] 格式回退，让 LatexBlock 处理
                scanner.setPosition(originalPosition)
                return ParsedInline.none()
            }

            else -> {
                // 不是 LaTeX 分隔符，回退
                scanner.setPosition(start)
                return ParsedInline.none()
            }
        }
    }

    private fun parseUntilClosing(
        scanner: Scanner,
        originalPosition: Position,
    ): ParsedInline? {
        val closingChar = ')'
        val latexContent = StringBuilder()

        while (scanner.hasNext()) {
            val c = scanner.peek()
            if (c == BACKSLASH_CHAR) {
                // 检查是否是结束分隔符 \)
                val currentPos = scanner.position()

                scanner.next() // 消费反斜杠
                if (scanner.hasNext() && scanner.peek() == closingChar) {
                    // 找到结束分隔符 \)
                    scanner.next() // 消费结束分隔符
                    return ParsedInline.of(
                        LatexNode(
                            latexContent.toString().trim(),
                            BACKSLASH_CHAR.toString(),
                            BACKSLASH_CHAR.toString()
                        ),
                        scanner.position()
                    )
                } else {
                    // 不是结束分隔符，这是LaTeX命令的开始
                    // 回退到反斜杠位置，然后正常处理
                    scanner.setPosition(currentPos)
                    latexContent.append(c)
                    scanner.next()
                }
            } else {
                latexContent.append(c)
                scanner.next()
            }
        }

        // 没有找到匹配的结束符，返回原始文本
        scanner.setPosition(originalPosition)
        return ParsedInline.none()
    }


    class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char>
            get() = setOf(BACKSLASH_CHAR, DOLLAR_CHAR)

        override fun create(): InlineContentParser {
            return LatexInlineContentParser()
        }
    }
}
