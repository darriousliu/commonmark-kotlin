package org.commonmark.ext.footnotes.internal

import org.commonmark.ext.computeIfAbsent2
import org.commonmark.ext.footnotes.FootnoteDefinition
import org.commonmark.ext.footnotes.FootnoteReference
import org.commonmark.ext.footnotes.InlineFootnote
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.CustomBlock
import org.commonmark.node.CustomNode
import org.commonmark.node.DefinitionMap
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlWriter
import kotlin.reflect.KClass

/**
 * HTML rendering for footnotes.
 *
 *
 * Aims to match the rendering of cmark-gfm (which is slightly different from GitHub's when it comes to class
 * attributes, not sure why).
 *
 *
 * Some notes on how rendering works:
 *
 *  * Footnotes are numbered according to the order of references, starting at 1
 *  * Definitions are rendered at the end of the document, regardless of where the definition was in the source
 *  * Definitions are ordered by number
 *  * Definitions have links back to their references (one or more)
 *
 *
 * <h4>Nested footnotes</h4>
 * Text in footnote definitions can reference other footnotes, even ones that aren't referenced in the main text.
 * This makes them tricky because it's not enough to just go through the main text for references.
 * And before we can render a definition, we need to know all references (because we add links back to references).
 *
 *
 * In other words, footnotes form a directed graph. Footnotes can reference each other so cycles are possible too.
 *
 *
 * One way to implement it, which is what cmark-gfm does, is to go through the whole document (including definitions)
 * and find all references in order. That guarantees that all definitions are found, but it has strange results for
 * ordering or when the reference is in an unreferenced definition, see tests. In graph terms, it renders all
 * definitions that have an incoming edge, no matter whether they are connected to the main text or not.
 *
 *
 * The way we implement it:
 *
 *  1. Start with the references in the main text; we can render them as we go
 *  1. After the main text is rendered, we have the referenced definitions, but there might be more from definition text
 *  1. To find the remaining definitions, we visit the definitions from before to look at references
 *  1. Repeat (breadth-first search) until we've found all definitions (note that we can't render before that's done because of backrefs)
 *  1. Now render the definitions (and any references inside)
 *
 * This means we only render definitions whose references are actually rendered, and in a meaningful order (all main
 * text footnotes first, then any nested ones).
 */
class FootnoteHtmlNodeRenderer(private val context: HtmlNodeRendererContext) : NodeRenderer {
    private val html: HtmlWriter = context.writer

    /**
     * All definitions (even potentially unused ones), for looking up references
     */
    private lateinit var definitionMap: DefinitionMap<FootnoteDefinition>

    /**
     * Definitions that were referenced, in order in which they should be rendered.
     */
    private val referencedDefinitions: LinkedHashMap<Node, ReferencedDefinition> = LinkedHashMap()

    /**
     * Information about references that should be rendered as footnotes. This doesn't contain all references, just the
     * ones from inside definitions.
     */
    private val references: HashMap<Node, ReferenceInfo> = HashMap()

    override val nodeTypes: Set<KClass<out Node>>
        get() = setOf(
            FootnoteReference::class,
            InlineFootnote::class,
            FootnoteDefinition::class
        )

    override fun beforeRoot(rootNode: Node) {
        // Collect all definitions first, so we can look them up when encountering a reference later.
        val visitor = DefinitionVisitor()
        rootNode.accept(visitor)
        definitionMap = visitor.definitions
    }

    override fun render(node: Node) {
        if (node is FootnoteReference) {
            // This is called for all references, even ones inside definitions that we render at the end.
            // Inside definitions, we have registered the reference already.
            val ref = node
            // Use containsKey because if value is null, we don't need to try registering again.
            val info =
                if (references.containsKey(ref)) references[ref] else tryRegisterReference(ref)
            if (info != null) {
                renderReference(ref, info)
            } else {
                // A reference without a corresponding definition is rendered as plain text
                html.text("[^" + ref.label + "]")
            }
        } else if (node is InlineFootnote) {
            var info = references[node]
            if (info == null) {
                info = registerReference(node, null)
            }
            renderReference(node, info)
        }
    }

