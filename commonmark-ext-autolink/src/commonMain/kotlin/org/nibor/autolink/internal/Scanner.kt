package org.nibor.autolink.internal

import org.nibor.autolink.LinkSpan

interface Scanner {
    /**
     * @param input input text
     * @param triggerIndex the index at which the trigger character for this scanner was
     * @param rewindIndex the index that can maximally be rewound to (either the very first character of the input or
     * the character after the last matched link)
     * need to be set to be set here
     * @return the matched link, or `null` if no link matched
     */
    fun scan(input: CharSequence, triggerIndex: Int, rewindIndex: Int): LinkSpan?
}
