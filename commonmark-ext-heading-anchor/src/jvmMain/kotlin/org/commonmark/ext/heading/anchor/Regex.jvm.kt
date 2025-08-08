package org.commonmark.ext.heading.anchor

import java.util.regex.Pattern

actual fun createUnicodeCharacterRegex(): Regex {
    return Pattern.compile("[\\w\\-_]+", Pattern.UNICODE_CHARACTER_CLASS).toRegex()
}