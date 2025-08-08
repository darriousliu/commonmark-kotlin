package org.commonmark.ext.image.attributes

import org.commonmark.Extension
import org.commonmark.ext.image.attributes.internal.ImageAttributesAttributeProvider
import org.commonmark.ext.image.attributes.internal.ImageAttributesDelimiterProcessor
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.AttributeProvider
import org.commonmark.renderer.html.AttributeProviderContext
import org.commonmark.renderer.html.AttributeProviderFactory
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Extension for adding attributes to image nodes.
 *
 *
 * Create it with [.create] and then configure it on the builders
 * ([Parser.Builder.extensions],
 * [HtmlRenderer.Builder.extensions]).
 *
 *
 * @since 0.15.0
 */
class ImageAttributesExtension private constructor() : Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customDelimiterProcessor(ImageAttributesDelimiterProcessor())
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.attributeProviderFactory(object : AttributeProviderFactory {
            override fun create(context: AttributeProviderContext): AttributeProvider {
                return ImageAttributesAttributeProvider.create()
            }
        })
    }

    companion object {
        fun create(): Extension {
            return ImageAttributesExtension()
        }
    }
}