    override fun afterRoot(rootNode: Node) {
        // Now render the referenced definitions if there are any.
        if (referencedDefinitions.isEmpty()) {
            return
        }

        val firstDef = referencedDefinitions.keys.iterator().next()
        val attrs = LinkedHashMap<String, String?>()
        attrs.put("class", "footnotes")
        attrs.put("data-footnotes", null)
        html.tag("section", context.extendAttributes(firstDef, "section", attrs))
        html.line()
        html.tag("ol")
        html.line()

        // Check whether there are any footnotes inside the definitions that we're about to render. For those, we might
        // need to render more definitions. So do a breadth-first search to find all relevant definitions.
        val check = ArrayList(referencedDefinitions.keys)
        while (!check.isEmpty()) {
            val def = check.removeFirst()
            def.accept(
                ShallowReferenceVisitor(
                    def
                ) { node ->
                    if (node is FootnoteReference) {
                        val ref: FootnoteReference = node
                        val d = definitionMap.get(ref.label)
                        if (d != null) {
                            if (!referencedDefinitions.containsKey(d)) {
                                check.add(d)
                            }
                            references.put(ref, registerReference(d, d.label))
                        }
                    } else if (node is InlineFootnote) {
                        check.add(node)
                        references.put(node, registerReference(node, null))
                    }
                }
            )
        }

        for (entry in referencedDefinitions.entries) {
            // This will also render any footnote references inside definitions
            renderDefinition(entry.key, entry.value)
        }

        html.tag("/ol")
        html.line()
        html.tag("/section")
        html.line()
    }

    private fun tryRegisterReference(ref: FootnoteReference): ReferenceInfo? {
        val def = definitionMap.get(ref.label)
        if (def == null) {
            return null
        }
        return registerReference(def, def.label)
    }

    private fun registerReference(node: Node, label: String?): ReferenceInfo {
        // The first referenced definition gets number 1, second one 2, etc.
        val referencedDef = referencedDefinitions.computeIfAbsent2(node) { k ->
            val num = referencedDefinitions.size + 1
            val key = definitionKey(label, num)
            ReferencedDefinition(
                num,
                key
            )
        }
        val definitionNumber = referencedDef.definitionNumber
        // The reference number for that particular definition. E.g. if there's two references for the same definition,
        // the first one is 1, the second one 2, etc. This is needed to give each reference a unique ID so that each
        // reference can get its own backlink from the definition.
        val refNumber = referencedDef.references.size + 1
        val definitionKey = referencedDef.definitionKey
        val id = referenceId(definitionKey, refNumber)
        referencedDef.references.add(id)

        return ReferenceInfo(id, definitionId(definitionKey), definitionNumber)
    }

    private fun renderReference(node: Node, referenceInfo: ReferenceInfo) {
        html.tag(
            "sup",
            context.extendAttributes(node, "sup", mutableMapOf("class" to "footnote-ref"))
        )

        val href = "#" + referenceInfo.definitionId
        val attrs = LinkedHashMap<String, String?>()
        attrs.put("href", href)
        attrs.put("id", referenceInfo.id)
        attrs.put("data-footnote-ref", null)
        html.tag("a", context.extendAttributes(node, "a", attrs))
        html.raw(referenceInfo.definitionNumber.toString())
        html.tag("/a")
        html.tag("/sup")
    }

    private fun renderDefinition(def: Node, referencedDefinition: ReferencedDefinition) {
        val attrs = LinkedHashMap<String, String?>()
        attrs.put("id", definitionId(referencedDefinition.definitionKey))
        html.tag("li", context.extendAttributes(def, "li", attrs))
        html.line()

        if (def.lastChild is Paragraph) {
            // Add backlinks into last paragraph before </p>. This is what GFM does.
            val lastParagraph = def.lastChild as Paragraph
            var node = def.firstChild
            while (node != lastParagraph) {
                if (node is Paragraph) {
                    // Because we're manually rendering the <p> for the last paragraph, do the same for all other
                    // paragraphs for consistency (Paragraph rendering might be overwritten by a custom renderer).
                    html.tag("p", context.extendAttributes(node, "p", mutableMapOf()))
                    renderChildren(node)
                    html.tag("/p")
                    html.line()
                } else {
                    context.render(node!!)
                }
                node = node.next
            }

            html.tag("p", context.extendAttributes(lastParagraph, "p", mutableMapOf()))
            renderChildren(lastParagraph)
            html.raw(" ")
            renderBackrefs(def, referencedDefinition)
            html.tag("/p")
            html.line()
        } else if (def is InlineFootnote) {
            html.tag("p", context.extendAttributes(def, "p", mutableMapOf()))
            renderChildren(def)
            html.raw(" ")
            renderBackrefs(def, referencedDefinition)
            html.tag("/p")
            html.line()
        } else {
            renderChildren(def)
            html.line()
            renderBackrefs(def, referencedDefinition)
        }

        html.tag("/li")
        html.line()
    }

