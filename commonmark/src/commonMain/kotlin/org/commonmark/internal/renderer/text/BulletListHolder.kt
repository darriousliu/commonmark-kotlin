package org.commonmark.internal.renderer.text

import org.commonmark.node.BulletList

class BulletListHolder(parent: ListHolder?, list: BulletList) : ListHolder(parent) {
    val marker: String? = list.marker
}
