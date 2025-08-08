package org.commonmark.renderer.html

import org.commonmark.node.Node

interface HtmlNodeRendererContext {
    /**
     * @param url to be encoded
     * @return an encoded URL (depending on the configuration)
     */
    fun encodeUrl(url: String): String

    /**
     * Let extensions modify the HTML tag attributes.
     *
     * @param node       the node for which the attributes are applied
     * @param tagName    the HTML tag name that these attributes are for (e.g. `h1`, `pre`, `code`).
     * @param attributes the attributes that were calculated by the renderer
     * @return the extended attributes with added/updated/removed entries
     */
    fun extendAttributes(
        node: Node,
        tagName: String,
        attributes: MutableMap<String, String?>
    ): Map<String, String?>

    /**
     * @return the HTML writer to use
     */
    val writer: HtmlWriter

    /**
     * @return HTML that should be rendered for a soft line break
     */
    val softbreak: String

    /**
     * Render the specified node and its children using the configured renderers. This should be used to render child
     * nodes; be careful not to pass the node that is being rendered, that would result in an endless loop.
     *
     * @param node the node to render
     */
    fun render(node: Node)

    /**
     * @return whether HTML blocks and tags should be escaped or not
     */
    fun shouldEscapeHtml(): Boolean

    /**
     * @return whether documents that only contain a single paragraph should be rendered without the `<p>` tag
     */
    fun shouldOmitSingleParagraphP(): Boolean

    /**
     * @return true if the [UrlSanitizer] should be used.
     * @since 0.14.0
     */
    fun shouldSanitizeUrls(): Boolean

    /**
     * @return Sanitizer to use for securing [org.commonmark.node.Link] href and [org.commonmark.node.Image] src if [.shouldSanitizeUrls] is true.
     * @since 0.14.0
     */
    fun urlSanitizer(): UrlSanitizer
}
