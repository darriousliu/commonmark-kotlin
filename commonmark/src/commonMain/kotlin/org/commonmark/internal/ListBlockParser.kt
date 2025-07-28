package org.commonmark.internal

import org.commonmark.internal.util.Parsing
import org.commonmark.node.*
import org.commonmark.parser.block.*

class ListBlockParser(override val block: ListBlock) : AbstractBlockParser() {

    private var hadBlankLine = false
    private var linesAfterBlank = 0

    override val isContainer: Boolean
        get() = true

    override fun canContain(childBlock: Block): Boolean {
        if (childBlock is ListItem) {
            // Another list item is added to this list block. If the previous line was blank, that means this list block
            // is "loose" (not tight).
            //
            // spec: A list is loose if blank lines separate any of its constituent list items
            if (hadBlankLine && linesAfterBlank == 1) {
                block.isTight = false
                hadBlankLine = false
            }
            return true
        } else {
            return false
        }
    }

    override fun tryContinue(parserState: ParserState): BlockContinue {
        if (parserState.isBlank) {
            hadBlankLine = true
            linesAfterBlank = 0
        } else if (hadBlankLine) {
            linesAfterBlank++
        }
        // List blocks themselves don't have any markers, only list items. So try to stay in the list.
        // If there is a block start other than the list item, canContain makes sure that this list is closed.
        return BlockContinue.atIndex(parserState.index)
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser
        ): BlockStart? {
            val matched = matchedBlockParser.matchedBlockParser

            if (state.indent >= Parsing.CODE_BLOCK_INDENT) {
                return BlockStart.none()
            }
            val markerIndex = state.nextNonSpaceIndex
            val markerColumn = state.column + state.indent
            val inParagraph = !matchedBlockParser.paragraphLines.isEmpty
            val listData = parseList(
                state.line.content,
                markerIndex,
                markerColumn,
                inParagraph
            )
            if (listData == null) {
                return BlockStart.none()
            }

            val newColumn = listData.contentColumn
            val listItemParser = ListItemParser(state.indent, newColumn - state.column)

            // prepend the list block if needed
            if (matched !is ListBlockParser ||
                !(listsMatch(
                    matched.block,
                    listData.listBlock
                ))
            ) {
                val listBlockParser = ListBlockParser(listData.listBlock)
                // We start out with assuming a list is tight. If we find a blank line, we set it to lose later.
                listData.listBlock.isTight = true

                return BlockStart.of(arrayOf(listBlockParser, listItemParser)).atColumn(newColumn)
            } else {
                return BlockStart.of(arrayOf(listItemParser)).atColumn(newColumn)
            }
        }
    }

    private class ListData(
        val listBlock: ListBlock,
        val contentColumn: Int
    )

    private class ListMarkerData(
        val listBlock: ListBlock,
        val indexAfterMarker: Int
    )

    companion object {
        /**
         * Parse a list marker and return data on the marker or null.
         */
        private fun parseList(
            line: CharSequence, markerIndex: Int, markerColumn: Int,
            inParagraph: Boolean
        ): ListData? {
            val listMarker = parseListMarker(line, markerIndex)
            if (listMarker == null) {
                return null
            }
            val listBlock = listMarker.listBlock

            val indexAfterMarker = listMarker.indexAfterMarker
            val markerLength = indexAfterMarker - markerIndex
            // marker doesn't include tabs, so counting them as columns directly is ok
            val columnAfterMarker = markerColumn + markerLength
            // the column within the line where the content starts
            var contentColumn = columnAfterMarker

            // See at which column the content starts if there is content
            var hasContent = false
            val length = line.length
            for (i in indexAfterMarker..<length) {
                val c = line[i]
                if (c == '\t') {
                    contentColumn += Parsing.columnsToNextTabStop(contentColumn)
                } else if (c == ' ') {
                    contentColumn++
                } else {
                    hasContent = true
                    break
                }
            }

            if (inParagraph) {
                // If the list item is ordered, the start number must be 1 to interrupt a paragraph.
                if (listBlock is OrderedList && listBlock.markerStartNumber != 1) {
                    return null
                }
                // Empty list item cannot interrupt a paragraph.
                if (!hasContent) {
                    return null
                }
            }

            if (!hasContent || (contentColumn - columnAfterMarker) > Parsing.CODE_BLOCK_INDENT) {
                // If this line is blank or has a code block, default to 1 space after marker
                contentColumn = columnAfterMarker + 1
            }

            return ListData(listBlock, contentColumn)
        }

        private fun parseListMarker(line: CharSequence, index: Int): ListMarkerData? {
            val c = line[index]
            when (c) {
                '-', '+', '*' -> if (isSpaceTabOrEnd(
                        line,
                        index + 1
                    )
                ) {
                    val bulletList = BulletList()
                    bulletList.marker = c.toString()
                    return ListMarkerData(bulletList, index + 1)
                } else {
                    return null
                }

                else -> return parseOrderedList(line, index)
            }
        }

        // spec: An ordered list marker is a sequence of 1-9 arabic digits (0-9), followed by either a `.` character or a
        // `)` character.
        private fun parseOrderedList(line: CharSequence, index: Int): ListMarkerData? {
            var digits = 0
            val length = line.length
            for (i in index..<length) {
                val c = line[i]
                when (c) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        digits++
                        if (digits > 9) {
                            return null
                        }
                    }

                    '.', ')' -> if (digits >= 1 && isSpaceTabOrEnd(
                            line,
                            i + 1
                        )
                    ) {
                        val number = line.subSequence(index, i).toString()
                        val orderedList = OrderedList()
                        orderedList.markerStartNumber = number.toIntOrNull()
                        orderedList.markerDelimiter = c.toString()
                        return ListMarkerData(orderedList, i + 1)
                    } else {
                        return null
                    }

                    else -> return null
                }
            }
            return null
        }

        private fun isSpaceTabOrEnd(line: CharSequence, index: Int): Boolean {
            return if (index < line.length) {
                when (line[index]) {
                    ' ', '\t' -> true
                    else -> false
                }
            } else {
                true
            }
        }

        /**
         * Returns true if the two list items are of the same type,
         * with the same delimiter and bullet character. This is used
         * in agglomerating list items into lists.
         */
        private fun listsMatch(a: ListBlock, b: ListBlock): Boolean {
            if (a is BulletList && b is BulletList) {
                return a.marker == b.marker
            } else if (a is OrderedList && b is OrderedList) {
                return a.markerDelimiter == b.markerDelimiter
            }
            return false
        }
    }
}
