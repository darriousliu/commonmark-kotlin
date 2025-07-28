package org.commonmark.node

/**
 * Block nodes such as paragraphs, list blocks, code blocks etc.
 */
abstract class Block : Node() {
    override fun getParent1(): Node? {
        return super.getParent1() as Block?
    }

    override fun setParent1(parent: Node?) {
        require(parent is Block) { "Parent of block must also be block (can not be inline)" }
        super.setParent1(parent)
    }
}
