package org.nibor.autolink.internal

import org.nibor.autolink.LinkSpan
import org.nibor.autolink.LinkType

data class LinkSpanImpl(
    override val type: LinkType,
    override val beginIndex: Int,
    override val endIndex: Int
) : LinkSpan
