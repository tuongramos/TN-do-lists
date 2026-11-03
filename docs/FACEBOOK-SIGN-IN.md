# Facebook Login qua Firebase

## Cấu hình Android đã thêm

- Meta App ID: `1121810017102411`.
- SDK: `com.facebook.android:facebook-login:19.0.0`.
- App ID và Client Token nằm trong `app/src/main/res/values/facebook.xml`, được tham chiếu từ manifest. Không có App Secret trong ứng dụng.
- `FacebookActivity`, `CustomTabActivity`, scheme `fb1121810017102411` và package visibility cho Facebook đã được khai báo.
- Cửa sổ đăng nhập dùng Activity Result API của SDK, hỗ trợ Facebook app hoặc trình duyệt dự phòng.
- Chỉ yêu cầu `public_profile`. Meta đã báo `Invalid Scopes: email` khi thử tài khoản thật, nên bản hiện tại bỏ quyền email chưa khả dụng của ứng dụng Meta. Firebase tiếp tục xác thực bằng access token; hồ sơ ghi rõ **Facebook chưa chia sẻ email** nếu không có email.

## Người dùng cần hoàn tất trên Meta và Firebase

### 1. Thêm nền tảng Android trong Meta

Mở ứng dụng Meta → **App settings / Cài đặt ứng dụng → Basic / Basic information** → cuộn xuống **Add Platform / Thêm nền tảng** → **Android**.

| Trường | Giá trị cho bản debug hiện tại |
|---|---|
| Google Play Package Name / Package name | `vn.edu.uth.todo` |
| Class Name | `vn.edu.uth.todo.MainActivity` |
| Key Hashes | `3/XjmzYER5vuA+qx+TWjLc6SM0A=` |

Lưu cấu hình. Key Hash trên được tính từ chính chứng chỉ ký debug ở `D:/Androidstudio/.android/debug.keystore`. Nếu dùng chứng chỉ khác hoặc phát hành bản release, phải thêm Key Hash tương ứng.

Ở giao diện hiện tại, mục Android có **Add app store**: chọn **Google Play Store** để hiện Package name và Class name. Nhãn **Main Hash** trên trình duyệt được dịch tương ứng với Key Hashes. Với ô dạng danh sách (Key Hash/Package name), nhập xong nhấn Enter để xác nhận thành mục rồi mới lưu; chữ còn đang gõ có thể chưa được form ghi nhận.

### 2. Bật provider Facebook trong Firebase

Firebase Console → dự án `to-do-lists-a558c` → **Authentication → Sign-in method → Facebook**:

1. Bật **Enable**.
2. App ID: `1121810017102411`.
3. App Secret: sao chép từ Meta → **App settings → Basic** rồi nhập trực tiếp tại Firebase.
4. Nhấn **Save**.

App Secret dùng để cấu hình Firebase, không chèn vào Kotlin, XML hoặc APK.

### 3. OAuth redirect URI

Trong Meta → **Use cases → Facebook Login → Customize → Settings**, bật Client OAuth Login và Web OAuth Login. Thêm đúng địa chỉ sau vào **Valid OAuth Redirect URIs**, rồi lưu:

`https://to-do-lists-a558c.firebaseapp.com/__/auth/handler`

### 4. Tài khoản dùng thử

Trong giai đoạn Development, dùng tài khoản có vai trò trên ứng dụng Meta (admin/developer/tester). Tài khoản được mời làm tester cần chấp nhận lời mời. Việc cho tài khoản công khai sử dụng phụ thuộc các yêu cầu phát hành/quyền truy cập Meta hiển thị cho ứng dụng.

## Chạy và kiểm tra

1. Sync Gradle, chạy app hoặc cài APK debug mới.
2. Mở đăng nhập → **Tiếp tục với Facebook**.
3. Người dùng tự đăng nhập và chấp nhận quyền trên cửa sổ Facebook.
4. Chỉ sau khi Firebase xác thực thành công, app thông báo thành công và chuyển Home.
5. Nhấn avatar hoặc Cài đặt: hồ sơ hiển thị tên, email (nếu Facebook cung cấp) và nhãn liên kết Facebook.
6. Firebase Console → **Authentication → Users**: kiểm tra UID và provider Facebook.
7. Đăng xuất: về màn đăng nhập; Firebase và Meta SDK được xóa phiên. Tác vụ dọn trạng thái Google được chờ tối đa 3 giây để không giữ người dùng ở trạng thái loading. Mở lại app sau khi đã đăng nhập sẽ khôi phục người dùng từ Firebase.

Số liệu công việc vẫn là dữ liệu mẫu trong repository. Chưa tích hợp đồng bộ Firestore.

## Luồng OOP và xử lý trạng thái

