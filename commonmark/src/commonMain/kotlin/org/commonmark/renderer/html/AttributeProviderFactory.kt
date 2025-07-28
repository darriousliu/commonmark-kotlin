package org.commonmark.renderer.html

/**
 * Factory for instantiating new attribute providers when rendering is done.
 */
interface AttributeProviderFactory {
    /**
     * Create a new attribute provider.
     *
     * @param context for this attribute provider
     * @return an AttributeProvider
     */
    fun create(context: AttributeProviderContext): AttributeProvider
}
