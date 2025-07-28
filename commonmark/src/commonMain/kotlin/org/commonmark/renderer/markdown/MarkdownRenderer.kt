package org.commonmark.renderer.markdown

import org.commonmark.Extension
import org.commonmark.internal.renderer.NodeRendererMap
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.Renderer

/**
 * Renders nodes to Markdown (CommonMark syntax); use [.builder] to create a renderer.
 *
 *
 * Note that it doesn't currently preserve the exact syntax of the original input Markdown (if any):
 *
 *  * Headings are output as ATX headings if possible (multi-line headings need Setext headings)
 *  * Links are always rendered as inline links (no support for reference links yet)
 *  * Escaping might be over-eager, e.g. a plain `*` might be escaped
 * even though it doesn't need to be in that particular context
 *  * Leading whitespace in paragraphs is not preserved
 *
 * However, it should produce Markdown that is semantically equivalent to the input, i.e. if the Markdown was parsed
 * again and compared against the original AST, it should be the same (minus bugs).
 */
class MarkdownRenderer private constructor(builder: Builder) : Renderer {
    private val nodeRendererFactories: MutableList<MarkdownNodeRendererFactory> =
        ArrayList(builder.nodeRendererFactories.size + 1)

    init {
        this.nodeRendererFactories.addAll(builder.nodeRendererFactories)
        // Add as last. This means clients can override the rendering of core nodes if they want.
        this.nodeRendererFactories.add(object : MarkdownNodeRendererFactory {
            override fun create(context: MarkdownNodeRendererContext): NodeRenderer {
                return CoreMarkdownNodeRenderer(context)
            }

            override val specialCharacters: Set<Char>
                get() = setOf()
        })
    }

    override fun render(node: Node, output: Appendable) {
        val context = RendererContext(MarkdownWriter(output))
        context.render(node)
    }

    override fun render(node: Node): String {
        val sb = StringBuilder()
        render(node, sb)
        return sb.toString()
    }

    /**
     * Builder for configuring a [MarkdownRenderer]. See methods for default configuration.
     */
    class Builder {
        internal val nodeRendererFactories: MutableList<MarkdownNodeRendererFactory> =
            mutableListOf()

        /**
         * @return the configured [MarkdownRenderer]
         */
        fun build(): MarkdownRenderer {
            return MarkdownRenderer(this)
        }

        /**
         * Add a factory for instantiating a node renderer (done when rendering). This allows to override the rendering
         * of node types or define rendering for custom node types.
         *
         *
         * If multiple node renderers for the same node type are created, the one from the factory that was added first
         * "wins". (This is how the rendering for core node types can be overridden; the default rendering comes last.)
         *
         * @param nodeRendererFactory the factory for creating a node renderer
         * @return `this`
         */
        fun nodeRendererFactory(nodeRendererFactory: MarkdownNodeRendererFactory): Builder {
            this.nodeRendererFactories.add(nodeRendererFactory)
            return this
        }

        /**
         * @param extensions extensions to use on this renderer
         * @return `this`
         */
        fun extensions(extensions: Iterable<Extension>): Builder {
            for (extension in extensions) {
                if (extension is MarkdownRendererExtension) {
                    val markdownRendererExtension = extension
                    markdownRendererExtension.extend(this)
                }
            }
            return this
        }
    }

    /**
     * Extension for [MarkdownRenderer] for rendering custom nodes.
     */
    interface MarkdownRendererExtension : Extension {
        /**
         * Extend Markdown rendering, usually by registering custom node renderers using [Builder.nodeRendererFactory].
         *
         * @param rendererBuilder the renderer builder to extend
         */
        fun extend(rendererBuilder: Builder)
    }

    private inner class RendererContext(override val writer: MarkdownWriter) :
        MarkdownNodeRendererContext {
        private val nodeRendererMap: NodeRendererMap = NodeRendererMap()
        private val additionalTextEscapes: Set<Char>

        init {
            val escapes = hashSetOf<Char>()
            for (factory in nodeRendererFactories) {
                escapes.addAll(factory.specialCharacters)
            }
            additionalTextEscapes = escapes.toSet()

            for (factory in nodeRendererFactories) {
                // Pass in this as context here, which uses the fields set above
                val renderer = factory.create(this)
                nodeRendererMap.add(renderer)
            }
        }

        override fun render(node: Node) {
            nodeRendererMap.render(node)
        }

        override val specialCharacters: Set<Char>
            get() = additionalTextEscapes
    }

    companion object {
        /**
         * Create a new builder for configuring a [MarkdownRenderer].
         *
         * @return a builder
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}
