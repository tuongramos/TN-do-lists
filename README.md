# UTH Todo — Navigation + OOP + giao diện Authentication

Dự án Kotlin / Jetpack Compose gồm **9 màn**: Start, 3 màn onboarding, đăng nhập Google/Facebook, trang chủ, hồ sơ, danh sách và chi tiết công việc. Hai màn công việc dùng SVG người dùng gửi ngày 07/11/2026. **Google và Facebook đã được nối với Firebase Authentication**. Google dùng Credential Manager; Facebook dùng Meta SDK và App ID/Client Token đã cung cấp. Cấu hình provider và cách thử tài khoản thật ở `docs/GOOGLE-SIGN-IN.md` và `docs/FACEBOOK-SIGN-IN.md`.

**Cập nhật 08/11/2026:** danh sách công việc lấy từ API `https://share.tryapps.store/mock/teach/tasks`, khởi tạo một lần và lưu thay đổi riêng từng tài khoản bằng **Firebase Realtime Database**. Trang chủ và hồ sơ tính thống kê từ cùng danh sách; có tải/lỗi/thử lại và cache riêng theo UID. Cấu hình token, đường dẫn database, quy tắc ngày của dữ liệu mẫu và cách kiểm tra nằm trong [API-REALTIME-DATABASE.md](docs/API-REALTIME-DATABASE.md).

Flow **Danh sách (UI + API)** được hoàn thiện trong [TASK-LIST-FLOW.md](docs/TASK-LIST-FLOW.md): tải khi mở tab, tìm kiếm không dấu kết hợp danh mục, phân biệt tải/lỗi/trống/không có kết quả, hoàn thành/mở lại có xác nhận Firebase và giữ tìm kiếm/bộ lọc khi quay lại từ chi tiết.

Xác minh flow Danh sách: **41 unit test + 24 test UI + 3 test tích hợp thật đạt**, build thành công, Lint 0 lỗi. Đối chiếu lại bố cục Danh sách với SVG đạt **99.07% tương đồng RGB**. [Ảnh Danh sách dùng dữ liệu Firebase thật](docs/screenshots/task_list_api.png).

Flow **Chi tiết (UI + API)**: GET/PATCH/DELETE theo ID, lưu bền vững vào Firebase và **quay lại Danh sách** cả khi mở từ Trang chủ. Xem hợp đồng API, phản hồi mẫu của server và cách xử lý lỗi tại [TASK-DETAIL-FLOW.md](docs/TASK-DETAIL-FLOW.md).

Xác minh flow Chi tiết: **49 unit test + 34 test UI + 4 test tích hợp thật đạt**, build thành công, Lint 0 lỗi. Chi tiết đối chiếu lại với SVG đạt **99.12% tương đồng RGB**. [Ảnh Chi tiết dùng dữ liệu Firebase thật](docs/screenshots/task_detail_api.png).

## Mở và chạy trong Android Studio

1. Chọn **Open** và mở thư mục `UTH todo` chứa file `settings.gradle.kts`.
2. Dùng **JDK 17** cho Gradle. Máy hiện tại có `C:\Users\Acer\.jdks\jbr-17.0.14`. Vào **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** nếu cần đổi. Gradle 8.10.2 của dự án cũng hỗ trợ JDK 21.
3. Chờ Gradle Sync, chọn thiết bị Android API 24 trở lên và chạy cấu hình **app**.
4. Xem từng màn trong Compose Preview của `OnboardingScreens.kt` và `AuthScreens.kt`.

SDK hiện tại được cấu hình bằng `local.properties`: `D:\Androidstudio\Sdk`. Nếu mở trên máy khác, Android Studio sẽ tạo lại file này.

## Hành vi

