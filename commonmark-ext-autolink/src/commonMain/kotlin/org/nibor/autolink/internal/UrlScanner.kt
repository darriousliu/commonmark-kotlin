package org.nibor.autolink.internal

import org.nibor.autolink.LinkSpan
import org.nibor.autolink.LinkType

/**
 * Scan for URLs starting from the trigger character ":", requires "://".
 *
 *
 * Based on RFC 3986.
 */
class UrlScanner : Scanner {
    override fun scan(input: CharSequence, triggerIndex: Int, rewindIndex: Int): LinkSpan? {
        val length: Int = input.length
        val afterSlashSlash = triggerIndex + 3
        if (afterSlashSlash >= length || input[triggerIndex + 1] != '/' || input[triggerIndex + 2] != '/') {
            return null
        }

        val first = findFirst(input, triggerIndex - 1, rewindIndex)
        if (first == -1) {
            return null
        }

        val last = Scanners.findUrlEnd(input, afterSlashSlash)
        if (last == -1) {
            return null
        }

        return LinkSpanImpl(LinkType.URL, first, last + 1)
    }

    // See "scheme" in RFC 3986
    private fun findFirst(input: CharSequence, beginIndex: Int, rewindIndex: Int): Int {
        var first = -1
        var digit = -1
        for (i in beginIndex downTo rewindIndex) {
            val c = input[i]
            if (Scanners.isAlpha(c)) {
                first = i
            } else if (Scanners.isDigit(c)) {
                digit = i
            } else if (!schemeSpecial(c)) {
                break
            }
        }
        if (first > 0 && first - 1 == digit) {
            // We don't want to extract "abc://foo" out of "1abc://foo".
            // ".abc://foo" and others are ok though, as they feel more like separators.
            first = -1
        }
        return first
    }

    companion object {
        private fun schemeSpecial(c: Char): Boolean {
            when (c) {
                '+', '-', '.' -> return true
            }
            return false
        }
    }
}
