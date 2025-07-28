package org.commonmark.internal.renderer.text

abstract class ListHolder internal constructor(val parent: ListHolder?) {
    val indent: String

    init {
        indent = if (parent != null) {
            parent.indent + INDENT_DEFAULT
        } else {
            INDENT_EMPTY
        }
    }

    companion object {
        private const val INDENT_DEFAULT = "   "
        private const val INDENT_EMPTY = ""
    }
}
