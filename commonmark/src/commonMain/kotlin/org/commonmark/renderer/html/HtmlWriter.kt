package org.commonmark.renderer.html

import okio.IOException
import org.commonmark.internal.util.Escaping

class HtmlWriter(out: Appendable) {
    private val buffer: Appendable = out
    private var lastChar = 0.toChar()

    fun raw(s: String) {
        append(s)
    }

    fun text(text: String) {
        append(Escaping.escapeHtml(text))
    }

    fun tag(name: String) {
        tag(name, NO_ATTRIBUTES)
    }

    fun tag(name: String, attrs: Map<String, String?>?) {
        tag(name, attrs, false)
    }

    fun tag(name: String, attrs: Map<String, String?>?, voidElement: Boolean) {
        append("<")
        append(name)
        if (attrs != null && !attrs.isEmpty()) {
            for (attr in attrs.entries) {
                append(" ")
                append(Escaping.escapeHtml(attr.key))
                val value = attr.value
                if (value != null) {
                    append("=\"")
                    append(Escaping.escapeHtml(value))
                    append("\"")
                }
            }
        }
        if (voidElement) {
            append(" /")
        }

        append(">")
    }

    fun line() {
        if (lastChar.code != 0 && lastChar != '\n') {
            append("\n")
        }
    }

    private fun append(s: String) {
        try {
            buffer.append(s)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val length = s.length
        if (length != 0) {
            lastChar = s[length - 1]
        }
    }

    companion object {
        private val NO_ATTRIBUTES: Map<String, String?> = mapOf()
    }
}
