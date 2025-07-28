package org.commonmark.text

object Characters {
    private val specialWords = listOf('$', '+', '<', '=', '>', '^', '`', '|', '~')

    fun find(c: Char, s: CharSequence, startIndex: Int): Int {
        val length = s.length
        for (i in startIndex..<length) {
            if (s[i] == c) {
                return i
            }
        }
        return -1
    }

    fun findLineBreak(s: CharSequence, startIndex: Int): Int {
        val length = s.length
        for (i in startIndex..<length) {
            when (s[i]) {
                '\n', '\r' -> return i
            }
        }
        return -1
    }

    /**
     * @see [blank line](https://spec.commonmark.org/0.31.2/.blank-line)
     */
    fun isBlank(s: CharSequence): Boolean {
        return skipSpaceTab(s, 0, s.length) == s.length
    }

    fun hasNonSpace(s: CharSequence): Boolean {
        val length = s.length
        val skipped = skip(' ', s, 0, length)
        return skipped != length
    }

    fun isLetter(s: CharSequence, index: Int): Boolean {
        return s[index].isLetter()
    }

    fun isSpaceOrTab(s: CharSequence, index: Int): Boolean {
        if (index < s.length) {
            when (s[index]) {
                ' ', '\t' -> return true
            }
        }
        return false
    }

    /**
     * @see [Unicode punctuation character](https://spec.commonmark.org/0.31.2/.unicode-punctuation-character)
     */
    fun isPunctuationCodePoint(codePoint: Int): Boolean {
        if (codePoint > Char.MAX_VALUE.code) {
            return when (codePoint) {
                in 0x10100..0x10102 -> true
                in 0x10137..0x1013F -> true
                in 0x10179..0x10189 -> true
                in 0x1018C..0x1018E -> true
                in 0x10190..0x1019C -> true
                0x101A0 -> true
                in 0x101D0..0x101FC -> true
                0x1039F -> true
                0x103D0 -> true
                0x1056F -> true
                0x10857 -> true
                in 0x10877..0x10878 -> true
                0x1091F -> true
                0x1093F -> true
                in 0x10A50..0x10A58 -> true
                0x10A7F -> true
                0x10AC8 -> true
                in 0x10AF0..0x10AF6 -> true
                in 0x10B39..0x10B3F -> true
                in 0x10B99..0x10B9C -> true
                0x10D6E -> true
                in 0x10D8E..0x10D8F -> true
                0x10EAD -> true
                in 0x10F55..0x10F59 -> true
                in 0x10F86..0x10F89 -> true
                in 0x11047..0x1104D -> true
                in 0x110BB..0x110BC -> true
                in 0x110BE..0x110C1 -> true
                in 0x11140..0x11143 -> true
                in 0x11174..0x11175 -> true
                in 0x111C5..0x111C8 -> true
                0x111CD -> true
                0x111DB -> true
                in 0x111DD..0x111DF -> true
                in 0x11238..0x1123D -> true
                0x112A9 -> true
                in 0x113D4..0x113D5 -> true
                in 0x113D7..0x113D8 -> true
                in 0x1144B..0x1144F -> true
                in 0x1145A..0x1145B -> true
                0x1145D -> true
                0x114C6 -> true
                in 0x115C1..0x115D7 -> true
                in 0x11641..0x11643 -> true
                in 0x11660..0x1166C -> true
                0x116B9 -> true
                in 0x1173C..0x1173F -> true
                0x1183B -> true
                in 0x11944..0x11946 -> true
                0x119E2 -> true
                in 0x11A3F..0x11A46 -> true
                in 0x11A9A..0x11A9C -> true
                in 0x11A9E..0x11AA2 -> true
                in 0x11B00..0x11B09 -> true
                0x11BE1 -> true
                in 0x11C41..0x11C45 -> true
                in 0x11C70..0x11C71 -> true
                in 0x11EF7..0x11EF8 -> true
                in 0x11F43..0x11F4F -> true
                in 0x11FD5..0x11FF1 -> true
                0x11FFF -> true
                in 0x12470..0x12474 -> true
                in 0x12FF1..0x12FF2 -> true
                in 0x16A6E..0x16A6F -> true
                0x16AF5 -> true
                in 0x16B37..0x16B3F -> true
                in 0x16B44..0x16B45 -> true
                in 0x16D6D..0x16D6F -> true
                in 0x16E97..0x16E9A -> true
                0x16FE2 -> true
                0x1BC9C -> true
                0x1BC9F -> true
                in 0x1CC00..0x1CCEF -> true
                in 0x1CD00..0x1CEB3 -> true
                in 0x1CF50..0x1CFC3 -> true
                in 0x1D000..0x1D0F5 -> true
                in 0x1D100..0x1D126 -> true
                in 0x1D129..0x1D164 -> true
                in 0x1D16A..0x1D16C -> true
                in 0x1D183..0x1D184 -> true
                in 0x1D18C..0x1D1A9 -> true
                in 0x1D1AE..0x1D1EA -> true
                in 0x1D200..0x1D241 -> true
                0x1D245 -> true
                in 0x1D300..0x1D356 -> true
                0x1D6C1 -> true
                0x1D6DB -> true
                0x1D6FB -> true
                0x1D715 -> true
                0x1D735 -> true
                0x1D74F -> true
                0x1D76F -> true
                0x1D789 -> true
                0x1D7A9 -> true
                0x1D7C3 -> true
                in 0x1D800..0x1D9FF -> true
                in 0x1DA37..0x1DA3A -> true
                in 0x1DA6D..0x1DA74 -> true
                in 0x1DA76..0x1DA83 -> true
                in 0x1DA85..0x1DA8B -> true
                0x1E14F -> true
                0x1E2FF -> true
                0x1E5FF -> true
                in 0x1E95E..0x1E95F -> true
                0x1ECAC -> true
                0x1ECB0 -> true
                0x1ED2E -> true
                in 0x1EEF0..0x1EEF1 -> true
                in 0x1F000..0x1F02B -> true
                in 0x1F030..0x1F093 -> true
                in 0x1F0A0..0x1F0AE -> true
                in 0x1F0B1..0x1F0BF -> true
                in 0x1F0C1..0x1F0CF -> true
                in 0x1F0D1..0x1F0F5 -> true
                in 0x1F10D..0x1F1AD -> true
                in 0x1F1E6..0x1F202 -> true
                in 0x1F210..0x1F23B -> true
                in 0x1F240..0x1F248 -> true
                in 0x1F250..0x1F251 -> true
                in 0x1F260..0x1F265 -> true
                in 0x1F300..0x1F6D7 -> true
                in 0x1F6DC..0x1F6EC -> true
                in 0x1F6F0..0x1F6FC -> true
                in 0x1F700..0x1F776 -> true
                in 0x1F77B..0x1F7D9 -> true
                in 0x1F7E0..0x1F7EB -> true
                0x1F7F0 -> true
                in 0x1F800..0x1F80B -> true
                in 0x1F810..0x1F847 -> true
                in 0x1F850..0x1F859 -> true
                in 0x1F860..0x1F887 -> true
                in 0x1F890..0x1F8AD -> true
                in 0x1F8B0..0x1F8BB -> true
                in 0x1F8C0..0x1F8C1 -> true
                in 0x1F900..0x1FA53 -> true
                in 0x1FA60..0x1FA6D -> true
                in 0x1FA70..0x1FA7C -> true
                in 0x1FA80..0x1FA89 -> true
                in 0x1FA8F..0x1FAC6 -> true
                in 0x1FACE..0x1FADC -> true
                in 0x1FADF..0x1FAE9 -> true
                in 0x1FAF0..0x1FAF8 -> true
                in 0x1FB00..0x1FB92 -> true
                in 0x1FB94..0x1FBEF -> true
                else -> false
            }
        }
        val char = codePoint.toChar()
        return when (char.category) {
            CharCategory.DASH_PUNCTUATION,
            CharCategory.START_PUNCTUATION,
            CharCategory.END_PUNCTUATION,
            CharCategory.CONNECTOR_PUNCTUATION,
            CharCategory.OTHER_PUNCTUATION,
            CharCategory.INITIAL_QUOTE_PUNCTUATION,
            CharCategory.FINAL_QUOTE_PUNCTUATION,
            CharCategory.MATH_SYMBOL,
            CharCategory.CURRENCY_SYMBOL,
            CharCategory.MODIFIER_SYMBOL,
            CharCategory.OTHER_SYMBOL,
                -> true

            else -> char in specialWords
        }
    }

