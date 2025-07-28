package org.commonmark.internal.inline

class AsteriskDelimiterProcessor(
    override val openingCharacter: Char = '*'
) : EmphasisDelimiterProcessor(openingCharacter)
