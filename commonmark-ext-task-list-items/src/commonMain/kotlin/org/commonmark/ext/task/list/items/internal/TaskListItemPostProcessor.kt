package org.commonmark.ext.task.list.items.internal

import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.parser.PostProcessor

class TaskListItemPostProcessor : PostProcessor {
    override fun process(node: Node): Node {
        val visitor = TaskListItemVisitor()
        node.accept(visitor)
        return node
    }

    private class TaskListItemVisitor : AbstractVisitor() {
        override fun visit(listItem: ListItem) {
            val child = listItem.firstChild
            if (child is Paragraph) {
                val node = child.firstChild
                if (node is Text) {
                    val textNode = node
                    val matchResult = REGEX_TASK_LIST_ITEM.matchEntire(textNode.literal)
                    if (matchResult != null) {
                        val checked = matchResult.groupValues[1]
                        val isChecked = checked.equals("X", ignoreCase = true)

                        // Add the task list item marker node as the first child of the list item.
                        listItem.prependChild(TaskListItemMarker(isChecked))

                        // 取出任务内容（去掉前面的 [ ] 或 [x] 等）
                        // Parse the node using the input after the task marker (in other words, group 2 from the matcher).
                        // (Note that the String has been trimmed, so we should add a space between the
                        // TaskListItemMarker and the text that follows it when we come to render it).
                        textNode.literal = matchResult.groupValues[2]
                    }
                }
            }
            visitChildren(listItem)
        }
    }

    companion object {
        private val REGEX_TASK_LIST_ITEM = Regex("^\\[([xX\\s])]\\s+(.*)")
    }
}
