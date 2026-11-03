# Google Sign-In qua Firebase — bước 6

## Cách chạy

1. Mở dự án trong Android Studio và Sync Gradle với JDK 17.
2. Chạy trên điện thoại hoặc máy ảo có Google Play Services.
3. Đi qua onboarding, mở màn đăng nhập và nhấn **Tiếp tục với Google**.
4. Chọn tài khoản, xác nhận màn đồng ý chia sẻ thông tin nếu Google yêu cầu.
5. Sau khi Firebase xác thực thành công, ứng dụng thông báo thành công và mở trang chủ. Hồ sơ hiển thị tên/email/chữ viết tắt của người dùng thật.
6. Nhấn Đăng xuất để trở về đăng nhập. Lần đăng nhập sau có thể chọn lại tài khoản.

Việc đăng nhập tài khoản Google thật cần chủ tài khoản chọn/xác nhận trên thiết bị. Các test tự động dùng fake token và repository, không sử dụng thông tin đăng nhập hoặc tạo tài khoản Firebase thật.

## Cấu hình đã kiểm tra

- Package `vn.edu.uth.todo` có trong `app/google-services.json`.
- Có OAuth client loại Web, dùng qua resource `default_web_client_id` do Google Services plugin sinh ra.
- OAuth client Android khớp SHA-1 của chứng chỉ debug hiện tại.
- Quyền INTERNET và các dependency Firebase Auth, Credential Manager, Google ID, coroutines Tasks đã được thêm.
- Kotlin và Compose compiler cập nhật đồng bộ từ 2.0.21 lên **2.4.21** vì Firebase Auth 25.0.0 sử dụng metadata Kotlin 2.4. Giữ Gradle 8.10.2, AGP 8.7.3 và JVM target 17.
- **Cập nhật sau bước Google:** Facebook SDK đã được tích hợp bằng App ID/Client Token đã cung cấp; cấu hình Console và cách thử ở `FACEBOOK-SIGN-IN.md`.

## Kiến trúc OOP

1. `TodoViewModel.signIn(GOOGLE)` tạo `GoogleSignInRequest` có ID, khóa nút trong lúc đang xử lý.
2. `TodoApp` nhận yêu cầu và gọi `GoogleCredentialManagerClient` bằng Activity hiện tại.
3. Client dùng `GetSignInWithGoogleOption` cho nút bấm; kiểm tra loại `CustomCredential`, đọc Google ID token.
4. Kết quả được trả về `completeGoogleSignIn(id, result)`. Kết quả cũ hoặc trùng ID đã xử lý bị bỏ qua.
5. `FirebaseAuthRepository` dùng `GoogleAuthProvider` và `signInWithCredential(...).await()` để Firebase xác minh token.
6. `AuthUserMapper` chuyển UID, tên và email thành `UserProfile`. Không sử dụng người dùng mẫu cho phiên xác thực.
7. Repository được tiêm vào ViewModel bằng factory có `createSavedStateHandle()`; không giữ Activity trong ViewModel hoặc repository.

ID token chỉ tồn tại tạm thời trong lời gọi, không được ghi vào SavedStateHandle hoặc log. Trạng thái chuỗi token khi chuyển thành string được che. Khi Activity tạo lại, UI có thể mở lại yêu cầu đang chờ; luồng cũ bị hủy theo coroutine của composition.

## Các trường hợp xử lý

| Trường hợp | Hành vi |
|---|---|
| Người dùng hủy chọn tài khoản | Dừng loading, giữ màn đăng nhập, không hiện lỗi |
| Không có credential/tài khoản phù hợp | Hộp thoại hướng dẫn kiểm tra tài khoản Google trên thiết bị |
| Thiếu hoặc lỗi dịch vụ provider | Thông báo kiểm tra/cập nhật Google Play Services |
| Credential trả về sai loại/token rỗng | Không gọi xác thực Firebase, hiển thị lỗi |
| Firebase báo lỗi mạng/xung đột tài khoản/quá nhiều yêu cầu | Thông báo lỗi tương ứng, có thể thử lại |
| Firebase xác thực thành công | Thông báo thành công và mở trang chủ |
| Mở ứng dụng với phiên Firebase đã lưu | Khôi phục người dùng thật và vào trang chủ |
| Đăng xuất | Xóa phiên Firebase; xóa trạng thái chọn tài khoản Google theo khả năng provider |

Các số liệu công việc và thống kê vẫn lấy từ repository dữ liệu mẫu của bài giao diện. Bước này chỉ tích hợp tài khoản Google, chưa thêm Firestore hoặc đồng bộ công việc.

## Xác nhận trên Firebase Console

Sau một lần đăng nhập tài khoản thật thành công, mở Firebase Console → Authentication → Users. Kiểm tra UID, email và provider Google. Ứng dụng chỉ chuyển Home khi nhận được người dùng từ Firebase.

Nếu có lỗi cấu hình, kiểm tra package, SHA-1, Web client ID và Google provider trong cùng Firebase project. Sau khi sửa OAuth trong Console, tải lại `google-services.json`, Sync rồi build lại. Không ghi token vào Logcat để chẩn đoán. Log `UTHTodoAuth` chỉ ghi mã lỗi Firebase.

## Kết quả kiểm tra ngày 02/11/2026

- `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug`: thành công.
- **18 unit test** đạt: điều hướng, hợp đồng xác thực, ánh xạ UID/tên/email và che token khi chuyển thành chuỗi.
- **18 test Android** đạt trên máy ảo Android 15: giao diện cũ, luồng dùng thử, Google thành công/hủy/lỗi, khôi phục phiên, bỏ qua callback trùng hoặc cũ và đăng xuất.
- Lint: **0 lỗi, 14 cảnh báo**. Các cảnh báo gồm phiên bản thư viện mới hơn, khóa chiều màn hình, hàm Modifier có sẵn và detector Credential Manager không nhận diện phần xử lý `GoogleIdTokenCredential` dù mã đã kiểm tra loại rồi đọc token.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.

Các kết quả tự động xác minh luồng ứng dụng qua fake repository và fake kết quả chọn tài khoản. Chưa thực hiện đăng nhập bằng tài khoản Google thật hoặc xác nhận bản ghi trong Authentication → Users; làm theo mục Cách chạy để kiểm tra kết nối thực tế.

## Tài liệu chính thức

- [Firebase Google Authentication](https://firebase.google.com/docs/auth/android/google-signin)
- [Android Credential Manager — button flow](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation)
- [Kotlin/Gradle/AGP compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
