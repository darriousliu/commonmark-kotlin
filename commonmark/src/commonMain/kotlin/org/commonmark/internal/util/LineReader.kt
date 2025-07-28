package org.commonmark.internal.util

import okio.BufferedSource
import okio.Closeable
import okio.IOException
import okio.use


/**
 * Reads lines from a reader like [java.io.BufferedReader] but also returns the line terminators.
 *
 *
 * Line terminators can be either a line feed `"\n"`, carriage return `"\r"`, or a carriage return followed
 * by a line feed `"\r\n"`. Call [.getLineTerminator] after [.readLine] to obtain the
 * corresponding line terminator. If a stream has a line at the end without a terminator, [.getLineTerminator]
 * returns `null`.
 */
class LineReader(private var source: BufferedSource?) : Closeable {
    var lineTerminator: String? = null
        private set

    /**
     * 读取一行文本。
     * @return 行内容，或 null（流结束且无内容）
     */
    @Throws(IOException::class)
    fun readLine(): String? {
        val src = source ?: return null
        if (src.exhausted()) return null

        val sb = StringBuilder()
        var foundTerminator: String? = null

        while (!src.exhausted()) {
            val c = src.readUtf8CodePoint().toChar()
            when (c) {
                '\n' -> {
                    foundTerminator = "\n"
                    break
                }

                '\r' -> {
                    if (!src.exhausted()) {
                        src.peek().use { peeked ->
                            val next = peeked.readUtf8CodePoint().toChar()
                            if (next == '\n') {
                                src.readUtf8CodePoint() // 消费掉 \n
                                foundTerminator = "\r\n"
                            } else {
                                foundTerminator = "\r"
                            }
                        }
                    } else {
                        foundTerminator = "\r"
                    }
                    break
                }

                else -> sb.append(c)
            }
        }

        // 处理流结尾
        if (sb.isEmpty() && foundTerminator == null && src.exhausted()) {
            return null
        }

        lineTerminator = foundTerminator
        return sb.toString()
    }

    override fun close() {
        source?.close()
        source = null
    }
}


