package org.commonmark.node

/**
 * The base class of all CommonMark AST nodes ([Block] and inlines).
 *
 *
 * A node can have multiple children, and a parent (except for the root node).
 */
abstract class Node {
    private var parent: Node? = null
    var firstChild: Node? = null
        private set
    var lastChild: Node? = null
        private set
    var previous: Node? = null
        private set
    var next: Node? = null
        private set
    private var sourceSpans: MutableList<SourceSpan>? = null

    abstract fun accept(visitor: Visitor)

    open fun parent(): Node? {
        return parent
    }

    open fun getParent1(): Node? {
        return parent
    }

    protected open fun setParent1(parent: Node?) {
        this.parent = parent
    }

    fun appendChild(child: Node) {
        child.unlink()
        child.parent = this
        val lastChild = this.lastChild
        if (lastChild != null) {
            lastChild.next = child
            child.previous = this.lastChild
        } else {
            this.firstChild = child
        }
        this.lastChild = child
    }

    fun prependChild(child: Node) {
        child.unlink()
        child.parent = this
        val firstChild = this.firstChild
        if (firstChild != null) {
            firstChild.previous = child
            child.next = this.firstChild
            this.firstChild = child
        } else {
            this.firstChild = child
            this.lastChild = child
        }
    }

    fun unlink() {
        val previous = this.previous
        val parent = this.parent
        if (previous != null) {
            previous.next = this.next
        } else if (parent != null) {
            parent.firstChild = this.next
        }
        val next = this.next
        val parent1 = this.parent
        if (next != null) {
            next.previous = this.previous
        } else if (parent1 != null) {
            parent1.lastChild = this.previous
        }
        this.parent = null
        this.next = null
        this.previous = null
    }

    /**
     * Inserts the `sibling` node after `this` node.
     */
    fun insertAfter(sibling: Node) {
        sibling.unlink()
        sibling.next = this.next
        val next = sibling.next
        if (next != null) {
            next.previous = sibling
        }
        sibling.previous = this
        this.next = sibling
        sibling.parent = this.parent
        if (sibling.next == null) {
            sibling.parent!!.lastChild = sibling
        }
    }

    /**
     * Inserts the `sibling` node before `this` node.
     */
    fun insertBefore(sibling: Node) {
        sibling.unlink()
        sibling.previous = this.previous
        val previous = sibling.previous
        if (previous != null) {
            previous.next = sibling
        }
        sibling.next = this
        this.previous = sibling
        sibling.parent = this.parent
        if (sibling.previous == null) {
            sibling.parent!!.firstChild = sibling
        }
    }

    /**
     * @return the source spans of this node if included by the parser, an empty list otherwise
     * @since 0.16.0
     */
    fun getSourceSpans(): List<SourceSpan> {
        return sourceSpans ?: emptyList()
    }

    /**
     * Replace the current source spans with the provided list.
     *
     * @param sourceSpans the new source spans to set
     * @since 0.16.0
     */
    fun setSourceSpans(sourceSpans: List<SourceSpan>) {
        if (sourceSpans.isEmpty()) {
            this.sourceSpans = null
        } else {
            this.sourceSpans = ArrayList(sourceSpans)
        }
    }

    /**
     * Add a source span to the end of the list.
     *
     * @param sourceSpan the source span to add
     * @since 0.16.0
     */
    fun addSourceSpan(sourceSpan: SourceSpan) {
        val list = sourceSpans ?: mutableListOf<SourceSpan>().also { sourceSpans = it }
        list.add(sourceSpan)
    }

    override fun toString(): String {
        return this::class.simpleName + "{" + toStringAttributes() + "}"
    }

    protected open fun toStringAttributes(): String? {
        return ""
    }
}
