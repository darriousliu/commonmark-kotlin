package org.commonmark.type

@OptIn(ObsoleteNativeApi::class)
actual class BitSetWrapper {
    private var bitSet = BitSet()

    actual operator fun get(index: Int): Boolean = bitSet[index]
    actual fun set(index: Int) = bitSet.set(index)
    actual operator fun set(index: Int, value: Boolean) = bitSet.set(index, value)
    actual fun clone(): BitSetWrapper = apply {
        val newBitSet = BitSet()
        for (i in 0 until bitSet.size) {
            newBitSet.set(i, bitSet[i])
        }
        bitSet = newBitSet
    }
}