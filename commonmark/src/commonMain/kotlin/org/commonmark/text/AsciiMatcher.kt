package org.commonmark.text

import org.commonmark.type.BitSetWrapper

/**
 * Char matcher that can match ASCII characters efficiently.
 */
class AsciiMatcher private constructor(builder: Builder) : CharMatcher {
    private val set: BitSetWrapper = builder.set

    override fun matches(c: Char): Boolean {
        return set[c.code]
    }

    fun newBuilder(): Builder {
        return Builder(set.clone())
    }

    class Builder internal constructor(internal val set: BitSetWrapper) {
        fun c(c: Char): Builder {
            require(c.code <= 127) { "Can only match ASCII characters" }
            set.set(c.code)
            return this
        }

        fun anyOf(s: String): Builder {
            for (i in 0..<s.length) {
                c(s[i])
            }
            return this
        }

        fun anyOf(characters: Set<Char>): Builder {
            for (c in characters) {
                c(c)
            }
            return this
        }

        fun range(from: Char, toInclusive: Char): Builder {
            var c = from
            while (c <= toInclusive) {
                c(c)
                c++
            }
            return this
        }

        fun build(): AsciiMatcher {
            return AsciiMatcher(this)
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder(BitSetWrapper())
        }

        fun builder(matcher: AsciiMatcher): Builder {
            return Builder(matcher.set.clone())
        }
    }
}
