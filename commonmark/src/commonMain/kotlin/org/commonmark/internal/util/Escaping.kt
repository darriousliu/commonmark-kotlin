package org.commonmark.internal.util


object Escaping {
    const val ESCAPABLE: String = "[!\"#$%&\'()*+,./:;<=>?@\\[\\\\\\]^_`{|}~-]"

    const val ENTITY: String = "&(?:#x[a-f0-9]{1,6}|#[0-9]{1,7}|[a-z][a-z0-9]{1,31});"

    private val BACKSLASH_OR_AMP = Regex("[\\\\&]")

    private val ENTITY_OR_ESCAPED_CHAR = Regex(
        "\\\\$ESCAPABLE|$ENTITY",
        RegexOption.IGNORE_CASE
    )

    // From RFC 3986 (see "reserved", "unreserved") except don't escape '[' or ']' to be compatible with JS encodeURI
    private val ESCAPE_IN_URI = Regex("(%[a-fA-F0-9]{0,2}|[^:/?#@!$&'()*+,;=a-zA-Z0-9\\-._~])")

    private val HEX_DIGITS =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    private val WHITESPACE = Regex("[ \t\r\n]+")

    private val UNESCAPE_REPLACER = object : Replacer {
        override fun replace(input: String, sb: StringBuilder) {
            if (input[0] == '\\') {
                sb.append(input, 1, input.length)
            } else {
                sb.append(Html5Entities.entityToString(input))
            }
        }
    }

    private val URI_REPLACER = object : Replacer {
        override fun replace(input: String, sb: StringBuilder) {
            if (input.startsWith("%")) {
                if (input.length == 3) {
                    // Already percent-encoded, preserve
                    sb.append(input)
                } else {
                    // %25 is the percent-encoding for %
                    sb.append("%25")
                    sb.append(input, 1, input.length)
                }
            } else {
                val bytes = input.encodeToByteArray()
                for (b in bytes) {
                    sb.append('%')
                    sb.append(HEX_DIGITS[(b.toInt() shr 4) and 0xF])
                    sb.append(HEX_DIGITS[b.toInt() and 0xF])
                }
            }
        }
    }

    fun escapeHtml(input: String): String {
        // Avoid building a new string in the majority of cases (nothing to escape)
        var sb: StringBuilder? = null

        loop@ for (i in 0..<input.length) {
            val c = input[i]
            val replacement = when (c) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '\"' -> "&quot;"
                else -> {
                    sb?.append(c)
                    continue@loop
                }
            }
            if (sb == null) {
                sb = StringBuilder()
                sb.append(input, 0, i)
            }
            sb.append(replacement)
        }

        return sb?.toString() ?: input
    }

    /**
     * Replace entities and backslash escapes with literal characters.
     */
    fun unescapeString(s: String): String {
        return if (BACKSLASH_OR_AMP.matches(s)) {
            replaceAll(ENTITY_OR_ESCAPED_CHAR, s, UNESCAPE_REPLACER)
        } else {
            s
        }
    }

    fun percentEncodeUrl(s: String): String {
        return replaceAll(ESCAPE_IN_URI, s, URI_REPLACER)
    }

    fun normalizeLabelContent(input: String): String {
        val trimmed = input.trim()

        // This is necessary to correctly case fold "\u1E9E" (LATIN CAPITAL LETTER SHARP S) to "SS":
        // "\u1E9E".toLowerCase(Locale.ROOT)  -> "\u00DF" (LATIN SMALL LETTER SHARP S)
        // "\u00DF".toUpperCase(Locale.ROOT)  -> "SS"
        // Note that doing upper first (or only upper without lower) wouldn't work because:
        // "\u1E9E".toUpperCase(Locale.ROOT)  -> "\u1E9E"
        val caseFolded = trimmed.lowercase().uppercase()

        return WHITESPACE.replace(caseFolded, " ")
    }

    private fun replaceAll(regex: Regex, s: String, replacer: Replacer): String {
        val matches = regex.findAll(s)

        if (!matches.any()) {
            return s
        }
        val sb = StringBuilder(s.length + 16)
        var lastEnd = 0

        for (match in matches) {
            sb.append(s, lastEnd, match.range.first)
            replacer.replace(match.value, sb)
            lastEnd = match.range.last + 1
        }

        if (lastEnd != s.length) {
            sb.append(s, lastEnd, s.length)
        }
        return sb.toString()
    }

    private interface Replacer {
        fun replace(input: String, sb: StringBuilder)
    }
}