    /**
     * Check whether the provided code point is a Unicode whitespace character as defined in the spec.
     *
     * @see [Unicode whitespace character](https://spec.commonmark.org/0.31.2/.unicode-whitespace-character)
     */
    fun isWhitespaceCodePoint(codePoint: Int): Boolean {
        if (codePoint > Char.MAX_VALUE.code) return false
        return when (codePoint) {
            ' '.code, '\t'.code, '\n'.code, '\u000c'.code, '\r'.code -> true
            else -> codePoint.toChar().category == CharCategory.SPACE_SEPARATOR
        }
    }

    fun skip(skip: Char, s: CharSequence, startIndex: Int, endIndex: Int): Int {
        for (i in startIndex..<endIndex) {
            if (s[i] != skip) {
                return i
            }
        }
        return endIndex
    }

    fun skipBackwards(skip: Char, s: CharSequence, startIndex: Int, lastIndex: Int): Int {
        for (i in startIndex downTo lastIndex) {
            if (s[i] != skip) {
                return i
            }
        }
        return lastIndex - 1
    }

    fun skipSpaceTab(s: CharSequence, startIndex: Int, endIndex: Int): Int {
        for (i in startIndex..<endIndex) {
            when (s[i]) {
                ' ', '\t' -> {}
                else -> return i
            }
        }
        return endIndex
    }

    fun skipSpaceTabBackwards(s: CharSequence, startIndex: Int, lastIndex: Int): Int {
        for (i in startIndex downTo lastIndex) {
            when (s[i]) {
                ' ', '\t' -> {}
                else -> return i
            }
        }
        return lastIndex - 1
    }

    fun isHighSurrogate(c: Char) = (c.code in 0xD800..0xDBFF)
    fun isLowSurrogate(c: Char) = (c.code in 0xDC00..0xDFFF)

    fun codePointAt(sequence: CharSequence, index: Int): Int {
        val ch1 = sequence[index]
        if (isHighSurrogate(ch1) && index + 1 < sequence.length) {
            val ch2 = sequence[index + 1]
            if (isLowSurrogate(ch2)) {
                return ((ch1.code - 0xD800) shl 10) + (ch2.code - 0xDC00) + 0x10000
            }
        }
        return ch1.code
    }

    fun toCodePoint(high: Char, low: Char): Int {
        // Optimized form of:
        // return ((high - MIN_HIGH_SURROGATE) << 10)
        //         + (low - MIN_LOW_SURROGATE)
        //         + MIN_SUPPLEMENTARY_CODE_POINT;
        return ((high.code shl 10) + low.code) + ((0x010000
                - (Char.MIN_HIGH_SURROGATE.code shl 10)
                - Char.MIN_LOW_SURROGATE.code))
    }
}