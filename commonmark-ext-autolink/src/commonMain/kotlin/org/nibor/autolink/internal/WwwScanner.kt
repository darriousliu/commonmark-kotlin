package org.nibor.autolink.internal

import org.nibor.autolink.LinkSpan
import org.nibor.autolink.LinkType

/**
 * Scan for WWW addresses such as "www.example.org" starting from the trigger character "w".
 * Requires "www." at the beginning and an additional dot in the domain.
 *
 *
 * Based on RFC 3986.
 */
class WwwScanner : Scanner {
    override fun scan(input: CharSequence, triggerIndex: Int, rewindIndex: Int): LinkSpan? {
        val afterDot = triggerIndex + 4
        if (afterDot >= input.length || !isWww(input, triggerIndex)) {
            return null
        }

        val first: Int = findFirst(
            input,
            triggerIndex,
            rewindIndex
        )
        if (first == -1) {
            return null
        }

        val last: Int = findLast(input, afterDot)
        if (last == -1) {
            return null
        }

        return LinkSpanImpl(LinkType.WWW, first, last + 1)
    }

    companion object {
        private fun findFirst(input: CharSequence, beginIndex: Int, rewindIndex: Int): Int {
            if (beginIndex == rewindIndex) {
                return beginIndex
            }

            // Is the character before www. allowed?
            if (isAllowed(input[beginIndex - 1])) {
                return beginIndex
            }

            return -1
        }

        private fun findLast(input: CharSequence, beginIndex: Int): Int {
            val last: Int = Scanners.findUrlEnd(input, beginIndex)
            if (last == -1) {
                return -1
            }

            // Make sure there is at least one dot after the first dot,
            // so www.something is not allowed, but www.something.co.uk is
            var pointer = last
            while (--pointer > beginIndex) {
                if (input[pointer] == '.' && pointer > beginIndex) {
                    return last
                }
            }

            return -1
        }

        private fun isAllowed(c: Char): Boolean {
            return c != '.' && !Scanners.isAlnum(c)
        }

        private fun isWww(input: CharSequence, triggerIndex: Int): Boolean {
            return input[triggerIndex + 1] == 'w' && input[triggerIndex + 2] == 'w' && input[triggerIndex + 3] == '.'
        }
    }
}
