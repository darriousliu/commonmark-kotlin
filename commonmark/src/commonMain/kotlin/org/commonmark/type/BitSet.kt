package org.commonmark.type

expect class BitSetWrapper() {
    operator fun get(index: Int): Boolean
    fun set(index: Int)
    operator fun set(index: Int, value: Boolean)
    fun clone(): BitSetWrapper
}