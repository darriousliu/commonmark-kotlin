package org.commonmark.renderer.html

import kotlin.jvm.JvmOverloads

/**
 *
 * Allows http, https, mailto, and data protocols for url.
 * Also allows protocol relative urls, and relative urls.
 * Implementation based on https://github.com/OWASP/java-html-sanitizer/blob/f07e44b034a45d94d6fd010279073c38b6933072/src/main/java/org/owasp/html/FilterUrlByProtocolAttributePolicy.java
 */
class DefaultUrlSanitizer @JvmOverloads constructor(
    protocols: Collection<String> = listOf(
        "http",
        "https",
        "mailto",
        "data"
    )
) : UrlSanitizer {
    private val protocols: Set<String?> = HashSet(protocols)

    override fun sanitizeLinkUrl(url: String): String {
        var url = url
        url = stripHtmlSpaces(url)
        var i = 0
        val n = url.length
        protocol_loop@ while (i < n) {
            when (url[i]) {
                '/', '#', '?' -> break@protocol_loop
                ':' -> {
                    val protocol: String? = url.substring(0, i).lowercase()
                    if (!protocols.contains(protocol)) {
                        return ""
                    }
                    break@protocol_loop
                }
            }
            ++i
        }
        return url
    }


    override fun sanitizeImageUrl(url: String): String {
        return sanitizeLinkUrl(url)
    }

    private fun stripHtmlSpaces(s: String): String {
        var i = 0
        var n: Int = s.length
        while (n > i) {
            if (!isHtmlSpace(s[n - 1].code)) {
                break
            }
            --n
        }
        while (i < n) {
            if (!isHtmlSpace(s[i].code)) {
                break
            }
            ++i
        }
        if (i == 0 && n == s.length) {
            return s
        }
        return s.substring(i, n)
    }

    private fun isHtmlSpace(ch: Int): Boolean {
        return when (ch) {
            ' '.code, '\t'.code, '\n'.code, '\u000c'.code, '\r'.code -> true
            else -> false
        }
    }
}
