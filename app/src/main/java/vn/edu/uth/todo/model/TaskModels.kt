package vn.edu.uth.todo.model

import java.io.Serializable
import java.text.Normalizer
import java.util.Locale

enum class TaskFilter(val title: String, val category: TaskCategory?) {
    ALL("Tất cả", null), STUDY("Học tập", TaskCategory.STUDY), LIFE("Cuộc sống", TaskCategory.LIFE)
}

enum class TaskPriority(val title: String) {
    NORMAL("Ưu tiên thường"), LOW("Ưu tiên thấp"), MEDIUM("Ưu tiên vừa"), HIGH("Ưu tiên cao")
}

data class Subtask(val id: String, val title: String, val completed: Boolean = false) : Serializable

data class TaskItem(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val date: String,
    val time: String,
    val completed: Boolean = false,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val notes: String,
    val reminder: String,
    val repetition: String,
    val subtasks: List<Subtask>,
    val remind: Boolean = true,
    val sortOrder: Int = 0
) : Serializable {
    val deadline: String get() = "${TaskDates.displayDate(date)}, $time"
    val completedSubtasks: Int get() = subtasks.count { it.completed }
    val subtaskProgress: Float get() = if (subtasks.isEmpty()) 0f else completedSubtasks.toFloat() / subtasks.size
    val subtaskProgressLabel: String get() = "$completedSubtasks/${subtasks.size}"
    fun toSummary() = TodoTask(id, title, category, time, completed)
}

/** The list and detail screens share one collection of task objects. */
data class TaskBoard(
    val items: List<TaskItem>,
    val filter: TaskFilter = TaskFilter.ALL,
    val query: String = ""
) : Serializable {
    val visibleItems: List<TaskItem> get() {
        val term = searchable(query.trim())
        return items.filter { item ->
            (filter.category == null || item.category == filter.category) &&
                (term.isEmpty() || searchable(item.title).contains(term))
        }
    }
    val pending: List<TaskItem> get() = visibleItems.filterNot { it.completed }
    val completed: List<TaskItem> get() = visibleItems.filter { it.completed }

    fun find(id: String): TaskItem? = items.firstOrNull { it.id == id }
    fun update(id: String, change: (TaskItem) -> TaskItem): TaskBoard =
        copy(items = items.map { if (it.id == id) change(it) else it })

    private fun searchable(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace('đ', 'd')
}

data class TaskListLabels(
    val title: String, val pendingCount: String, val search: String,
    val pending: String, val completed: String, val urgent: String, val empty: String
)

data class TaskDetailLabels(
    val back: String, val edit: String, val deadline: String, val reminder: String,
    val repetition: String, val notes: String, val subtasks: String, val newSubtask: String,
    val add: String, val complete: String, val reopen: String, val delete: String,
    val deleteTitle: String, val deleteMessage: String, val confirmDelete: String,
    val cancel: String, val save: String, val title: String, val editSubtask: String
)

data class TaskContent(val tasks: List<TaskItem>, val list: TaskListLabels, val detail: TaskDetailLabels)
