package org.commonmark.ext.task.list.items

import org.commonmark.Extension
import org.commonmark.ext.task.list.items.internal.TaskListItemHtmlNodeRenderer
import org.commonmark.ext.task.list.items.internal.TaskListItemPostProcessor
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlNodeRendererFactory
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Extension for adding task list items.
 *
 *
 * Create it with [.create] and then configure it on the builders
 * ([Parser.Builder.extensions],
 * [HtmlRenderer.Builder.extensions]).
 *
 *
 * @since 0.15.0
 */
class TaskListItemsExtension private constructor() : Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension {

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessor(TaskListItemPostProcessor())
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(object : HtmlNodeRendererFactory {
            override fun create(context: HtmlNodeRendererContext): NodeRenderer {
                return TaskListItemHtmlNodeRenderer(context)
            }
        })
    }

    companion object {
        fun create(): Extension {
            return TaskListItemsExtension()
        }
    }
}
