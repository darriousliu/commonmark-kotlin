package org.commonmark.ext.heading.anchor.internal

import org.commonmark.ext.heading.anchor.IdGenerator
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Heading
import org.commonmark.node.Node
import org.commonmark.renderer.html.AttributeProvider

class HeadingIdAttributeProvider private constructor(
    defaultId: String,
    prefix: String,
    suffix: String
) : AttributeProvider {
    private val idGenerator: IdGenerator = IdGenerator.builder()
        .defaultId(defaultId)
        .prefix(prefix)
        .suffix(suffix)
        .build()

    override fun setAttributes(
        node: Node,
        tagName: String,
        attributes: MutableMap<String, String?>
    ) {
        if (node is Heading) {
            val wordList = ArrayList<String>()

            node.accept(object : AbstractVisitor() {
                override fun visit(text: org.commonmark.node.Text) {
                    wordList.add(text.literal)
                }

                override fun visit(code: org.commonmark.node.Code) {
                    wordList.add(code.literal)
                }
            })

            var finalString = ""
            for (word in wordList) {
                finalString += word
            }
            finalString = finalString.trim().lowercase()

            attributes.put("id", idGenerator.generateId(finalString))
        }
    }

    companion object {
        fun create(
            defaultId: String,
            prefix: String,
            suffix: String
        ): HeadingIdAttributeProvider {
            return HeadingIdAttributeProvider(
                defaultId,
                prefix,
                suffix
            )
        }
    }
}
