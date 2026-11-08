package vn.edu.uth.todo.model

data class TaskDetailStatus(
    val taskId: String? = null, val loading: Boolean = false, val loaded: Boolean = false,
    val warning: String? = null, val notFound: Boolean = false
)

data class TaskDetailSnapshot(
    val task: TaskItem, val source: TaskSource, val cloudWritable: Boolean = false, val warning: String? = null
)

object TaskDetailMessages {
    const val LOAD_ERROR = "Không thể tải chi tiết công việc"
    const val FIREBASE_PENDING = "API đã xử lý, nhưng chưa lưu được vào Firebase. Hãy thử lại."
}
