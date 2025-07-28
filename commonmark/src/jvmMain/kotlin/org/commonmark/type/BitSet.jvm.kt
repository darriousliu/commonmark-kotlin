package org.commonmark.type

import java.util.BitSet

actual class BitSetWrapper actual constructor() {
    private var bitSet = BitSet()

    actual operator fun get(index: Int): Boolean = bitSet[index]
    actual fun set(index: Int) = bitSet.set(index)
    actual operator fun set(index: Int, value: Boolean) = bitSet.set(index, value)
    actual fun clone(): BitSetWrapper = apply {
        bitSet = bitSet.clone() as BitSet
    }
}