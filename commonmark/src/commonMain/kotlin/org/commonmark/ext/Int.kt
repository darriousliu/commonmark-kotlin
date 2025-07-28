package org.commonmark.ext

@OptIn(ExperimentalStdlibApi::class)
fun Int.toChars(): CharArray {
    if (isBmpCodePoint(this)) {
        return charArrayOf(this.toChar())
    } else if (isValidCodePoint(this)) {
        val result = CharArray(2)
        toSurrogates(this, result, 0)
        return result
    } else {
        throw IllegalArgumentException(
            "Not a valid Unicode code point: ${this.toHexString()}"
        )
    }
}

fun isBmpCodePoint(codePoint: Int): Boolean {
    return codePoint ushr 16 == 0
    // Optimized form of:
    //     codePoint >= MIN_VALUE && codePoint <= MAX_VALUE
    // We consistently use logical shift (>>>) to facilitate
    // additional runtime optimizations.
}

fun isValidCodePoint(codePoint: Int): Boolean {
    // Optimized form of:
    //     codePoint >= MIN_CODE_POINT && codePoint <= MAX_CODE_POINT
    val plane = codePoint ushr 16
    return plane < ((0X10FFFF + 1) ushr 16)
}

fun lowSurrogate(codePoint: Int): Char {
    return ((codePoint and 0x3ff) + Char.MIN_LOW_SURROGATE.code).toChar()
}

fun highSurrogate(codePoint: Int): Char {
    return ((codePoint ushr 10)
            + (Char.MIN_HIGH_SURROGATE.code - (0x010000 ushr 10))).toChar()
}

fun toSurrogates(codePoint: Int, dst: CharArray, index: Int) {
    // We write elements "backwards" to guarantee all-or-nothing
    dst[index + 1] = lowSurrogate(codePoint)
    dst[index] = highSurrogate(codePoint)
}