package vn.edu.uth.todo.data

import vn.edu.uth.todo.model.*

interface TaskRepository { fun getContent(): TaskContent }

class FigmaTaskRepository : TaskRepository {
    override fun getContent() = TaskContent(
        tasks = listOf(
            TaskItem("mobile", "Nộp bài tập Lập trình di động", TaskCategory.STUDY, "Hôm nay", "09:00",
                priority = TaskPriority.HIGH,
                notes = "Nộp file .zip lên hệ thống của trường. Nhớ\nkèm ảnh chụp màn hình app chạy trên máy\nảo.",
                reminder = "Trước 15 phút", repetition = "Không",
                subtasks = listOf(
                    Subtask("login", "Hoàn thiện màn hình đăng nhập", true),
                    Subtask("screenshots", "Chụp ảnh màn hình app", true),
                    Subtask("submit", "Nén file và nộp bài")
                )),
            TaskItem("chapter", "Ôn chương 3 – Cấu trúc dữ liệu", TaskCategory.STUDY, "Hôm nay", "14:00",
                notes = "Ôn lại lý thuyết chương 3 và làm các bài tập về cấu trúc dữ liệu.",
                reminder = "Trước 15 phút", repetition = "Không",
                subtasks = listOf(Subtask("theory", "Đọc lại lý thuyết"), Subtask("practice", "Làm bài tập chương 3"))),
            TaskItem("shopping", "Đi chợ mua đồ cho tuần", TaskCategory.LIFE, "Hôm nay", "17:30",
                notes = "Mua thực phẩm và các đồ dùng cần thiết cho tuần.",
                reminder = "Trước 15 phút", repetition = "Hàng tuần",
                subtasks = listOf(Subtask("vegetables", "Mua rau và trái cây"), Subtask("food", "Mua thực phẩm cho tuần"))),
            TaskItem("meeting", "Họp nhóm đồ án", TaskCategory.STUDY, "Thứ Năm", "20:00",
                notes = "Trao đổi tiến độ đồ án, thống nhất công việc tiếp theo với các thành viên trong nhóm.",
                reminder = "Trước 15 phút", repetition = "Không",
                subtasks = listOf(Subtask("progress", "Chuẩn bị báo cáo tiến độ"), Subtask("agenda", "Ghi lại nội dung cần thảo luận"))),
            TaskItem("exercise", "Tập thể dục 30 phút", TaskCategory.LIFE, "Hôm nay", "06:30", completed = true,
                notes = "Dành 30 phút vận động để bắt đầu ngày mới.",
                reminder = "Trước 15 phút", repetition = "Hàng ngày",
                subtasks = listOf(Subtask("warmup", "Khởi động", true), Subtask("workout", "Tập thể dục 30 phút", true))),
            TaskItem("reading", "Đọc 20 trang sách", TaskCategory.LIFE, "Hôm nay", "21:00", completed = true,
                notes = "Đọc 20 trang sách và ghi lại những ý chính.",
                reminder = "Trước 15 phút", repetition = "Hàng ngày",
                subtasks = listOf(Subtask("read", "Đọc 20 trang sách", true), Subtask("notes", "Ghi lại ý chính", true)))
        ),
        list = TaskListLabels("Danh sách", "%d việc chưa xong", "Tìm công việc", "CẦN LÀM", "ĐÃ XONG", "Gấp", "Không tìm thấy công việc"),
        detail = TaskDetailLabels(
            "Danh sách", "Sửa", "Hạn chót", "Nhắc nhở", "Lặp lại", "Ghi chú", "Việc nhỏ",
            "Thêm việc nhỏ", "Thêm", "Đánh dấu hoàn thành", "Đánh dấu chưa hoàn thành", "Xóa công việc",
            "Xóa công việc?", "Bạn muốn xóa công việc này khỏi danh sách?", "Xóa", "Hủy", "Lưu", "Tên công việc", "Sửa việc nhỏ"
        )
    )
}
