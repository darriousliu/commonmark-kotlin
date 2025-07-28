package org.commonmark.internal.util

import org.commonmark.parser.beta.Scanner

object LinkScanner {
    /**
     * Attempt to scan the contents of a link label (inside the brackets), stopping after the content or returning false.
     * The stopped position can bei either the closing `]`, or the end of the line if the label continues on
     * the next line.
     */
    fun scanLinkLabelContent(scanner: Scanner): Boolean {
        while (scanner.hasNext()) {
            when (scanner.peek()) {
                '\\' -> {
                    scanner.next()
                    if (isEscapable(scanner.peek())) {
                        scanner.next()
                    }
                }

                ']' -> return true
                '[' ->                     // spec: Unescaped square bracket characters are not allowed inside the opening and closing
                    // square brackets of link labels.
                    return false

                else -> scanner.next()
            }
        }
        return true
    }

    /**
     * Attempt to scan a link destination, stopping after the destination or returning false.
     */
    fun scanLinkDestination(scanner: Scanner): Boolean {
        if (!scanner.hasNext()) {
            return false
        }

        if (scanner.next('<')) {
            while (scanner.hasNext()) {
                when (scanner.peek()) {
                    '\\' -> {
                        scanner.next()
                        if (isEscapable(scanner.peek())) {
                            scanner.next()
                        }
                    }

                    '\n', '<' -> return false
                    '>' -> {
                        scanner.next()
                        return true
                    }

                    else -> scanner.next()
                }
            }
            return false
        } else {
            return scanLinkDestinationWithBalancedParens(scanner)
        }
    }

    fun scanLinkTitle(scanner: Scanner): Boolean {
        if (!scanner.hasNext()) {
            return false
        }
        val endDelimiter: Char = when (scanner.peek()) {
            '"' -> '"'
            '\'' -> '\''
            '(' -> ')'
            else -> return false
        }
        scanner.next()

        if (!scanLinkTitleContent(scanner, endDelimiter)) {
            return false
        }
        if (!scanner.hasNext()) {
            return false
        }
        scanner.next()
        return true
    }

    fun scanLinkTitleContent(scanner: Scanner, endDelimiter: Char): Boolean {
        while (scanner.hasNext()) {
            val c: Char = scanner.peek()
            if (c == '\\') {
                scanner.next()
                if (isEscapable(scanner.peek())) {
                    scanner.next()
                }
            } else if (c == endDelimiter) {
                return true
            } else if (endDelimiter == ')' && c == '(') {
                // unescaped '(' in the title within parens is invalid
                return false
            } else {
                scanner.next()
            }
        }
        return true
    }

    // spec: a nonempty sequence of characters that does not start with <, does not include ASCII space or control
    // characters, and includes parentheses only if (a) they are backslash-escaped or (b) they are part of a balanced
    // pair of unescaped parentheses
    private fun scanLinkDestinationWithBalancedParens(scanner: Scanner): Boolean {
        var parens = 0
        var empty = true
        while (scanner.hasNext()) {
            val c: Char = scanner.peek()
            when (c) {
                ' ' -> return !empty
                '\\' -> {
                    scanner.next()
                    if (isEscapable(scanner.peek())) {
                        scanner.next()
                    }
                }

                '(' -> {
                    parens++
                    // Limit to 32 nested parens for pathological cases
                    if (parens > 32) {
                        return false
                    }
                    scanner.next()
                }

                ')' -> {
                    if (parens == 0) {
                        return true
                    } else {
                        parens--
                    }
                    scanner.next()
                }

                else -> {
                    // or control character
                    if (c.isISOControl()) {
                        return !empty
                    }
                    scanner.next()
                }
            }
            empty = false
        }
        return true
    }

    private fun isEscapable(c: Char): Boolean {
        when (c) {
            '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~' -> return true
        }
        return false
    }
}
