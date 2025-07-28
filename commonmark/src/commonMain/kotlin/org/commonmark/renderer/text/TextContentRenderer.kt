package org.commonmark.renderer.text

import org.commonmark.Extension
import org.commonmark.internal.renderer.NodeRendererMap
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.Renderer

/**
 * Renders nodes to plain text content with minimal markup-like additions.
 */
class TextContentRenderer private constructor(builder: Builder) : Renderer {
    private val lineBreakRendering: LineBreakRendering

    private val nodeRendererFactories: List<TextContentNodeRendererFactory>

    init {
        this.lineBreakRendering = builder.lineBreakRendering

        this.nodeRendererFactories = ArrayList(builder.nodeRendererFactories.size + 1)
        this.nodeRendererFactories.addAll(builder.nodeRendererFactories)
        // Add as last. This means clients can override the rendering of core nodes if they want.
        this.nodeRendererFactories.add(object : TextContentNodeRendererFactory {
            override fun create(context: TextContentNodeRendererContext): NodeRenderer {
                return CoreTextContentNodeRenderer(context)
            }
        })
    }

    override fun render(node: Node, output: Appendable) {
        val context = RendererContext(TextContentWriter(output, lineBreakRendering))
        context.render(node)
    }

    override fun render(node: Node): String {
        val sb = StringBuilder()
        render(node, sb)
        return sb.toString()
    }

    /**
     * Builder for configuring a [TextContentRenderer]. See methods for default configuration.
     */
    class Builder {
        internal val nodeRendererFactories: MutableList<TextContentNodeRendererFactory> =
            mutableListOf()
        internal var lineBreakRendering: LineBreakRendering = LineBreakRendering.COMPACT

        /**
         * @return the configured [TextContentRenderer]
         */
        fun build(): TextContentRenderer {
            return TextContentRenderer(this)
        }

        /**
         * Configure how line breaks (newlines) are rendered, see [LineBreakRendering].
         * The default is [LineBreakRendering.COMPACT].
         *
         * @param lineBreakRendering the mode to use
         * @return `this`
         */
        fun lineBreakRendering(lineBreakRendering: LineBreakRendering): Builder {
            this.lineBreakRendering = lineBreakRendering
            return this
        }

        /**
         * Set the value of a flag for stripping new lines.
         *
         * @param stripNewlines true for stripping new lines and render text as "single line",
         * false for keeping all line breaks
         * @return `this`
         */
        @Deprecated("Use {@link #lineBreakRendering(LineBreakRendering)} with {@link LineBreakRendering#STRIP} instead")
        fun stripNewlines(stripNewlines: Boolean): Builder {
            this.lineBreakRendering =
                if (stripNewlines) LineBreakRendering.STRIP else LineBreakRendering.COMPACT
            return this
        }

        /**
         * Add a factory for instantiating a node renderer (done when rendering). This allows overriding the rendering
         * of node types or define rendering for custom node types.
         *
         *
         * If multiple node renderers for the same node type are created, the one from the factory that was added first
         * "wins". (This is how the rendering for core node types can be overridden; the default rendering comes last.)
         *
         * @param nodeRendererFactory the factory for creating a node renderer
         * @return `this`
         */
        fun nodeRendererFactory(nodeRendererFactory: TextContentNodeRendererFactory): Builder {
            this.nodeRendererFactories.add(nodeRendererFactory)
            return this
        }

        /**
         * @param extensions extensions to use on this text content renderer
         * @return `this`
         */
        fun extensions(extensions: Iterable<Extension>): Builder {
            for (extension in extensions) {
                if (extension is TextContentRendererExtension) {
                    val textContentRendererExtension =
                        extension
                    textContentRendererExtension.extend(this)
                }
            }
            return this
        }
    }

    /**
     * Extension for [TextContentRenderer].
     */
    interface TextContentRendererExtension : Extension {
        fun extend(rendererBuilder: Builder)
    }

    private inner class RendererContext(private val textContentWriter: TextContentWriter) :
        TextContentNodeRendererContext {
        private val nodeRendererMap: NodeRendererMap = NodeRendererMap()

        init {
            for (factory in nodeRendererFactories) {
                val renderer = factory.create(this)
                nodeRendererMap.add(renderer)
            }
        }

        override fun lineBreakRendering(): LineBreakRendering {
            return lineBreakRendering
        }

        @Deprecated("Use {@link #lineBreakRendering()} instead")
        override fun stripNewlines(): Boolean {
            return lineBreakRendering === LineBreakRendering.STRIP
        }

        override val writer: TextContentWriter
            get() = textContentWriter

        override fun render(node: Node) {
            nodeRendererMap.render(node)
        }
    }

    companion object {
        /**
         * Create a new builder for configuring a [TextContentRenderer].
         *
         * @return a builder
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}
