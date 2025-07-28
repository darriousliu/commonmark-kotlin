package org.commonmark.renderer.html

import org.commonmark.Extension
import org.commonmark.internal.renderer.NodeRendererMap
import org.commonmark.internal.util.Escaping
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.Renderer

/**
 * Renders a tree of nodes to HTML.
 *
 *
 * Start with the [.builder] method to configure the renderer. Example:
 * <pre>`
 * HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();
 * renderer.render(node);
`</pre> *
 */
class HtmlRenderer private constructor(builder: Builder) : Renderer {
    private val softbreak: String = builder.softbreak
    private val escapeHtml: Boolean = builder.escapeHtml
    private val percentEncodeUrls: Boolean = builder.percentEncodeUrls
    private val omitSingleParagraphP: Boolean = builder.omitSingleParagraphP
    private val sanitizeUrls: Boolean = builder.sanitizeUrls
    private val urlSanitizer: UrlSanitizer = builder.urlSanitizer
    private val attributeProviderFactories: MutableList<AttributeProviderFactory> =
        ArrayList(builder.attributeProviderFactories)
    private val nodeRendererFactories: MutableList<HtmlNodeRendererFactory> =
        ArrayList(builder.nodeRendererFactories.size + 1)

    init {
        this.nodeRendererFactories.addAll(builder.nodeRendererFactories)
        // Add as last. This means clients can override the rendering of core nodes if they want.
        this.nodeRendererFactories.add(object : HtmlNodeRendererFactory {
            override fun create(context: HtmlNodeRendererContext): NodeRenderer {
                return CoreHtmlNodeRenderer(context)
            }
        })
    }

    override fun render(node: Node, output: Appendable) {
        val context = RendererContext(HtmlWriter(output))
        context.beforeRoot(node)
        context.render(node)
        context.afterRoot(node)
    }

    override fun render(node: Node): String {
        val sb = StringBuilder()
        render(node, sb)
        return sb.toString()
    }

    /**
     * Builder for configuring an [HtmlRenderer]. See methods for default configuration.
     */
    class Builder {
        internal var softbreak: String = "\n"
        internal var escapeHtml = false
        internal var sanitizeUrls = false
        internal var urlSanitizer: UrlSanitizer = DefaultUrlSanitizer()
        internal var percentEncodeUrls = false
        internal var omitSingleParagraphP = false
        internal val attributeProviderFactories: MutableList<AttributeProviderFactory> =
            mutableListOf()
        internal val nodeRendererFactories: MutableList<HtmlNodeRendererFactory> = mutableListOf()

        /**
         * @return the configured [HtmlRenderer]
         */
        fun build(): HtmlRenderer {
            return HtmlRenderer(this)
        }

        /**
         * The HTML to use for rendering a softbreak, defaults to `"\n"` (meaning the rendered result doesn't have
         * a line break).
         *
         *
         * Set it to `"<br>"` (or `"<br />"` to make them hard breaks.
         *
         *
         * Set it to `" "` to ignore line wrapping in the source.
         *
         * @param softbreak HTML for softbreak
         * @return `this`
         */
        fun softbreak(softbreak: String): Builder {
            this.softbreak = softbreak
            return this
        }

        /**
         * Whether [org.commonmark.node.HtmlInline] and [org.commonmark.node.HtmlBlock] should be escaped, defaults to `false`.
         *
         *
         * Note that [org.commonmark.node.HtmlInline] is only a tag itself, not the text between an opening tag and a closing tag. So
         * markup in the text will be parsed as normal and is not affected by this option.
         *
         * @param escapeHtml true for escaping, false for preserving raw HTML
         * @return `this`
         */
        fun escapeHtml(escapeHtml: Boolean): Builder {
            this.escapeHtml = escapeHtml
            return this
        }

        /**
         * Whether [org.commonmark.node.Image] src and [org.commonmark.node.Link] href should be sanitized, defaults to `false`.
         *
         * @param sanitizeUrls true for sanitization, false for preserving raw attribute
         * @return `this`
         * @since 0.14.0
         */
        fun sanitizeUrls(sanitizeUrls: Boolean): Builder {
            this.sanitizeUrls = sanitizeUrls
            return this
        }

        /**
         * [UrlSanitizer] used to filter URL's if [.sanitizeUrls] is true.
         *
         * @param urlSanitizer Filterer used to filter [org.commonmark.node.Image] src and [org.commonmark.node.Link].
         * @return `this`
         * @since 0.14.0
         */
        fun urlSanitizer(urlSanitizer: UrlSanitizer): Builder {
            this.urlSanitizer = urlSanitizer
            return this
        }

