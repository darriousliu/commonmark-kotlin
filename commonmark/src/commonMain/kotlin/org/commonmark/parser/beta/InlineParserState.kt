package org.commonmark.parser.beta

interface InlineParserState {
    /**
     * Return a scanner for the input for the current position (on the trigger character that the inline parser was
     * added for).
     *
     *
     * Note that this always returns the same instance, if you want to backtrack you need to use
     * [Scanner.position] and [Scanner.setPosition].
     */
    fun scanner(): Scanner
}