1. `TodoViewModel` tạo `FacebookSignInRequest` có ID, khóa thao tác đăng nhập khác trong khi chờ.
2. `FacebookSignInClient` thuộc Activity, mở cửa sổ SDK bằng Activity Result API; không giữ Activity trong ViewModel/repository.
3. Callback trả về `FacebookSignInResult.Token`, `Cancelled` hoặc `Failure`. Token không ghi log hoặc SavedStateHandle; `toString()` được che.
4. ViewModel chỉ nhận callback trùng ID còn đang chờ; callback cũ/trùng bị bỏ qua.
5. `FirebaseAuthRepository` chuyển token bằng `FacebookAuthProvider.getCredential()` rồi gọi `signInWithCredential().await()`.
6. `AuthUserMapper` ánh xạ người dùng Firebase thành `UserProfile`, dùng chung với Google.
7. Hủy bỏ loading và giữ Login. Lỗi SDK, mạng, credential hoặc xung đột tài khoản hiển thị thông báo; Firebase thất bại không mở Home. Xung đột email yêu cầu dùng phương thức đăng nhập đã đăng ký, không tự liên kết tài khoản.
8. Nếu Activity tạo lại trong lúc cửa sổ Facebook đang mở, client lưu ID đang chờ để nhận kết quả qua launcher mới. Sau khi tiến trình bị hủy, token không được khôi phục từ SavedState; người dùng có thể thử đăng nhập lại.

## Chẩn đoán cấu hình

- **Invalid key hash:** thêm Key Hash của đúng chứng chỉ ký APK vào nền tảng Android trong Meta.
- **App not active / unavailable:** kiểm tra vai trò tài khoản và trạng thái/quyền truy cập ứng dụng Meta.
- **Redirect URI mismatch:** đối chiếu URI với Firebase, không thêm dấu `/` cuối.
- **Provider chưa bật:** bật Facebook trong Firebase và lưu App ID/App Secret của cùng ứng dụng Meta.
- **Email đang dùng phương thức khác:** thử phương thức đã đăng ký trước (ví dụ Google); liên kết các provider là luồng riêng chưa thuộc phạm vi này.
- **Invalid Scopes: email / Phạm vi quyền không hợp lệ: email:** đóng cửa sổ Facebook cũ và chạy APK mới chỉ yêu cầu `public_profile`. Nếu sau này muốn xin email, cần cấu hình quyền `email` trong trường hợp sử dụng Facebook Login trên Meta và bảo đảm quyền khả dụng cho tài khoản thử nghiệm trước khi thêm lại scope vào mã.

## Kiểm tra ngày 02/11/2026

- `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest`: thành công.
- **20 unit test** đạt; bổ sung việc che Facebook access token và ánh xạ tài khoản không có email.
- **25 test Android** đạt trên Android 15, bao gồm 7 test Facebook: thành công/hồ sơ/đăng xuất, hủy rồi thử lại, lỗi SDK, xung đột tài khoản Firebase, chờ xác thực trước khi chuyển Home, chặn callback trùng/cũ và khôi phục người dùng Facebook trong ViewModel mới.
- Kiểm thử dùng thử thật qua MainActivity vẫn giữ đúng trạng thái sau Activity recreation và đăng xuất.
- Lint: **0 lỗi, 15 cảnh báo**; không thay đổi bố cục các màn Figma.
- APK debug mới: `app/build/outputs/apk/debug/app-debug.apk`.

Các test xác thực dùng kết quả SDK và repository giả lập; không đăng nhập tài khoản Facebook thật hoặc tạo bản ghi Authentication thật. Người dùng đã bật Google/Facebook trong Firebase và xác nhận Google đăng nhập bình thường. Facebook vẫn cần được thử lại bằng APK đã bỏ scope email.

## Xác minh bản sửa Invalid Scopes: email

Người dùng đã xác nhận Google đăng nhập bình thường. Ảnh lỗi Facebook cho thấy Meta từ chối scope `email`; bản sửa chỉ yêu cầu `public_profile`, đồng thời hiển thị nhãn thiếu email từ đối tượng `ProfileContent` khi cần.

- Build APK và test APK thành công, **20 unit test đạt**, lint **0 lỗi / 15 cảnh báo**.
- Cài đè APK bằng `adb install -r` trên máy ảo đang mở, giữ dữ liệu ứng dụng.
- Chạy riêng hai lớp `FacebookSignInUiTest` và `GoogleSignInUiTest` qua AndroidJUnitRunner: **14 test đạt** (8 Facebook, 6 Google), gồm đăng nhập Facebook không có email.
- Báo cáo lần chạy: `app/build/reports/facebook-email-fix-instrumentation.txt`.
- Các test vẫn dùng repository/token giả lập; người dùng cần đóng cửa sổ Facebook cũ và thử đăng nhập thật với APK mới.

## Nguồn chính thức

- [Firebase Authentication: Facebook Login trên Android](https://firebase.google.com/docs/auth/android/facebook-login)
- [FirebaseUser: email có thể không được provider cung cấp](https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseUser#getEmail())
- [Meta SDK 19: LoginManager và Activity Result API](https://github.com/facebook/facebook-android-sdk/blob/sdk-version-19.0.0/facebook-common/src/main/java/com/facebook/login/LoginManager.kt)
- [Firebase Android Quickstart: cấu hình Facebook](https://github.com/firebase/quickstart-android/tree/master/auth#facebook-login-setup)
