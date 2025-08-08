package org.commonmark.ext.heading.anchor

import org.commonmark.Extension
import org.commonmark.ext.heading.anchor.internal.HeadingIdAttributeProvider
import org.commonmark.renderer.html.AttributeProvider
import org.commonmark.renderer.html.AttributeProviderContext
import org.commonmark.renderer.html.AttributeProviderFactory
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Extension for adding auto generated IDs to headings.
 *
 *
 * Create it with [.create] or [.builder] and then configure it on the
 * renderer builder ([HtmlRenderer.Builder.extensions]).
 *
 *
 * The heading text will be used to create the id. Multiple headings with the
 * same text will result in appending a hyphen and number. For example:
 * <pre>`
 * # Heading
 * # Heading
`</pre> *
 * will result in
 * <pre>`
 * <h1 id="heading">Heading</h1>
 * <h1 id="heading-1">Heading</h1>
`</pre> *
 *
 * @see IdGenerator the IdGenerator class if just the ID generation part is needed
 */
class HeadingAnchorExtension private constructor(builder: Builder) :
    HtmlRenderer.HtmlRendererExtension {
    private val defaultId: String
    private val idPrefix: String
    private val idSuffix: String

    init {
        this.defaultId = builder.defaultId
        this.idPrefix = builder.idPrefix
        this.idSuffix = builder.idSuffix
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.attributeProviderFactory(object : AttributeProviderFactory {
            override fun create(context: AttributeProviderContext): AttributeProvider {
                return HeadingIdAttributeProvider.create(defaultId, idPrefix, idSuffix)
            }
        })
    }

    class Builder {
        internal var defaultId: String = "id"
        internal var idPrefix: String = ""
        internal var idSuffix: String = ""

        /**
         * @param value Default value for the id to take if no generated id can be extracted. Default "id"
         * @return `this`
         */
        fun defaultId(value: String): Builder {
            this.defaultId = value
            return this
        }

        /**
         * @param value Set the value to be prepended to every id generated. Default ""
         * @return `this`
         */
        fun idPrefix(value: String): Builder {
            this.idPrefix = value
            return this
        }

        /**
         * @param value Set the value to be appended to every id generated. Default ""
         * @return `this`
         */
        fun idSuffix(value: String): Builder {
            this.idSuffix = value
            return this
        }

        /**
         * @return a configured extension
         */
        fun build(): Extension {
            return HeadingAnchorExtension(this)
        }
    }

    companion object {
        /**
         * @return the extension built with default settings
         */
        fun create(): Extension {
            return HeadingAnchorExtension(builder())
        }

        /**
         * @return a builder to configure the extension settings
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}
