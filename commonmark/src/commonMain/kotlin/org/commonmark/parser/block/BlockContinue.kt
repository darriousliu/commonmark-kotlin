package org.commonmark.parser.block

import org.commonmark.internal.BlockContinueImpl

/**
 * Result object for continuing parsing of a block, see static methods for constructors.
 */
open class BlockContinue {
    companion object {
        fun none(): BlockContinue? {
            return null
        }

        fun atIndex(newIndex: Int): BlockContinue {
            return BlockContinueImpl(newIndex, -1, false)
        }

        fun atColumn(newColumn: Int): BlockContinue {
            return BlockContinueImpl(-1, newColumn, false)
        }

        fun finished(): BlockContinue {
            return BlockContinueImpl(-1, -1, true)
        }
    }
}
