package org.commonmark.internal

import org.commonmark.parser.block.BlockContinue

class BlockContinueImpl(
    val newIndex: Int,
    val newColumn: Int,
    val isFinalize: Boolean
) : BlockContinue()
