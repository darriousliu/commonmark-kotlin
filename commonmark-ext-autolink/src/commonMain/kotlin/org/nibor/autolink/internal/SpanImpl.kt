package org.nibor.autolink.internal

import org.nibor.autolink.Span

data class SpanImpl(
    override val beginIndex: Int,
    override val endIndex: Int
) : Span