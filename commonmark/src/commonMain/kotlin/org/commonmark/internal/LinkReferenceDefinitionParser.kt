package org.commonmark.internal

import org.commonmark.internal.util.Escaping
import org.commonmark.internal.util.LinkScanner
import org.commonmark.node.LinkReferenceDefinition
import org.commonmark.node.SourceSpan
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.parser.beta.Scanner
import kotlin.math.max

/**
 * Parser for link reference definitions at the beginning of a paragraph.
 *
 * @see [Link reference definitions](https://spec.commonmark.org/0.31.2/.link-reference-definitions)
 */
class LinkReferenceDefinitionParser {
    private var state: State = State.START_DEFINITION

    private val paragraphLines: MutableList<SourceLine> = mutableListOf()
    private val definitions: MutableList<LinkReferenceDefinition> = mutableListOf()
    private val sourceSpans: MutableList<SourceSpan> = mutableListOf()

    private var label: StringBuilder? = null
    private var destination: String? = null
    private var titleDelimiter = 0.toChar()
    private var title: StringBuilder? = null
    private var referenceValid = false

    fun parse(line: SourceLine) {
        paragraphLines.add(line)
        if (state == State.PARAGRAPH) {
            // We're in a paragraph now. Link reference definitions can only appear at the beginning, so once
            // we're in a paragraph, there's no going back.
            return
        }

        val scanner = Scanner.of(SourceLines.of(line))
        while (scanner.hasNext()) {
            val success: Boolean
            when (state) {
                State.START_DEFINITION -> {
                    success = startDefinition(scanner)
                }

                State.LABEL -> {
                    success = label(scanner)
                }

                State.DESTINATION -> {
                    success = destination(scanner)
                }

                State.START_TITLE -> {
                    success = startTitle(scanner)
                }

                State.TITLE -> {
                    success = title(scanner)
                }

                else -> {
                    throw IllegalStateException("Unknown parsing state: $state")
                }
            }
            // Parsing failed, which means we fall back to treating text as a paragraph.
            if (!success) {
                state = State.PARAGRAPH
                // If parsing of the title part failed, we still have a valid reference that we can add, and we need to
                // do it before the source span for this line is added.
                finishReference()
                return
            }
        }
    }

    fun addSourceSpan(sourceSpan: SourceSpan) {
        sourceSpans.add(sourceSpan)
    }

    /**
     * @return the lines that are normal paragraph content, without newlines
     */
    fun getParagraphLines(): SourceLines {
        return SourceLines.of(paragraphLines)
    }

    val paragraphSourceSpans: List<SourceSpan>
        get() = sourceSpans

    fun getDefinitions(): List<LinkReferenceDefinition> {
        finishReference()
        return definitions
    }

    fun removeLines(lines: Int): List<SourceSpan> {
        val removedSpans = buildList {
            addAll(sourceSpans.subList(max(sourceSpans.size - lines, 0), sourceSpans.size))
        }
        removeLast(lines, paragraphLines)
        removeLast(lines, sourceSpans)
        return removedSpans
    }

    private fun startDefinition(scanner: Scanner): Boolean {
        // Finish any outstanding references now. We don't do this earlier because we need addSourceSpan to have been
        // called before we do it.
        finishReference()

        scanner.whitespace()
        if (!scanner.next('[')) {
            return false
        }

        state = State.LABEL
        val label = StringBuilder().also { label = it }

        if (!scanner.hasNext()) {
            label.append('\n')
        }
        return true
    }

    private fun label(scanner: Scanner): Boolean {
        val start = scanner.position()
        if (!LinkScanner.scanLinkLabelContent(scanner)) {
            return false
        }
        val label = label ?: return false

        label.append(scanner.getSource(start, scanner.position()).content)

        if (!scanner.hasNext()) {
            // the label might continue on the next line
            label.append('\n')
            return true
        } else if (scanner.next(']')) {
            // end of label
            if (!scanner.next(':')) {
                return false
            }

            // spec: A link label can have at most 999 characters inside the square brackets.
            if (label.length > 999) {
                return false
            }

            val normalizedLabel: String = Escaping.normalizeLabelContent(label.toString())
            if (normalizedLabel.isEmpty()) {
                return false
            }

            state = State.DESTINATION

            scanner.whitespace()
            return true
        } else {
            return false
        }
    }