        /**
         * Whether URLs of link or images should be percent-encoded, defaults to `false`.
         *
         *
         * If enabled, the following is done:
         *
         *  * Existing percent-encoded parts are preserved (e.g. "%20" is kept as "%20")
         *  * Reserved characters such as "/" are preserved, except for "[" and "]" (see encodeURI in JS)
         *  * Unreserved characters such as "a" are preserved
         *  * Other characters such umlauts are percent-encoded
         *
         *
         * @param percentEncodeUrls true to percent-encode, false for leaving as-is
         * @return `this`
         */
        fun percentEncodeUrls(percentEncodeUrls: Boolean): Builder {
            this.percentEncodeUrls = percentEncodeUrls
            return this
        }

        /**
         * Whether documents that only contain a single paragraph should be rendered without the `<p>` tag. Set to
         * `true` to render without the tag; the default of `false` always renders the tag.
         *
         * @return `this`
         */
        fun omitSingleParagraphP(omitSingleParagraphP: Boolean): Builder {
            this.omitSingleParagraphP = omitSingleParagraphP
            return this
        }

        /**
         * Add a factory for an attribute provider for adding/changing HTML attributes to the rendered tags.
         *
         * @param attributeProviderFactory the attribute provider factory to add
         * @return `this`
         */
        fun attributeProviderFactory(attributeProviderFactory: AttributeProviderFactory): Builder {
            this.attributeProviderFactories.add(attributeProviderFactory)
            return this
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
        fun nodeRendererFactory(nodeRendererFactory: HtmlNodeRendererFactory): Builder {
            this.nodeRendererFactories.add(nodeRendererFactory)
            return this
        }

        /**
         * @param extensions extensions to use on this HTML renderer
         * @return `this`
         */
        fun extensions(extensions: Iterable<Extension>): Builder {
            for (extension in extensions) {
                if (extension is HtmlRendererExtension) {
                    val htmlRendererExtension = extension
                    htmlRendererExtension.extend(this)
                }
            }
            return this
        }
    }

    /**
     * Extension for [HtmlRenderer].
     */
    interface HtmlRendererExtension : Extension {
        fun extend(rendererBuilder: Builder)
    }

    private inner class RendererContext(private val htmlWriter: HtmlWriter) :
        HtmlNodeRendererContext,
        AttributeProviderContext {
        private val attributeProviders: MutableList<AttributeProvider> =
            ArrayList(attributeProviderFactories.size)
        private val nodeRendererMap: NodeRendererMap = NodeRendererMap()

        init {
            for (attributeProviderFactory in attributeProviderFactories) {
                attributeProviders.add(attributeProviderFactory.create(this))
            }

            for (factory in nodeRendererFactories) {
                val renderer = factory.create(this)
                nodeRendererMap.add(renderer)
            }
        }

        override fun shouldEscapeHtml(): Boolean {
            return escapeHtml
        }

        override fun shouldOmitSingleParagraphP(): Boolean {
            return omitSingleParagraphP
        }

        override fun shouldSanitizeUrls(): Boolean {
            return sanitizeUrls
        }

        override fun urlSanitizer(): UrlSanitizer {
            return urlSanitizer
        }

        override fun encodeUrl(url: String): String {
            return if (percentEncodeUrls) {
                Escaping.percentEncodeUrl(url)
            } else {
                url
            }
        }

        override fun extendAttributes(
            node: Node,
            tagName: String,
            attributes: Map<String, String?>
        ): Map<String, String?> {
            val attrs: Map<String, String?> = LinkedHashMap(attributes)
            setCustomAttributes(node, tagName, attrs)
            return attrs
        }

        override val writer: HtmlWriter
            get() = htmlWriter

        override val softbreak: String
            get() = this@HtmlRenderer.softbreak

        override fun render(node: Node) {
            nodeRendererMap.render(node)
        }

        fun beforeRoot(node: Node) {
            nodeRendererMap.beforeRoot(node)
        }

        fun afterRoot(node: Node) {
            nodeRendererMap.afterRoot(node)
        }

        fun setCustomAttributes(node: Node, tagName: String, attrs: Map<String, String?>) {
            for (attributeProvider in attributeProviders) {
                attributeProvider.setAttributes(node, tagName, attrs)
            }
        }
    }

    companion object {
        /**
         * Create a new builder for configuring an [HtmlRenderer].
         *
         * @return a builder
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}
