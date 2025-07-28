package org.commonmark.internal

import org.commonmark.node.HtmlBlock
import org.commonmark.node.Paragraph
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState

class HtmlBlockParser private constructor(private val closingPattern: Regex?) :
    AbstractBlockParser() {
    override val block: HtmlBlock = HtmlBlock()

    private var finished = false
    private var content: BlockContent? = BlockContent()

    override fun tryContinue(parserState: ParserState): BlockContinue? {
        if (finished) {
            return BlockContinue.none()
        }

        // Blank line ends type 6 and type 7 blocks
        return if (parserState.isBlank && closingPattern == null) {
            BlockContinue.none()
        } else {
            BlockContinue.atIndex(parserState.index)
        }
    }

    override fun addLine(line: SourceLine) {
        content?.add(line.content)

        if (closingPattern != null && closingPattern.matches(line.content)) {
            finished = true
        }
    }

    override fun closeBlock() {
        block.literal = content?.string
        content = null
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser
        ): BlockStart? {
            val nextNonSpace: Int = state.nextNonSpaceIndex
            val line: CharSequence = state.line.content

            if (state.indent < 4 && line[nextNonSpace] == '<') {
                for (blockType in 1..7) {
                    // Type 7 cannot interrupt a paragraph (not even a lazy one)
                    if (blockType == 7 && (matchedBlockParser.matchedBlockParser.block is Paragraph ||
                                state.activeBlockParser.canHaveLazyContinuationLines())
                    ) {
                        continue
                    }
                    val opener = BLOCK_PATTERNS[blockType][0]
                    val closer = BLOCK_PATTERNS[blockType][1]
                    val matches =
                        opener?.matches(line.subSequence(nextNonSpace, line.length)) == true
                    if (matches) {
                        return BlockStart.of(arrayOf(HtmlBlockParser(closer))).atIndex(state.index)
                    }
                }
            }
            return BlockStart.none()
        }
    }

    companion object {
        private const val TAGNAME = "[A-Za-z][A-Za-z0-9-]*"
        private const val ATTRIBUTENAME = "[a-zA-Z_:][a-zA-Z0-9:._-]*"
        private const val UNQUOTEDVALUE = "[^\"'=<>`\\x00-\\x20]+"
        private const val SINGLEQUOTEDVALUE = "'[^']*'"
        private const val DOUBLEQUOTEDVALUE = "\"[^\"]*\""
        private const val ATTRIBUTEVALUE =
            "(?:$UNQUOTEDVALUE|$SINGLEQUOTEDVALUE|$DOUBLEQUOTEDVALUE)"
        private const val ATTRIBUTEVALUESPEC = "(?:\\s*=\\s*$ATTRIBUTEVALUE)"
        private const val ATTRIBUTE = "(?:\\s+$ATTRIBUTENAME$ATTRIBUTEVALUESPEC?)"

        private const val OPENTAG = "<$TAGNAME$ATTRIBUTE*\\s*/?>"
        private const val CLOSETAG = "</$TAGNAME\\s*[>]"

        private val BLOCK_PATTERNS: Array<Array<Regex?>> = arrayOf<Array<Regex?>>(
            arrayOf(null, null),  // not used (no type 0)
            arrayOf(
                Regex("^<(?:script|pre|style|textarea)(?:\\s|>|$)", RegexOption.IGNORE_CASE),
                Regex("</(?:script|pre|style|textarea)>", RegexOption.IGNORE_CASE)
            ),
            arrayOf(
                Regex("^<!--"),
                Regex("-->")
            ),
            arrayOf(
                Regex("^<[?]"),
                Regex("\\?>")
            ),
            arrayOf(
                Regex("^<![A-Z]"),
                Regex(">")
            ),
            arrayOf(
                Regex("^<!\\[CDATA\\["),
                Regex("]]>")
            ),
            arrayOf(
                Regex(
                    "^</?(?:" +
                            "address|article|aside|" +
                            "base|basefont|blockquote|body|" +
                            "caption|center|col|colgroup|" +
                            "dd|details|dialog|dir|div|dl|dt|" +
                            "fieldset|figcaption|figure|footer|form|frame|frameset|" +
                            "h1|h2|h3|h4|h5|h6|head|header|hr|html|" +
                            "iframe|" +
                            "legend|li|link|" +
                            "main|menu|menuitem|" +
                            "nav|noframes|" +
                            "ol|optgroup|option|" +
                            "p|param|" +
                            "search|section|summary|" +
                            "table|tbody|td|tfoot|th|thead|title|tr|track|" +
                            "ul" +
                            ")(?:\\s|/?>|$)", RegexOption.IGNORE_CASE
                ),
                null // terminated by a blank line
            ),
            arrayOf(
                Regex(
                    "^(?:$OPENTAG|$CLOSETAG)\\s*$",
                    RegexOption.IGNORE_CASE
                ),
                null // terminated by a blank line
            )
        )
    }
}
