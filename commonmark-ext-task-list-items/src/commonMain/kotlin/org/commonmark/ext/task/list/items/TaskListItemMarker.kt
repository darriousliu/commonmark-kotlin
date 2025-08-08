package org.commonmark.ext.task.list.items

import org.commonmark.node.CustomNode

/**
 * A marker node indicating that a list item contains a task.
 */
class TaskListItemMarker(val isChecked: Boolean) : CustomNode()
