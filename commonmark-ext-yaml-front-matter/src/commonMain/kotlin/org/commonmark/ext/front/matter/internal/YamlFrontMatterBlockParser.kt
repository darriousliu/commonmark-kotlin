package org.commonmark.ext.front.matter.internal

import org.commonmark.ext.front.matter.YamlFrontMatterBlock
import org.commonmark.ext.front.matter.YamlFrontMatterNode
import org.commonmark.node.Document
import org.commonmark.parser.InlineParser
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.*

class YamlFrontMatterBlockParser : AbstractBlockParser() {
    private var inLiteral = false
    private var currentKey: String? = null
    private var currentValues: ArrayList<String> = ArrayList()
    override val block: YamlFrontMatterBlock = YamlFrontMatterBlock()

    override fun addLine(line: SourceLine) {
    }

    override fun tryContinue(parserState: ParserState): BlockContinue {
        val line = parserState.line.content

        if (REGEX_END.matches(line)) {
            currentKey?.let { block.appendChild(YamlFrontMatterNode(it, currentValues)) }
            return BlockContinue.finished()
        }

        val metadataMatch = REGEX_METADATA.matchEntire(line)
        if (metadataMatch != null) {
            currentKey?.let { block.appendChild(YamlFrontMatterNode(it, currentValues)) }

            inLiteral = false
            currentKey = metadataMatch.groupValues[1]
            currentValues = ArrayList()
            val value = metadataMatch.groupValues[2]
            if ("|" == value) {
                inLiteral = true
            } else if ("" != value) {
                currentValues.add(parseString(value))
            }

            return BlockContinue.atIndex(parserState.index)
        } else {
            if (inLiteral) {
                val literalMatch = REGEX_METADATA_LITERAL.matchEntire(line)
                if (literalMatch != null) {
                    val literalValue = literalMatch.groupValues[1].trim()
                    if (currentValues.size == 1) {
                        currentValues[0] = currentValues[0] + "\n" + literalValue
                    } else {
                        currentValues.add(literalValue)
                    }
                }
            } else {
                val listMatch = REGEX_METADATA_LIST.matchEntire(line)
                if (listMatch != null) {
                    val value = listMatch.groupValues[1]
                    currentValues.add(parseString(value))
                }
            }

            return BlockContinue.atIndex(parserState.index)
        }
    }

    override fun parseInlines(inlineParser: InlineParser) {
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser
        ): BlockStart? {
            val line = state.line.content
            val parentParser = matchedBlockParser.matchedBlockParser
            // check whether this line is the first line of whole document or not
            if (parentParser.block is Document && parentParser.block.firstChild == null &&
                REGEX_BEGIN.matchEntire(line) != null
            ) {
                return BlockStart.of(arrayOf(YamlFrontMatterBlockParser()))
                    .atIndex(state.nextNonSpaceIndex)
            }

            return BlockStart.none()
        }
    }

    companion object {
        private val REGEX_METADATA = Regex("^ {0,3}([A-Za-z0-9._-]+):\\s*(.*)")
        private val REGEX_METADATA_LIST = Regex("^ +-\\s*(.*)")
        private val REGEX_METADATA_LITERAL = Regex("^\\s*(.*)")
        private val REGEX_BEGIN = Regex("^-{3}(\\s.*)?")
        private val REGEX_END = Regex("^(-{3}|\\.{3})(\\s.*)?")

        private fun parseString(s: String): String {
            // Limited parsing of https://yaml.org/spec/1.2.2/#73-flow-scalar-styles
            // We assume input is well-formed and otherwise treat it as a plain string. In a real
            // parser, e.g. `'foo` would be invalid because it's missing a trailing `'`.
            if (s.startsWith("'") && s.endsWith("'")) {
                val inner = s.substring(1, s.length - 1)
                return inner.replace("''", "'")
            } else if (s.startsWith("\"") && s.endsWith("\"")) {
                val inner = s.substring(1, s.length - 1)
                // Only support escaped `\` and `"`, nothing else.
                return inner.replace("\\\"", "\"")
                    .replace("\\\\", "\\")
            } else {
                return s
            }
        }
    }
}
