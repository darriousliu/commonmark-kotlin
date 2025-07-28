package org.nibor.autolink

/**
 * Type of extracted link.
 */
enum class LinkType {
    /**
     * URL such as `http://example.com`
     */
    URL,

    /**
     * Email address such as `foo@example.com`
     */
    EMAIL,

    /**
     * URL such as `www.example.com`
     */
    WWW
}
