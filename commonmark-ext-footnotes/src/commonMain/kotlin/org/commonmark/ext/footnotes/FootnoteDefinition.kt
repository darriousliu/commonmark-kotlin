package org.commonmark.ext.footnotes

import org.commonmark.node.CustomBlock

/**
 * A footnote definition, e.g.:
 * <pre>`
 * [^foo]: This is the footnote text
`</pre> *
 * The [label][.getLabel] is the text in brackets after `^`, so `foo` in the example. The contents
 * of the footnote are child nodes of the definition, a [org.commonmark.node.Paragraph] in the example.
 *
 *
 * Footnote definitions are parsed even if there's no corresponding [FootnoteReference].
 */
class FootnoteDefinition(val label: String) : CustomBlock()