- Start hiển thị 2,4 giây rồi chuyển sang onboarding đầu tiên. Có thể chạm logo để chuyển sớm.
- **Tiếp tục** mở trang kế tiếp.
- **Bỏ qua** đưa tới trang cuối; các dấu chấm hoặc vuốt hình minh họa cho phép đổi trang.
- Nút Back của Android quay lại trang trước.
- **Bắt đầu ngay** mở đăng nhập, có đúng hai nhà cung cấp **Google và Facebook**.
- **Google** mở Credential Manager, xác thực ID token với Firebase, rồi vào trang chủ khi thành công. Hủy sẽ giữ màn đăng nhập; lỗi hiển thị hộp thoại có thể thử lại.
- **Facebook** mở đăng nhập bằng Meta SDK, lấy access token rồi xác thực với Firebase. Thành công mở trang chủ, hủy giữ màn đăng nhập, lỗi có thể thử lại.
- Bản sửa lỗi `Invalid Scopes: email` chỉ yêu cầu `public_profile` trên Facebook. Nếu provider không chia sẻ email, hồ sơ hiển thị thông báo tương ứng; UID/tên vẫn lấy từ tài khoản thật.
- **Dùng thử không cần tài khoản** mở trang chủ bằng dữ liệu API/cache riêng trên thiết bị, không ghi Firebase. Nhấn avatar hoặc Cài đặt để mở hồ sơ; Đăng xuất trở về đăng nhập.
- Hồ sơ dùng thử hiển thị nhãn **Chế độ dùng thử**. Compose Preview dùng dữ liệu Google trong SVG để đối chiếu bản thiết kế.
- Khi `AuthRepository` trả về thành công, ứng dụng thông báo thành công, mở trang chủ và hiển thị thông tin người dùng được trả về; lỗi hiển thị hộp thoại, hủy giữ nguyên màn đăng nhập.
- ViewModel và SavedStateHandle giữ trang hiện tại khi Activity được tạo lại. Lần khởi động mới khôi phục người dùng từ Firebase Auth; người đã đăng nhập vào thẳng trang chủ. Đăng xuất xóa phiên Firebase, phiên Meta SDK và yêu cầu xóa trạng thái Credential Manager.
- **Danh sách** ở thanh điều hướng và **Xem tất cả** trên Trang chủ mở màn danh sách mới: tìm kiếm có/không dấu, lọc Học tập/Cuộc sống và đánh dấu hoàn thành.
- Nhấn bất kỳ công việc trên Trang chủ hoặc Danh sách để mở đúng chi tiết: hạn chót, nhắc nhở, lặp lại, ghi chú và việc nhỏ. Có thể sửa tên/ghi chú, thêm/sửa/xóa hoặc hoàn thành việc nhỏ, đánh dấu hoàn thành và xóa công việc sau xác nhận.
- Các công việc dùng chung đối tượng dữ liệu; ViewModel và SavedStateHandle giữ bộ lọc, tìm kiếm và công việc đang mở khi Android khôi phục Activity. Người đăng nhập đọc/lưu Realtime Database tại `users/{uid}/taskData`; thay đổi được lắng nghe realtime. Dữ liệu đã sửa/xóa không bị API ghi đè khi mở lại.
- Thêm công việc lớn và sửa hồ sơ vẫn thông báo phạm vi bài tiếp theo.

## OOP

| Thành phần | Vai trò |
|---|---|
| `AppBrand`, `Illustration`, `OnboardingPage`, `OnboardingLabels`, `OnboardingContent` | Các đối tượng dữ liệu bất biến |
| `OnboardingRepository` | Interface cung cấp dữ liệu |
| `FigmaOnboardingRepository` | Lớp triển khai, chứa nội dung 4 màn |
| `AppScreen` | Sealed interface mô tả trạng thái màn hình |
| `OnboardingNavigator` | Đóng gói quy tắc chuyển trang, giới hạn chỉ số trang |
| `TodoUiState`, `TodoViewModel` | Đóng gói trạng thái của toàn ứng dụng, kết quả đăng nhập và điều hướng |
| `LoginContent`, `DashboardContent`, `ProfileContent`, `UserProfile`, `TodoTask`, `CategorySummary`, `DailyProgress`, `WeekDay` | Đối tượng dữ liệu cho ba màn mới |
| `TodoRepository`, `FigmaTodoRepository` | Interface và nguồn dữ liệu SVG |
| `AuthRepository`, `AuthResult`, `UnconfiguredAuthRepository` | Ranh giới để tích hợp xác thực, xử lý thành công/lỗi/hủy |
| `FirebaseAuthRepository`, `AuthUserMapper` | Xác thực token Google, khôi phục phiên và ánh xạ người dùng thật |
| `GoogleCredentialManagerClient`, `GoogleSignInRequest`, `GoogleSignInResult` | Mở chọn tài khoản ở UI; truyền kết quả một lần về ViewModel |
| `FacebookSignInClient`, `FacebookSignInRequest`, `FacebookSignInResult` | Activity Result API cho Meta SDK; chuyển access token về repository để Firebase xác thực |
| `TaskItem`, `Subtask`, `TaskBoard`, `TaskFilter`, `TaskPriority` | Đối tượng công việc, việc nhỏ, tiến độ, tìm kiếm và lọc danh sách |
| `TaskRepository`, `FigmaTaskRepository`, `TaskContent` | Nội dung và 6 công việc mẫu cho danh sách/chi tiết từ SVG |
| `TaskApi`, `MockTaskApi`, `ApiTaskDto`, `ApiSubtaskDto` | Lấy và ánh xạ dữ liệu API thực, không phụ thuộc Composable |
| `TaskSyncRepository`, `ApiFirebaseTaskRepository`, `FirebaseTaskStore`, `TaskCache` | Khởi tạo API một lần, đồng bộ Realtime Database, cache theo UID |
| `TaskStatisticsCalculator`, `TaskDataStatus` | Thống kê từ dữ liệu và trạng thái tải/lưu/lỗi |
| `DesignFrame`, `TodoDesign` | Kích thước, màu sắc và kiểu chữ dùng chung |

UI nhận đối tượng nội dung và callback. Tài nguyên minh họa là hình trang trí trích từ SVG gốc.

