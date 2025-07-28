package org.commonmark.renderer.text

import okio.IOException

class TextContentWriter(
    private val buffer: Appendable,
    private val lineBreakRendering: LineBreakRendering
) {
    private val tight: MutableList<Boolean> = mutableListOf()

    private var blockSeparator: String? = null
    private var lastChar = 0.toChar()

    constructor(out: Appendable) : this(out, LineBreakRendering.COMPACT)

    fun whitespace() {
        if (lastChar.code != 0 && lastChar != ' ') {
            write(' ')
        }
    }

    fun colon() {
        if (lastChar.code != 0 && lastChar != ':') {
            write(':')
        }
    }

    fun line() {
        append('\n')
    }

    fun block() {
        blockSeparator = if (lineBreakRendering === LineBreakRendering.STRIP) " " else  //
            if (lineBreakRendering === LineBreakRendering.COMPACT || isTight()) "\n" else "\n\n"
    }

    fun resetBlock() {
        blockSeparator = null
    }

    fun writeStripped(s: String) {
        write(s.replace("[\\r\\n\\s]+", " "))
    }

    fun write(s: String) {
        flushBlockSeparator()
        append(s)
    }

    fun write(c: Char) {
        flushBlockSeparator()
        append(c)
    }

    /**
     * Change whether blocks are tight or loose. Loose is the default where blocks are separated by a blank line. Tight
     * is where blocks are not separated by a blank line. Tight blocks are used in lists if there are no blank lines
     * within the list.
     *
     *
     * Note that changing this does not affect block separators that have already been enqueued with [.block],
     * only future ones.
     */
    fun pushTight(tight: Boolean) {
        this.tight.add(tight)
    }

    /**
     * Remove the last "tight" setting from the top of the stack.
     */
    fun popTight() {
        this.tight.removeLast()
    }

    private fun isTight(): Boolean {
        return !tight.isEmpty() && tight.last()
    }

    /**
     * If a block separator has been enqueued with [.block] but not yet written, write it now.
     */
    private fun flushBlockSeparator() {
        blockSeparator?.let {
            append(it)
            blockSeparator = null
        }
    }

    private fun append(s: String) {
        try {
            buffer.append(s)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        val length: Int = s.length
        if (length != 0) {
            lastChar = s[length - 1]
        }
    }

    private fun append(c: Char) {
        try {
            buffer.append(c)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        lastChar = c
    }
}
