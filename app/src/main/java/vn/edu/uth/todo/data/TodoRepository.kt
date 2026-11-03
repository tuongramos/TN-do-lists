package vn.edu.uth.todo.data

import vn.edu.uth.todo.R
import vn.edu.uth.todo.model.*

interface TodoRepository { fun getContent(): TodoContent }

class FigmaTodoRepository : TodoRepository {
    override fun getContent() = TodoContent(
        login = LoginContent(
            title = "Chào mừng đến với\nUTH Todo",
            description = "Đăng nhập để lưu công việc và đồng bộ\ntrên mọi thiết bị của bạn.",
            benefits = listOf(
                LoginBenefit("Tự động sao lưu, không lo mất dữ liệu", R.drawable.auth_sync),
                LoginBenefit("Dùng trên iPhone, iPad và Mac", R.drawable.auth_device)
            ),
            options = listOf(
                SignInOption(AuthProvider.GOOGLE, "Tiếp tục với Google", "G"),
                SignInOption(AuthProvider.FACEBOOK, "Tiếp tục với Facebook", "f")
            ),
            divider = "hoặc",
            guestLabel = "Dùng thử không cần tài khoản",
            termsPrefix = "Khi tiếp tục, bạn đồng ý với ",
            terms = "Điều khoản sử dụng",
            termsJoin = " và ",
            privacyFirstLine = "Chính",
            privacySecondLine = "sách quyền riêng tư",
            termsSuffix = " của UTH Todo."
        ),
        dashboard = DashboardContent(
            date = "Thứ Ba, 20 tháng 10",
            greeting = "Chào UTH!",
            greetingTemplate = "Chào %s!",
            progressTitle = "Tiến độ hôm nay",
            encouragement = "Bắt đầu từ việc nhỏ nhất nhé.",
            progress = DailyProgress(3, 7),
            categories = listOf(
                CategorySummary(TaskCategory.STUDY, "Học tập", 4, R.drawable.study),
                CategorySummary(TaskCategory.LIFE, "Cuộc sống", 3, R.drawable.life)
            ),
            tasks = listOf(
                TodoTask("mobile", "Nộp bài tập Lập trình di động", TaskCategory.STUDY, "09:00", true),
                TodoTask("chapter", "Ôn chương 3 – Cấu trúc dữ liệu", TaskCategory.STUDY, "14:00", false),
                TodoTask("shopping", "Đi chợ mua đồ cho tuần", TaskCategory.LIFE, "17:30", false),
                TodoTask("exercise", "Tập thể dục 30 phút", TaskCategory.LIFE, "06:30", true)
            ),
            todayTitle = "Việc hôm nay",
            allTasks = "Xem tất cả",
            navigation = listOf(
                NavigationItem(HomeAction.HOME, "Trang chủ", R.drawable.nav_home),
                NavigationItem(HomeAction.LIST, "Danh sách", R.drawable.nav_list),
                NavigationItem(HomeAction.ADD, "Thêm công việc", R.drawable.home_add),
                NavigationItem(HomeAction.NOTIFICATIONS, "Thông báo", R.drawable.nav_bell),
                NavigationItem(HomeAction.PROFILE, "Cài đặt", R.drawable.nav_settings)
            )
        ),
        profile = ProfileContent(
            user = UserProfile("figma-reference", "Minh Anh", "minhanh@gmail.com", "MA", AuthProvider.GOOGLE),
            back = "Cài đặt", title = "Hồ sơ", edit = "Sửa",
            linkedPrefix = "Đã liên kết tài khoản",
            guestBadge = "Chế độ dùng thử",
            statistics = listOf(
                ProfileStatistic("128", "Việc đã xong"),
                ProfileStatistic("12 ngày", "Chuỗi liên tiếp"),
                ProfileStatistic("86%", "Đúng hạn")
            ),
            weekTitle = "Tuần này", weeklyProgress = "18/22 việc",
            week = listOf(WeekDay("T2", 3), WeekDay("T3", 4, true), WeekDay("T4", 2),
                WeekDay("T5", 4), WeekDay("T6", 3), WeekDay("T7", 1), WeekDay("CN", 1)),
            sync = "Đồng bộ dữ liệu", lastSync = "Vừa xong", privacy = "Quyền riêng tư", logout = "Đăng xuất",
            missingFacebookEmail = "Facebook chưa chia sẻ email"
        ),
        labels = AppLabels(
            loginErrorTitle = "Không thể đăng nhập",
            acknowledge = "Đã hiểu",
            signedIn = "Đăng nhập thành công",
            signedOut = "Đã đăng xuất",
            guestMessage = "Bạn đang dùng thử với dữ liệu mẫu, chưa xác thực tài khoản.",
            featurePending = "Chức năng này sẽ được bổ sung trong bài tiếp theo.",
            unexpectedError = "Không thể kết nối. Vui lòng thử lại."
        )
    )
}