## Đối chiếu Figma

- Nguồn bài trước: `docs/figma/`. Nguồn đăng nhập, trang chủ, hồ sơ: `docs/figma/auth/`.
- Start: **400 × 860**. Onboarding: **390 × 844**.
- Các file Section có nhãn tên nằm ngoài khung điện thoại. Ứng dụng lấy vùng `x=3, y≈31, w=390, h=844` để bỏ nhãn và bóng của khung ngoài.
- Màu chính `#2F5BEA`; font **Be Vietnam Pro**, đủ trọng lượng 400/500/600/700/800, có sẵn trong ứng dụng và dùng được khi ngoại tuyến.
- Logo, 3 hình minh họa và bóng đổ được xuất từ SVG ở **3x**.
- Tiêu đề 28/36 px, tracking −0,4 px; mô tả 16/24 px; chữ nút 17 px, đậm 600.
- Lề ngang 28 px, logo 32 × 32 px ở (28,66), hình minh họa 280 × 300 px ở (55,171), nút cao 56 px ở y=748, bo góc 16 px.
- Nội dung được bố trí theo hệ tọa độ gốc rồi co giãn đồng đều theo màn hình để giữ tỷ lệ. Thanh hệ thống ẩn để khớp bản thiết kế; vùng thừa trên thiết bị khác tỷ lệ dùng cùng màu nền.
- Bo góc 44 px trong Figma mô phỏng mép điện thoại; màn Android thực tế dùng mép màn hình của thiết bị.

## Kiểm tra

```powershell
$env:JAVA_HOME = 'C:/Users/Acer/.jdks/jbr-17.0.14'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

Unit test kiểm tra chuyển trang, quay lại, bỏ qua và chỉ số khôi phục ngoài phạm vi. Instrumentation test kiểm tra thao tác Compose và lưu ảnh 4 màn để đối chiếu.

APK debug sau build: `app/build/outputs/apk/debug/app-debug.apk`.

Kết quả của bài onboarding trước: `docs/VERIFICATION.md` và `docs/comparison.png`. Thông số ba màn mới và điểm nối Firebase: `docs/AUTH-UI.md`.

Kiểm tra sau tích hợp Google/Facebook ngày 02/11/2026: **20 unit test + 25 test Android đạt**, build thành công, Lint **0 lỗi, 15 cảnh báo**. Các test xác thực dùng repository giả lập; đăng nhập bằng tài khoản thật cần được xác nhận trên thiết bị sau khi hoàn tất cấu hình Console. Ảnh ba màn: `docs/auth-preview.png`; đối chiếu với SVG: `docs/auth-comparison.png`.

Phần tích hợp Google và cách kiểm tra tài khoản thật: `docs/GOOGLE-SIGN-IN.md`. Plugin Kotlin và Compose hiện dùng **2.4.21** để đọc metadata Kotlin 2.4 của Firebase Auth 25.0.0.

Phần Facebook, Key Hash Android và cấu hình Meta/Firebase còn cần hoàn tất: `docs/FACEBOOK-SIGN-IN.md`.

Có thể tái xuất tài nguyên bằng `node tools/export-figma-assets.cjs` sau khi cài `@resvg/resvg-js@2.6.2` trong `tools/`. Công cụ này chỉ dùng khi phát triển; ứng dụng Android không cần Node.js.

Tài nguyên ba màn mới: `node tools/export-auth-assets.cjs`.

Thông số và cách thử hai màn công việc: `docs/TASK-SCREENS.md`. Xuất icon/tham chiếu: `node tools/export-task-assets.cjs`; đối chiếu ảnh: `python tools/compare-task-screens.py`.

Bản công việc ngày 07/11/2026: build thành công, **25 unit test + 9 test công việc + 14 test hồi quy Google/Facebook đạt**, lint 0 lỗi. Ảnh mới: `docs/tasks-preview.png`, đối chiếu: `docs/tasks-comparison.png`; tương đồng theo sai số RGB lần lượt 99.07% và 99.12%.

Bản API + Realtime Database ngày 08/11/2026: **39 unit test, 30 test giao diện và 3 test tích hợp thật đạt**; đã chạy lại 7 test đồng bộ sau bản sửa xử lý quyền ghi, đều đạt. APK/debug Android test build thành công, Lint **0 lỗi, 16 cảnh báo**. GET API thật đạt trên cả máy phát triển và Android. Sau khi người dùng đăng nhập Google trên máy ảo, đã xác nhận ghi/đọc/xóa một công việc tạm của chính tài khoản, bảo toàn các ID công việc ban đầu và chặn đọc UID khác. Đã xử lý cả lỗi quyền dạng `Exception` thường mà `DatabaseReference.get()` có thể trả về. Token thật chỉ nằm trong cấu hình local bị Git bỏ qua.

Font phát hành theo SIL Open Font License, xem `docs/FONT-LICENSE.txt` và [kho Google Fonts](https://github.com/google/fonts/tree/main/ofl/bevietnampro).
