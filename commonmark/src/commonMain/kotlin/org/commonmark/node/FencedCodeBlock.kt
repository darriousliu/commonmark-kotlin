package org.commonmark.node

/**
 * A fenced code block, e.g.:
 * <pre>
 * ```
 * foo
 * bar
 * ```
</pre> *
 *
 *
 *
 * @see [CommonMark Spec](https://spec.commonmark.org/0.31.2/.fenced-code-blocks)
 */
class FencedCodeBlock : Block() {
    /**
     * @return the fence character that was used, e.g. `` ` `` or `~`, if available, or null otherwise
     */
    var fenceCharacter: String? = null
    private var openingFenceLength: Int? = null
    private var closingFenceLength: Int? = null
    var fenceIndent: Int = 0

    /**
     * @see [CommonMark spec](http://spec.commonmark.org/0.31.2/.info-string)
     */
    var info: String? = null
    var literal: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    /**
     * @return the length of the opening fence (how many of {[.getFenceCharacter]} were used to start the code
     * block) if available, or null otherwise
     */
    fun getOpeningFenceLength(): Int? {
        return openingFenceLength
    }

    fun setOpeningFenceLength(openingFenceLength: Int?) {
        require(!(openingFenceLength != null && openingFenceLength < 3)) { "openingFenceLength needs to be >= 3" }
        checkFenceLengths(openingFenceLength, closingFenceLength)
        this.openingFenceLength = openingFenceLength
    }

    /**
     * @return the length of the closing fence (how many of [.getFenceCharacter] were used to end the code
     * block) if available, or null otherwise
     */
    fun getClosingFenceLength(): Int? {
        return closingFenceLength
    }

    fun setClosingFenceLength(closingFenceLength: Int?) {
        require(!(closingFenceLength != null && closingFenceLength < 3)) { "closingFenceLength needs to be >= 3" }
        checkFenceLengths(openingFenceLength, closingFenceLength)
        this.closingFenceLength = closingFenceLength
    }

    @get:Deprecated("use {@link #getFenceCharacter()} instead")
    @set:Deprecated("use {@link #setFenceCharacter} instead")
    var fenceChar: Char
        get() {
            val fenceCharacter = fenceCharacter
            return if (!fenceCharacter.isNullOrEmpty()) fenceCharacter.first() else '\u0000'
        }
        set(fenceChar) {
            this.fenceCharacter = if (fenceChar != '\u0000') fenceChar.toString() else null
        }

    @get:Deprecated("use {@link #getOpeningFenceLength} instead")
    @set:Deprecated("use {@link #setOpeningFenceLength} instead")
    var fenceLength: Int
        get() = openingFenceLength ?: 0
        set(fenceLength) {
            this.openingFenceLength = if (fenceLength != 0) fenceLength else null
        }

    companion object {
        private fun checkFenceLengths(openingFenceLength: Int?, closingFenceLength: Int?) {
            if (openingFenceLength != null && closingFenceLength != null) {
                require(closingFenceLength >= openingFenceLength) { "fence lengths required to be: closingFenceLength >= openingFenceLength" }
            }
        }
    }
}
