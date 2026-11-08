package vn.edu.uth.todo.data

import vn.edu.uth.todo.model.*

data class FirebaseSubtaskRecord(
    var id: String = "", var title: String = "", var done: Boolean = false, var position: Int = 0
)

/** Writable, no-argument DTOs are the serialization boundary for Realtime Database. */
data class FirebaseTaskRecord(
    var id: String = "", var title: String = "", var note: String = "", var category: String = "study",
    var date: String = "", var time: String = "", var priority: String = "medium", var remind: Boolean = false,
    var done: Boolean = false, var repetition: String = "Không", var position: Int = 0,
    var subtasks: Map<String, FirebaseSubtaskRecord> = emptyMap()
) {
    fun toDomain(): TaskItem {
        require(id.matches(Regex("[A-Za-z0-9_-]+")) && title.isNotBlank())
        require(TaskDates.isIsoDate(date) && time.matches(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")))
        val kind = when (category) { "study" -> TaskCategory.STUDY; "life" -> TaskCategory.LIFE; else -> error("category") }
        val level = when (priority) {
            "high" -> TaskPriority.HIGH; "low" -> TaskPriority.LOW; "medium" -> TaskPriority.MEDIUM; else -> error("priority")
        }
        return TaskItem(id, title, kind, date, time, done, level, note,
            if (remind) "Trước 15 phút" else "Không", repetition,
            subtasks.values.sortedBy { it.position }.map {
                require(it.id.matches(Regex("[A-Za-z0-9_-]+")) && it.title.isNotBlank())
                Subtask(it.id, it.title, it.done)
            }, remind, position)
    }

    companion object {
        fun from(task: TaskItem) = FirebaseTaskRecord(task.id, task.title, task.notes,
            if (task.category == TaskCategory.STUDY) "study" else "life", task.date, task.time,
            when (task.priority) { TaskPriority.HIGH -> "high"; TaskPriority.LOW -> "low"; else -> "medium" },
            task.remind, task.completed, task.repetition, task.sortOrder,
            task.subtasks.mapIndexed { index, item -> item.id to FirebaseSubtaskRecord(item.id, item.title, item.completed, index) }.toMap())
    }
}
