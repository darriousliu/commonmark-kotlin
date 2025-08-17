package org.commonmark.ext.heading.anchor

actual fun createUnicodeCharacterRegex(): Regex {
    return Regex("[\\p{L}\\p{N}_]+")
}