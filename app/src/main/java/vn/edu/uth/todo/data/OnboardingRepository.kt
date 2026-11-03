package vn.edu.uth.todo.data

import vn.edu.uth.todo.R
import vn.edu.uth.todo.model.AppBrand
import vn.edu.uth.todo.model.Illustration
import vn.edu.uth.todo.model.OnboardingContent
import vn.edu.uth.todo.model.OnboardingLabels
import vn.edu.uth.todo.model.OnboardingPage

interface OnboardingRepository {
    fun getContent(): OnboardingContent
}

/** The single source of content for the four screens from UTH Todo V2. */
class FigmaOnboardingRepository : OnboardingRepository {
    override fun getContent() = OnboardingContent(
        brand = AppBrand(
            university = "UTH",
            product = "Todo",
            tagline = "Việc học, việc nhà — gọn gàng mỗi ngày",
            version = "Phiên bản 2.1.0",
            splashLogo = R.drawable.logo_splash,
            headerLogo = R.drawable.logo_header
        ),
        pages = listOf(
            OnboardingPage(
                id = 1,
                title = "Mọi việc, gọn trong một\nchỗ",
                description = "Ghi lại bài tập, việc nhà, lịch hẹn. Không\ncòn quên việc nào.",
                illustration = Illustration(R.drawable.illustration_1, "Danh sách ba công việc, một việc đã hoàn thành")
            ),
            OnboardingPage(
                id = 2,
                title = "Chia theo Học tập và\nCuộc sống",
                description = "Mỗi việc có nhóm riêng, nhìn là biết hôm\nnay cần lo gì.",
                illustration = Illustration(R.drawable.illustration_2, "Hai nhóm công việc: Học tập và Cuộc sống")
            ),
            OnboardingPage(
                id = 3,
                title = "Nhắc đúng lúc",
                description = "Bật thông báo để UTH Todo nhắc bạn\ntrước hạn chót.",
                illustration = Illustration(R.drawable.illustration_3, "Chuông nhắc: Ôn bài Toán lúc 19:00")
            )
        ),
        labels = OnboardingLabels(
            skip = "Bỏ qua",
            next = "Tiếp tục",
            start = "Bắt đầu ngay",
            completed = "Bạn đã sẵn sàng sử dụng UTH Todo!",
            progress = "Giới thiệu"
        )
    )
}