    private fun renderBackrefs(def: Node, referencedDefinition: ReferencedDefinition) {
        val refs = referencedDefinition.references
        for (i in 0..<refs.size) {
            val ref = refs[i]
            val refNumber = i + 1
            val idx =
                referencedDefinition.definitionNumber.toString() + (if (refNumber > 1) ("-$refNumber") else "")

            val attrs = LinkedHashMap<String, String?>()
            attrs.put("href", "#$ref")
            attrs.put("class", "footnote-backref")
            attrs.put("data-footnote-backref", null)
            attrs.put("data-footnote-backref-idx", idx)
            attrs.put("aria-label", "Back to reference $idx")
            html.tag("a", context.extendAttributes(def, "a", attrs))
            if (refNumber > 1) {
                html.tag(
                    "sup",
                    context.extendAttributes(def, "sup", mutableMapOf("class" to "footnote-ref"))
                )
                html.raw(refNumber.toString())
                html.tag("/sup")
            }
            // U+21A9 LEFTWARDS ARROW WITH HOOK
            html.raw("\u21A9")
            html.tag("/a")
            if (i + 1 < refs.size) {
                html.raw(" ")
            }
        }
    }

    private fun referenceId(definitionKey: String?, number: Int): String {
        return "fnref" + definitionKey + (if (number == 1) "" else ("-$number"))
    }

    private fun definitionKey(label: String?, number: Int): String {
        // Named definitions use the pattern "fn-{name}" and inline definitions use "fn{number}" so as not to conflict.
        // "fn{number}" is also what pandoc uses (for all types), starting with number 1.
        return if (label != null) {
            "-$label"
        } else {
            "" + number
        }
    }

    private fun definitionId(definitionKey: String?): String {
        return "fn$definitionKey"
    }

    private fun renderChildren(parent: Node) {
        var node = parent.firstChild
        while (node != null) {
            val next = node.next
            context.render(node)
            node = next
        }
    }

    private class DefinitionVisitor : AbstractVisitor() {
        val definitions: DefinitionMap<FootnoteDefinition> =
            DefinitionMap(FootnoteDefinition::class)

        override fun visit(customBlock: CustomBlock) {
            if (customBlock is FootnoteDefinition) {
                val def: FootnoteDefinition = customBlock
                definitions.putIfAbsent(def.label, def)
            } else {
                super.visit(customBlock)
            }
        }
    }

    /**
     * Visit footnote references/inline footnotes inside the parent (but not the parent itself). We want a shallow visit
     * because the caller wants to control when to descend.
     */
    private class ShallowReferenceVisitor(
        private val parent: Node?,
        private val consumer: (Node) -> Unit
    ) : AbstractVisitor() {

        override fun visit(customNode: CustomNode) {
            if (customNode is FootnoteReference) {
                consumer(customNode)
            } else if (customNode is InlineFootnote) {
                if (customNode === parent) {
                    // Descend into the parent (inline footnotes can contain inline footnotes)
                    super.visit(customNode)
                } else {
                    // Don't descend here because we want to be shallow.
                    consumer(customNode)
                }
            } else {
                super.visit(customNode)
            }
        }
    }

    private class ReferencedDefinition(
        /**
         * The definition number, starting from 1, and in order in which they're referenced.
         */
        val definitionNumber: Int,
        /**
         * The unique key of the definition. Together with a static prefix it forms the ID used in the HTML.
         */
        val definitionKey: String?
    ) {
        /**
         * The IDs of references for this definition, for backrefs.
         */
        val references: ArrayList<String?> = ArrayList()
    }

    private class ReferenceInfo(
        /**
         * The ID of the reference; in the corresponding definition, a link back to this reference will be rendered.
         */
        val id: String?,
        /**
         * The ID of the definition, for linking to the definition.
         */
        val definitionId: String?,
        /**
         * The definition number, rendered in superscript.
         */
        val definitionNumber: Int
    )
}
