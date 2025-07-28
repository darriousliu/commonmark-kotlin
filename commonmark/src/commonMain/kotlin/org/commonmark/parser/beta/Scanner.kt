package org.commonmark.parser.beta

import org.commonmark.node.SourceSpan
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.text.Characters
import kotlin.require

/**
 * Scanner 类用于逐行解析输入源并提供字符的迭代和匹配功能。
 *
 * 本类支持对多行内容的逐字符读取，提供方法解析字符、匹配内容以及查询位置，
 * 用于处理需要逐步扫描源内容的场景。
 *
 * 构造函数为内部实现，建议通过伴生对象中的 `of` 方法创建实例。
 *
 * 功能包含：
 * - 获取当前位置字符或代码点。
 * - 查询当前位置之前的代码点。
 * - 验证是否有下一个字符或跳至下一个字符。
 * - 匹配特定字符、字符串或字符集合。
 * - 统计连续匹配的字符数量。
 * - 定位特定字符或字符集的出现位置。
 * - 处理空白字符。
 * - 管理和恢复扫描器的当前位置。
 * - 获取指定范围内的内容信息。
 *
 * 此类主要面向解析器或需要对文本逐行操作的场景，旨在提供更高效和直观的操作。
 */
class Scanner internal constructor(
    // Lines without newlines at the end. The scanner will yield `\n` between lines because they're significant for
    // parsing and the final output. There is no `\n` after the last line.
    private val lines: List<SourceLine>,
    // Which line we're at.
    private var lineIndex: Int,
    // The index within the line. If index == length(), we pretend that there's a `\n` and only advance after we yield
    // that.
    private var index: Int
) {
    // Current line or "" if at the end of the lines (using "" instead of null saves a null check)
    /**
     * 表示当前正在处理的行，如果到达行尾则为 `""`。
     * 使用空字符串而非 `null` 是为了避免空值检查。
     */
    private var line: SourceLine = SourceLine.of("", null)

    /**
     * 表示当前处理行的长度。
     * 用于辅助扫描器解析输入字符串时跟踪当前行的字符数。
     * 此变量可能会在解析过程中被动态更新以适应不同行的内容。
     */
    private var lineLength = 0

    init {
        if (!lines.isEmpty()) {
            checkPosition(lineIndex, index)
            setLine(lines[lineIndex])
        }
    }

    /**
     * 返回当前扫描器的当前位置的字符。
     *
     * 如果当前位置在当前行范围内，则返回当前索引处的字符。
     * 如果当前位置超出当前行，但还有后续行，则返回换行符 `\n`。
     * 如果已经是文件中最后一行的末尾，则返回特定的结束符号 `END`。
     *
     * @return 当前索引处的字符，或根据位置返回换行符或结束符号。
     */
    fun peek(): Char {
        return if (index < lineLength) {
            line.content[index]
        } else {
            if (lineIndex < lines.size - 1) {
                '\n'
            } else {
                // Don't return a newline for the end of the last line
                END
            }
        }
    }

    /**
     * 获取当前扫描器位置的代码点（Unicode Code Point），不移动扫描器位置。
     * 如果当前位置是高代理项（high surrogate）并且下一个位置存在低代理项（low surrogate），
     * 则计算并返回对应的补充字符代码点（surrogate pair）。
     * 如果是普通字符则返回其代码点值。
     * 在最后一行的末尾返回预定义的结束标记代码点。
     *
     * @return 当前扫描器位置的代码点。如果到达最后一行的末尾且无更多代码点时，返回结束标记代码点。
     */
    fun peekCodePoint(): Int {
        if (index < lineLength) {
            val c = line.content[index]
            if (c.isHighSurrogate() && index + 1 < lineLength) {
                val low = line.content[index + 1]
                if (low.isLowSurrogate()) {
                    return Characters.toCodePoint(c, low)
                }
            }
            return c.code
        } else {
            return if (lineIndex < lines.size - 1) {
                '\n'.code
            } else {
                // Don't return a newline for the end of the last line
                END.code
            }
        }
    }

    /**
     * 获取当前位置之前的 Unicode 码点。如果当前位置存在有效的前一个字符或字符对，则返回其对应的 Unicode 码点；
     * 如果当前处于第一行开头，则返回行分隔符的 Unicode 码点；否则返回特殊结束标志的 Unicode 码点。
     *
     * @return 上一个 Unicode 码点，如果没有有效的前一个字符则返回 '\n'.code 或 END.code
     */
    fun peekPreviousCodePoint(): Int {
        if (index > 0) {
            val prev = index - 1
            val c: Char = line.content[prev]
            if (c.isLowSurrogate() && prev > 0) {
                val high: Char = line.content[prev - 1]
                if (high.isHighSurrogate()) {
                    return Characters.toCodePoint(high, c)
                }
            }
            return c.code
        } else {
            return if (lineIndex > 0) {
                '\n'.code
            } else {
                END.code
            }
        }
    }

    /**
     * 检查扫描器是否还有更多字符或行可供处理。
     *
     * @return 如果扫描器中还有未处理的字符或行，返回 true；否则返回 false。
     */
    fun hasNext(): Boolean {
        return if (index < lineLength) {
            true
        } else {
            // No newline at the end of the last line
            lineIndex < lines.size - 1
        }
    }

    /**
     * 更新扫描器的当前位置，并根据需要切换到下一行。如果当前位置超出当前行的长度，则将扫描器位置移动到下一行。
     * 如果已到达最后一行，则设置为空行，并将当前位置重置为行起始位置。
     */
    fun next() {
        index++
        if (index > lineLength) {
            lineIndex++
            if (lineIndex < lines.size) {
                setLine(lines[lineIndex])
            } else {
                setLine(SourceLine.of("", null))
            }
            index = 0
        }
    }

    /**
     * 尝试将当前扫描器的下一个字符与指定字符进行匹配。
     * 如果匹配成功，将扫描器移动到下一个字符并返回true；否则不移动扫描器并返回false。
     *
     * @param c 要与当前扫描器下一个字符进行匹配的字符
     * @return 如果匹配成功返回true，否则返回false
     */
    fun next(c: Char): Boolean {
        if (peek() == c) {
            next()
            return true
        } else {
            return false
        }
    }

    /**
     * 检查并消费当前扫描器位置是否匹配指定的字符串内容。
     *
     * @param content 要匹配的字符串内容。
     * @return 如果当前扫描器位置的内容与指定字符串匹配并成功消费，则返回true；否则返回false。
     */
    fun next(content: String): Boolean {
        if (index < lineLength && index + content.length <= lineLength) {
            // Can't use startsWith because it's not available on CharSequence
            for (i in 0..<content.length) {
                if (line.content[index + i] != content[i]) {
                    return false
                }
            }
            index += content.length
            return true
        } else {
            return false
        }
    }

    /**
     * 匹配给定字符并返回连续匹配的次数。
     * 方法会使用内部提供的`peek`方法检测当前字符，如果与指定字符匹配，则计数，并移动到下一个字符。
     *
     * @param c 要匹配的字符。
     * @return 匹配到的连续字符的数量。
     */
    fun matchMultiple(c: Char): Int {
        var count = 0
        while (peek() == c) {
            count++
            next()
        }
        return count
    }

    /**
     * 匹配并统计输入中连续满足给定字符匹配条件的字符数量。
     *
     * @param matcher 用于判断字符是否匹配的匹配器，其定义了匹配条件。
     * @return 连续匹配的字符数量。
     */
    fun match(matcher: org.commonmark.text.CharMatcher): Int {
        var count = 0
        while (matcher.matches(peek())) {
            count++
            next()
        }
        return count
    }

    /**
     * 计算并返回连续的空白字符数量。
     *
     * 遍历输入流中的字符序列，直到碰到非空白字符为止。
     * 空白字符包括以下几种：
     * - 空格 (' ')
     * - 制表符 ('\t')
     * - 换行符 ('\n')
     * - 垂直制表符 ('\u000B')
     * - 表单换页符 ('\u000C')
     * - 回车符 ('\r')
     *
     * @return 连续空白字符的数量
     */
    fun whitespace(): Int {
        var count = 0
        while (true) {
            when (peek()) {
                ' ', '\t', '\n', '\u000B', '\u000c', '\r' -> {
                    count++
                    next()
                }

                else -> return count
            }
        }
    }

    /**
     * 在输入流中查找指定字符并返回其第一次出现的位置。
     *
     * @param c 要查找的字符。
     * @return 如果找到指定字符，则返回其相对于当前扫描起点的索引位置；如果未找到指定字符且到达输入流末尾，则返回 -1。
     */
    fun find(c: Char): Int {
        var count = 0
        while (true) {
            val cur = peek()
            if (cur == END) {
                return -1
            } else if (cur == c) {
                return count
            }
            count++
            next()
        }
    }

    /**
     * 在文本中查找第一个匹配给定条件的字符位置。
     * 指定的匹配条件由 `matcher` 参数定义，方法会遍历当前行或缓冲区，
     * 返回第一个满足条件字符的相对位置。如果遍历结束仍未找到，则返回 -1。
     *
     * @param matcher 用于匹配字符的条件，必须实现 `CharMatcher` 接口。
     * @return 首个匹配字符相对当前位置的索引，如果没有匹配项，则返回 -1。
     */
    fun find(matcher: org.commonmark.text.CharMatcher): Int {
        var count = 0
        while (true) {
            val c = peek()
            if (c == END) {
                return -1
            } else if (matcher.matches(c)) {
                return count
            }
            count++
            next()
        }
    }

    /**
     * 返回当前解析器的位置。
     *
     * 此方法将解析器的当前位置以不可变的 `Position` 对象形式返回。位置通常包括行索引和字符索引。
     *
     * @return 一个描述当前扫描位置的 `Position` 实例
     */
// Don't expose the int index, because it would be good if we could switch input to a List<String> of lines later
    // instead of one contiguous String.
    fun position(): Position {
        return Position(lineIndex, index)
    }

    /**
     * 设置扫描器中的当前位置。
     *
     * @param position 表示新的行索引和字符索引的位置信息。
     */
    fun setPosition(position: Position) {
        checkPosition(position.lineIndex, position.index)
        this.lineIndex = position.lineIndex
        this.index = position.index
        setLine(lines[this.lineIndex])
    }

    /**
     * 根据指定的起始位置和结束位置，返回对应的源代码行集合。
     *
     * @param begin 起始位置的位置信息，包含行索引和行内偏移索引
     * @param end 结束位置的位置信息，包含行索引和行内偏移索引
     * @return 源代码的多行集合，表示从起始位置到结束位置的内容
     */
// For cases where the caller appends the result to a StringBuilder, we could offer another method to avoid some
    // unnecessary copying.
    fun getSource(begin: Position, end: Position): SourceLines {
        if (begin.lineIndex == end.lineIndex) {
            // Shortcut for the common case of the text from a single line
            val line = lines[begin.lineIndex]
            val newContent = line.content.subSequence(begin.index, end.index)
            var newSourceSpan: SourceSpan? = null
            val sourceSpan = line.getSourceSpan()
            if (sourceSpan != null) {
                newSourceSpan = sourceSpan.subSpan(begin.index, end.index)
            }
            return SourceLines.of(SourceLine.of(newContent, newSourceSpan))
        } else {
            val sourceLines: SourceLines = SourceLines.empty()

            val firstLine: SourceLine = lines[begin.lineIndex]
            sourceLines.addLine(firstLine.substring(begin.index, firstLine.content.length))

            // Lines between start and end (we are appending the full line)
            for (line in begin.lineIndex + 1..<end.lineIndex) {
                sourceLines.addLine(lines[line])
            }

            val lastLine: SourceLine = lines[end.lineIndex]
            sourceLines.addLine(lastLine.substring(0, end.index))
            return sourceLines
        }
    }

    /**
     * 设置当前处理的行内容。
     *
     * @param line 包含要设置的文本内容及其相关信息的 SourceLine 实例
     */
    private fun setLine(line: SourceLine) {
        this.line = line
        this.lineLength = line.content.length
    }

    /**
     * 检查给定的行索引和字符索引是否在有效范围内。
     *
     * @param lineIndex 行索引，用于定位要检查的文本行。
     * @param index 字符索引，用于确定文本行中的具体位置。
     * @throws IllegalArgumentException 如果行索引超出有效范围或字符索引超出指定行的长度。
     */
    private fun checkPosition(lineIndex: Int, index: Int) {
        require(!(lineIndex < 0 || lineIndex >= lines.size)) { "Line index " + lineIndex + " out of range, number of lines: " + lines.size }
        val line = lines[lineIndex]
        require(
            !(index < 0 || index > line.content.length)
        ) { "Index " + index + " out of range, line length: " + line.content.length }
    }

    /**
     * 扫描器（Scanner）的伴生对象，用于提供辅助方法和常量。
     */
    companion object {
        /**
         * 表示扫描器中的结束字符常量。其值为 Unicode 空字符 '\u0000'。
         *
         * 用于在扫描过程中标识结束位置或者为空的占位符。可以通过该值判断是否已经到达数据流的末尾。
         */
        const val END: Char = '\u0000'

        /**
         * 创建一个新的 Scanner 实例，并初始化其状态。
         *
         * @param lines 输入的 SourceLines 对象，包含需要解析的内容行。
         * @return 新的 Scanner 实例，用于字符扫描和解析。
         */
        fun of(lines: SourceLines): Scanner {
            return Scanner(lines.getLines(), 0, 0)
        }
    }
}
