package org.commonmark.node

/**
 * A list block like [BulletList] or [OrderedList].
 */
abstract class ListBlock : Block() {
    /**
     * @return whether this list is tight or loose
     * @see [CommonMark Spec for tight lists](https://spec.commonmark.org/0.31.2/.tight)
     */
    var isTight: Boolean = false
}
