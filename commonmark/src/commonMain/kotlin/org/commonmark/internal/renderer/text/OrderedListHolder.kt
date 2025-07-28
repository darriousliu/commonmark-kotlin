package org.commonmark.internal.renderer.text

import org.commonmark.node.OrderedList

class OrderedListHolder(parent: ListHolder?, list: OrderedList) : ListHolder(parent) {
    val delimiter: String
    var counter: Int
        private set

    init {
        val markerDelimiter = list.markerDelimiter
        delimiter = markerDelimiter ?: "."
        val markerStartNumber = list.markerStartNumber
        counter = markerStartNumber ?: 1
    }

    fun increaseCounter() {
        counter++
    }
}
