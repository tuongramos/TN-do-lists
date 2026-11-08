package vn.edu.uth.todo.model

enum class TaskSource { LOCAL, API, FIREBASE, CACHE }

data class TaskDataStatus(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val loaded: Boolean = false,
    val cloudWritable: Boolean = false,
    val source: TaskSource = TaskSource.LOCAL,
    val warning: String? = null
)

data class TaskSnapshot(
    val tasks: List<TaskItem>,
    val source: TaskSource,
    val cloudWritable: Boolean = false,
    val warning: String? = null
)

data class CloudTaskCollection(val initialized: Boolean, val tasks: List<TaskItem>)

enum class TaskFailure { AUTHENTICATION, NETWORK, PERMISSION, CONFIGURATION, INVALID_RESPONSE, NOT_FOUND, SERVER, UNKNOWN }

class TaskDataException(val reason: TaskFailure, cause: Throwable? = null,
    message: String = TaskDataMessages.forFailure(reason)) : Exception(message, cause)

object TaskDataMessages {
    const val LOADING = "Đang tải công việc…"
    const val SAVING = "Đang lưu thay đổi…"
    const val RETRY = "Thử lại"
    const val REFRESH = "Tải lại công việc"
    const val SAVE_ERROR = "Không thể lưu thay đổi"
    const val LOAD_ERROR = "Không thể tải công việc"
    const val EMPTY = "Chưa có công việc trong ngày này."
    const val SAVED = "Đã lưu thay đổi"
    fun forFailure(reason: TaskFailure): String = when (reason) {
        TaskFailure.AUTHENTICATION -> "API từ chối truy cập. Hãy kiểm tra Bearer Token."
        TaskFailure.NETWORK -> "Không thể kết nối. Hãy kiểm tra Internet rồi thử lại."
        TaskFailure.PERMISSION -> "Firebase chưa cho phép đọc/ghi. Hãy kiểm tra đăng nhập và Realtime Database Rules."
        TaskFailure.CONFIGURATION -> "Thiếu cấu hình API hoặc URL Realtime Database."
        TaskFailure.INVALID_RESPONSE -> "Dữ liệu công việc trả về không hợp lệ."
        TaskFailure.NOT_FOUND -> "Không tìm thấy công việc này."
        TaskFailure.SERVER -> "Máy chủ công việc đang gặp lỗi. Vui lòng thử lại."
        TaskFailure.UNKNOWN -> "Chưa thể đồng bộ công việc. Vui lòng thử lại."
    }
}
