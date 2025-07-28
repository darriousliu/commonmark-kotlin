package org.nibor.autolink

import org.nibor.autolink.internal.EmailScanner
import org.nibor.autolink.internal.Scanner
import org.nibor.autolink.internal.SpanImpl
import org.nibor.autolink.internal.UrlScanner
import org.nibor.autolink.internal.WwwScanner

/**
 * Extracts links from input.
 *
 *
 * Create and configure an extractor using [.builder], then call [.extractLinks].
 */
class LinkExtractor private constructor(
    private val urlScanner: UrlScanner?,
    private val wwwScanner: WwwScanner?,
    private val emailScanner: EmailScanner?
) {

    /**
     * Extract the links from the input text. Can be called multiple times with different inputs (thread-safe).
     *
     * @param input the input text, must not be null
     * @return a lazy iterable for the links in order that they appear in the input, never null
     * @see .extractSpans
     */
    fun extractLinks(input: CharSequence?): Iterable<LinkSpan> {
        if (input == null) {
            throw NullPointerException("input must not be null")
        }
        return object : Iterable<LinkSpan> {
            override fun iterator(): Iterator<LinkSpan> {
                return LinkIterator(input)
            }
        }
    }

    /**
     * Extract spans from the input text. A span is a substring of the input and represents either a link
     * (see [LinkSpan]) or plain text outside a link.
     *
     *
     * Using this is more convenient than [.extractLinks] if you want to transform the whole input text to
     * a different format.
     *
     * @param input the input text, must not be null
     * @return a lazy iterable for the spans in order that they appear in the input, never null
     */
    fun extractSpans(input: CharSequence?): Iterable<Span> {
        if (input == null) {
            throw NullPointerException("input must not be null")
        }
        return object : Iterable<Span> {
            override fun iterator(): Iterator<Span> {
                return SpanIterator(input, LinkIterator(input))
            }
        }
    }

    private fun trigger(c: Char): Scanner? {
        return when (c) {
            ':' -> urlScanner
            '@' -> emailScanner
            'w' -> wwwScanner
            else -> null
        }
    }

    /**
     * Builder for configuring link extractor.
     */
    class Builder internal constructor() {
        private var linkTypes: Set<LinkType> = LinkType.entries.toSet()
        private var emailDomainMustHaveDot = true

        /**
         * @param linkTypes the link types that should be extracted (by default, all types are extracted)
         * @return this builder
         */
        fun linkTypes(linkTypes: Set<LinkType>?): Builder {
            if (linkTypes == null) {
                throw NullPointerException("linkTypes must not be null")
            }
            this.linkTypes = HashSet(linkTypes)
            return this
        }

        /**
         * @param emailDomainMustHaveDot true if the domain in an email address is required to have more than one part,
         * false if it can also just have single part (e.g. foo@com); true by default
         * @return this builder
         */
        fun emailDomainMustHaveDot(emailDomainMustHaveDot: Boolean): Builder {
            this.emailDomainMustHaveDot = emailDomainMustHaveDot
            return this
        }

        /**
         * @return the configured link extractor
         */
        fun build(): LinkExtractor {
            val urlScanner = if (linkTypes.contains(LinkType.URL)) UrlScanner() else null
            val wwwScanner = if (linkTypes.contains(LinkType.WWW)) WwwScanner() else null
            val emailScanner = if (linkTypes.contains(LinkType.EMAIL)) EmailScanner(
                emailDomainMustHaveDot
            ) else null
            return LinkExtractor(urlScanner, wwwScanner, emailScanner)
        }
    }

    inner class LinkIterator(private val input: CharSequence) :
        Iterator<LinkSpan> {
        private var next: LinkSpan? = null
        private var index = 0
        private var rewindIndex = 0

        override fun hasNext(): Boolean {
            setNext()
            return next != null
        }

        override fun next(): LinkSpan {
            if (hasNext()) {
                val link = next
                next = null
                return link!!
            } else {
                throw NoSuchElementException()
            }
        }

        fun remove() {
            throw UnsupportedOperationException("remove")
        }

        fun setNext() {
            if (next != null) {
                return
            }

            val length = input.length
            while (index < length) {
                val scanner = trigger(input[index])
                if (scanner != null) {
                    val link = scanner.scan(input, index, rewindIndex)
                    if (link != null) {
                        next = link
                        index = link.endIndex
                        rewindIndex = index
                        break
                    } else {
                        index++
                    }
                } else {
                    index++
                }
            }
        }
    }

    private inner class SpanIterator(
        private val input: CharSequence,
        private val linkIterator: LinkIterator
    ) : Iterator<Span> {
        private var index = 0
        private var nextLink: LinkSpan? = null

        override fun hasNext(): Boolean {
            return index < input.length
        }

        fun nextTextSpan(endIndex: Int): Span {
            val span = SpanImpl(index, endIndex)
            index = endIndex
            return span
        }

        override fun next(): Span {
            if (!hasNext()) {
                throw NoSuchElementException()
            }

            if (nextLink == null) {
                if (linkIterator.hasNext()) {
                    nextLink = linkIterator.next()
                } else {
                    return nextTextSpan(input.length)
                }
            }
            val nextLink = nextLink!!

            if (index < nextLink.beginIndex) {
                // text before link, return plain
                return nextTextSpan(nextLink.beginIndex)
            } else {
                // at link, return it and make sure we continue after it next time
                val span = nextLink
                index = nextLink.endIndex
                this.nextLink = null
                return span
            }
        }

        fun remove() {
            throw UnsupportedOperationException("remove")
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }
    }
}
