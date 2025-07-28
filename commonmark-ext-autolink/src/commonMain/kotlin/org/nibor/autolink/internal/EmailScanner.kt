package org.nibor.autolink.internal

import org.nibor.autolink.LinkSpan
import org.nibor.autolink.LinkType

/**
 * Scan for email address starting from the trigger character "@".
 *
 *
 * Based on RFC 6531, but also scans invalid IDN. Doesn't match IP address in domain part or quoting in local part.
 */
class EmailScanner(private val domainMustHaveDot: Boolean) : Scanner {
    override fun scan(input: CharSequence, triggerIndex: Int, rewindIndex: Int): LinkSpan? {
        val beforeAt = triggerIndex - 1
        val first = findFirst(input, beforeAt, rewindIndex)
        if (first == -1) {
            return null
        }

        val afterAt = triggerIndex + 1
        val last = findLast(input, afterAt)
        if (last == -1) {
            return null
        }

        return LinkSpanImpl(LinkType.EMAIL, first, last + 1)
    }

    // See "Local-part" in RFC 5321, plus extensions in RFC 6531
    private fun findFirst(input: CharSequence, beginIndex: Int, rewindIndex: Int): Int {
        var first = -1
        var atomBoundary = true
        for (i in beginIndex downTo rewindIndex) {
            val c: Char = input[i]
            if (localAtomAllowed(c)) {
                first = i
                atomBoundary = false
            } else if (c == '.') {
                if (atomBoundary) {
                    break
                }
                atomBoundary = true
            } else {
                break
            }
        }
        return first
    }

    // See "Domain" in RFC 5321, plus extension of "sub-domain" in RFC 6531
    private fun findLast(input: CharSequence, beginIndex: Int): Int {
        var firstInSubDomain = true
        var canEndSubDomain = false
        var firstDot = -1
        var last = -1
        for (i in beginIndex..<input.length) {
            val c = input[i]
            if (firstInSubDomain) {
                if (subDomainAllowed(c)) {
                    last = i
                    firstInSubDomain = false
                    canEndSubDomain = true
                } else {
                    break
                }
            } else {
                if (c == '.') {
                    if (!canEndSubDomain) {
                        break
                    }
                    firstInSubDomain = true
                    if (firstDot == -1) {
                        firstDot = i
                    }
                } else if (c == '-') {
                    canEndSubDomain = false
                } else if (subDomainAllowed(c)) {
                    last = i
                    canEndSubDomain = true
                } else {
                    break
                }
            }
        }
        return if (domainMustHaveDot && (firstDot == -1 || firstDot > last)) {
            -1
        } else {
            last
        }
    }

    // See "Atom" in RFC 5321, "atext" in RFC 5322
    private fun localAtomAllowed(c: Char): Boolean {
        if (Scanners.isAlnum(c) || Scanners.isNonAscii(c)) {
            return true
        }
        when (c) {
            '!', '#', '$', '%', '&', '\'', '*', '+', '-', '/', '=', '?', '^', '_', '`', '{', '|', '}', '~' -> return true
        }
        return false
    }

    // See "sub-domain" in RFC 5321. Extension in RFC 6531 is simplified, this can also match invalid domains.
    private fun subDomainAllowed(c: Char): Boolean {
        return Scanners.isAlnum(c) || Scanners.isNonAscii(c)
    }
}