    private fun destination(scanner: Scanner): Boolean {
        scanner.whitespace()
        val start = scanner.position()
        if (!LinkScanner.scanLinkDestination(scanner)) {
            return false
        }

        val rawDestination = scanner.getSource(start, scanner.position()).content
        destination = if (rawDestination.startsWith("<")) rawDestination.substring(
            1,
            rawDestination.length - 1
        ) else rawDestination

        val whitespace = scanner.whitespace()
        if (!scanner.hasNext()) {
            // Destination was at end of line, so this is a valid reference for sure (and maybe a title).
            // If not at the end of the line, wait for the title to be valid first.
            referenceValid = true
            paragraphLines.clear()
        } else if (whitespace == 0) {
            // spec: The title must be separated from the link destination by whitespace
            return false
        }

        state = State.START_TITLE
        return true
    }

    private fun startTitle(scanner: Scanner): Boolean {
        scanner.whitespace()
        if (!scanner.hasNext()) {
            state = State.START_DEFINITION
            return true
        }

        titleDelimiter = '\u0000'
        val c = scanner.peek()
        when (c) {
            '"', '\'' -> titleDelimiter = c
            '(' -> titleDelimiter = ')'
        }

        if (titleDelimiter != '\u0000') {
            state = State.TITLE
            val title = StringBuilder().also { title = it }
            scanner.next()
            if (!scanner.hasNext()) {
                title.append('\n')
            }
        } else {
            // There might be another reference instead, try that for the same character.
            state = State.START_DEFINITION
        }
        return true
    }

    private fun title(scanner: Scanner): Boolean {
        val start = scanner.position()
        if (!LinkScanner.scanLinkTitleContent(scanner, titleDelimiter)) {
            // Invalid title, stop. Title collected so far must not be used.
            title = null
            return false
        }
        val title = title ?: return false

        title.append(scanner.getSource(start, scanner.position()).content)

        if (!scanner.hasNext()) {
            // Title ran until the end of the line, so continue on the next line (until we find the delimiter)
            title.append('\n')
            return true
        }

        // Skip delimiter character
        scanner.next()
        scanner.whitespace()
        if (scanner.hasNext()) {
            // spec: No further non-whitespace characters may occur on the line.
            // Title collected so far must not be used.
            this.title = null
            return false
        }
        referenceValid = true
        paragraphLines.clear()

        // See if there's another definition.
        state = State.START_DEFINITION
        return true
    }

    private fun finishReference() {
        if (!referenceValid) {
            return
        }

        val d = Escaping.unescapeString(destination.orEmpty())
        val t = if (title != null) Escaping.unescapeString(title.toString()) else null
        val definition = LinkReferenceDefinition(label.toString(), d, t)
        definition.setSourceSpans(sourceSpans)
        sourceSpans.clear()
        definitions.add(definition)

        label = null
        referenceValid = false
        destination = null
        title = null
    }

    internal enum class State {
        // Looking for the start of a definition, i.e. `[`
        START_DEFINITION,

        // Parsing the label, i.e. `foo` within `[foo]`
        LABEL,

        // Parsing the destination, i.e. `/url` in `[foo]: /url`
        DESTINATION,

        // Looking for the start of a title, i.e. the first `"` in `[foo]: /url "title"`
        START_TITLE,

        // Parsing the content of the title, i.e. `title` in `[foo]: /url "title"`
        TITLE,

        // End state, no matter what kind of lines we add, they won't be references
        PARAGRAPH,
    }

    companion object {
        private fun <T> removeLast(n: Int, list: MutableList<T>) {
            if (n >= list.size) {
                list.clear()
            } else {
                repeat(n) { list.removeAt(list.size - 1) }
            }
        }
    }
}
