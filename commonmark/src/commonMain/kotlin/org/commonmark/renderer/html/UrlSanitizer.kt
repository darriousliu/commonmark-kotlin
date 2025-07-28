package org.commonmark.renderer.html

/**
 * Sanitizes urls for img and a elements by whitelisting protocols.
 * This is intended to prevent XSS payloads like [Click this totally safe url](javascript:document.xss=true;)
 *
 *
 * Implementation based on https://github.com/OWASP/java-html-sanitizer/blob/f07e44b034a45d94d6fd010279073c38b6933072/src/main/java/org/owasp/html/FilterUrlByProtocolAttributePolicy.java
 *
 * @since 0.14.0
 */
interface UrlSanitizer {
    /**
     * Sanitize a url for use in the href attribute of a [org.commonmark.node.Link].
     *
     * @param url Link to sanitize
     * @return Sanitized link
     */
    fun sanitizeLinkUrl(url: String): String

    /**
     * Sanitize a url for use in the src attribute of a [org.commonmark.node.Image].
     *
     * @param url Link to sanitize
     * @return Sanitized link [org.commonmark.node.Image]
     */
    fun sanitizeImageUrl(url: String): String
}
