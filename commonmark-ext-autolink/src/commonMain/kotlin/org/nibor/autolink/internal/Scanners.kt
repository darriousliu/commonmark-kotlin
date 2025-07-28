package org.nibor.autolink.internal

object Scanners {
    fun isAlpha(c: Char): Boolean {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
    }

    fun isDigit(c: Char): Boolean {
        return c >= '0' && c <= '9'
    }

    fun isAlnum(c: Char): Boolean {
        return isAlpha(c) || isDigit(c)
    }

    fun isNonAscii(c: Char): Boolean {
        return c.code >= 0x80
    }

    fun findUrlEnd(input: CharSequence, beginIndex: Int): Int {
        var round = 0
        var square = 0
        var curly = 0
        val doubleQuote = false
        var singleQuote = false
        var last = -1
        loop@ for (i in beginIndex..<input.length) {
            val c: Char = input[i]
            when (c) {
                '\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007', '\u0008', '\t', '\n', '\u000B', '\u000c', '\r', '\u000E', '\u000F', '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017', '\u0018', '\u0019', '\u001A', '\u001B', '\u001C', '\u001D', '\u001E', '\u001F', ' ', '\"', '<', '>', '`', '\u007F', '\u0080', '\u0081', '\u0082', '\u0083', '\u0084', '\u0085', '\u0086', '\u0087', '\u0088', '\u0089', '\u008A', '\u008B', '\u008C', '\u008D', '\u008E', '\u008F', '\u0090', '\u0091', '\u0092', '\u0093', '\u0094', '\u0095', '\u0096', '\u0097', '\u0098', '\u0099', '\u009A', '\u009B', '\u009C', '\u009D', '\u009E', '\u009F', '\u00A0', '\u2000', '\u2001', '\u2002', '\u2003', '\u2004', '\u2005', '\u2006', '\u2007', '\u2008', '\u2009', '\u200A', '\u2028', '\u2029', '\u202F', '\u205F', '\u3000' ->                     // While these are allowed by RFC 3987, they are Unicode whitespace characters
                    // that look like a space, so it would be confusing not to end URLs.
                    // They are also excluded from IDNs by some browsers.
                    break@loop

                '?', '!', '.', ',', ':', ';' -> {}
                '/' ->                     // This may be part of an URL and at the end, but not if the previous character can't be the end of an URL
                    if (last == i - 1) {
                        last = i
                    }

                '(' -> round++
                ')' -> {
                    round--
                    if (round >= 0) {
                        last = i
                    } else {
                        // More closing than opening brackets, stop now
                        break@loop
                    }
                }

                '[' ->                     // Allowed in IPv6 address host
                    square++

                ']' -> {
                    // Allowed in IPv6 address host
                    square--
                    if (square >= 0) {
                        last = i
                    } else {
                        // More closing than opening brackets, stop now
                        break@loop
                    }
                }

                '{' -> curly++
                '}' -> {
                    curly--
                    if (curly >= 0) {
                        last = i
                    } else {
                        // More closing than opening brackets, stop now
                        break@loop
                    }
                }

                '\'' -> {
                    singleQuote = !singleQuote
                    if (!singleQuote) {
                        last = i
                    }
                }

                else -> last = i
            }
        }
        return last
    }
}
