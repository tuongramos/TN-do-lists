# UTH Todo — Navigation + OOP + giao diện Authentication

Dự án Kotlin / Jetpack Compose gồm **7 màn**: Start, 3 màn onboarding, đăng nhập Google/Facebook, trang chủ và hồ sơ. Ba màn mới dùng các SVG người dùng gửi ngày 02/11/2026. **Google và Facebook đã được nối với Firebase Authentication**. Google dùng Credential Manager; Facebook dùng Meta SDK và App ID/Client Token đã cung cấp. Hoàn tất cấu hình nền tảng Android trong Meta và bật Facebook provider trong Firebase theo `docs/FACEBOOK-SIGN-IN.md` trước khi thử tài khoản thật.

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
- **Dùng thử không cần tài khoản** mở trang chủ bằng dữ liệu mẫu. Nhấn avatar hoặc Cài đặt để mở hồ sơ; Đăng xuất trở về đăng nhập.
- Hồ sơ dùng thử hiển thị nhãn **Chế độ dùng thử**. Compose Preview dùng dữ liệu Google trong SVG để đối chiếu bản thiết kế.
- Khi `AuthRepository` trả về thành công, ứng dụng thông báo thành công, mở trang chủ và hiển thị thông tin người dùng được trả về; lỗi hiển thị hộp thoại, hủy giữ nguyên màn đăng nhập.
- ViewModel và SavedStateHandle giữ trang hiện tại khi Activity được tạo lại. Lần khởi động mới khôi phục người dùng từ Firebase Auth; người đã đăng nhập vào thẳng trang chủ. Đăng xuất xóa phiên Firebase, phiên Meta SDK và yêu cầu xóa trạng thái Credential Manager.
- Các chức năng ngoài ba màn mới như thêm việc, danh sách riêng, sửa hồ sơ và đồng bộ chỉ thông báo phạm vi bài tiếp theo.

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

Font phát hành theo SIL Open Font License, xem `docs/FONT-LICENSE.txt` và [kho Google Fonts](https://github.com/google/fonts/tree/main/ofl/bevietnampro).
