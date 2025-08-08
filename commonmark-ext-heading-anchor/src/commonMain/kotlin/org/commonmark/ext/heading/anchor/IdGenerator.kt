package org.commonmark.ext.heading.anchor

/**
 * Generates strings to be used as identifiers.
 *
 *
 * Use [.builder] to create an instance.
 */
class IdGenerator private constructor(builder: Builder) {
    private val allowedCharacters: Regex = compileAllowedCharactersPattern()
    private val identityMap: HashMap<String, Int>
    private val prefix: String
    private val suffix: String
    private val defaultIdentifier: String

    init {
        this.defaultIdentifier = builder.defaultIdentifier
        this.prefix = builder.prefix
        this.suffix = builder.suffix
        this.identityMap = HashMap()
    }

    /**
     *
     *
     * Generate an ID based on the provided text and previously generated IDs.
     *
     *
     * This method is not thread safe, concurrent calls can end up
     * with non-unique identifiers.
     *
     *
     * Note that collision can occur in the case that
     *
     *  * Method called with 'X'
     *  * Method called with 'X' again
     *  * Method called with 'X-1'
     *
     *
     *
     * In that case, the three generated IDs will be:
     *
     *  * X
     *  * X-1
     *  * X-1
     *
     *
     *
     * Therefore if collisions are unacceptable you should ensure that
     * numbers are stripped from end of `text`.
     *
     * @param text Text that the identifier should be based on. Will be normalised, then used to generate the
     * identifier.
     * @return `text` if this is the first instance that the `text` has been passed
     * to the method. Otherwise, `text + "-" + X` will be returned, where X is the number of times
     * that `text` has previously been passed in. If `text` is empty, the default
     * identifier given in the constructor will be used.
     */
    fun generateId(text: String?): String? {
        var normalizedIdentity = if (text != null) normalizeText(text) else defaultIdentifier

        if (normalizedIdentity.isEmpty()) {
            normalizedIdentity = defaultIdentifier
        }

        if (!identityMap.containsKey(normalizedIdentity)) {
            identityMap.put(normalizedIdentity, 1)
            return prefix + normalizedIdentity + suffix
        } else {
            val currentCount: Int = identityMap[normalizedIdentity]!!
            identityMap.put(normalizedIdentity, currentCount + 1)
            return (prefix + normalizedIdentity) + "-" + currentCount + suffix
        }
    }

    /**
     * Assume we've been given a space separated text.
     *
     * @param text Text to normalize to an ID
     */
    private fun normalizeText(text: String): String {
        val firstPassNormalising = text.lowercase().replace(" ", "-")
        return allowedCharacters.findAll(firstPassNormalising)
            .joinToString(separator = "") { it.value }
    }

    class Builder {
        internal var defaultIdentifier = "id"
        internal var prefix: String = ""
        internal var suffix: String = ""

        fun build(): IdGenerator {
            return IdGenerator(this)
        }

        /**
         * @param defaultId the default identifier to use in case the provided text is empty or only contains unusable characters
         * @return `this`
         */
        fun defaultId(defaultId: String): Builder {
            this.defaultIdentifier = defaultId
            return this
        }

        /**
         * @param prefix the text to place before the generated identity
         * @return `this`
         */
        fun prefix(prefix: String): Builder {
            this.prefix = prefix
            return this
        }

        /**
         * @param suffix the text to place after the generated identity
         * @return `this`
         */
        fun suffix(suffix: String): Builder {
            this.suffix = suffix
            return this
        }
    }

    companion object {
        /**
         * @return a new builder with default arguments
         */
        fun builder(): Builder {
            return Builder()
        }

        private fun compileAllowedCharactersPattern(): Regex {
            return try {
                createUnicodeCharacterRegex()
            } catch (_: IllegalArgumentException) {
                // Android only supports the flag in API level 24. But it actually uses Unicode character classes by
                // default, so not specifying the flag is ok. See issue #71.
                Regex("[\\w\\-_]+")
            }
        }

    }
}
