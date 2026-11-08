package vn.edu.uth.todo.model

enum class TaskListBody { CONTENT, LOADING, ERROR, EMPTY, NO_RESULTS }

/** Presentation of the same task collection used by Home and task details. */
data class TaskListState(val board: TaskBoard, val status: TaskDataStatus) {
    val pending: List<TaskItem> get() = board.pending
    val completed: List<TaskItem> get() = board.completed
    val pendingTotal: Int get() = board.items.count { !it.completed }
    val body: TaskListBody get() = when {
        board.items.isNotEmpty() && board.visibleItems.isNotEmpty() -> TaskListBody.CONTENT
        board.items.isEmpty() && status.loading -> TaskListBody.LOADING
        board.items.isEmpty() && status.warning != null -> TaskListBody.ERROR
        board.items.isEmpty() -> TaskListBody.EMPTY
        else -> TaskListBody.NO_RESULTS
    }
}

object TaskListMessages {
    const val EMPTY = "Chưa có công việc nào."
    const val CLEAR_FILTERS = "Xóa tìm kiếm và bộ lọc"
}
