# API công việc và Firebase Realtime Database

Bản tích hợp ngày 08/11/2026 dùng Kotlin, Jetpack Compose và các lớp dữ liệu OOP. Giữ nguyên bố cục màn đã thiết kế; nội dung công việc và thống kê được tính từ dữ liệu thực.

## Cấu hình

- GET `https://share.tryapps.store/mock/teach/tasks`.
- Header `Authorization: Bearer <token được cấp>`; không thêm dấu `\` vào token.
- Database `https://to-do-lists-a558c-default-rtdb.asia-southeast1.firebasedatabase.app`.
- `app/google-services.json` phải thuộc project Firebase `to-do-lists-a558c`, Android package `vn.edu.uth.todo`.
- Google/Facebook dùng Firebase Authentication hiện có.

Máy hiện tại đã được đặt đúng token người dùng cung cấp trong `local.properties`. File này bị Git bỏ qua. Khi clone trên máy khác, sao chép `local.properties.example` thành `local.properties`, sửa `sdk.dir` và thay `YOUR_BEARER_TOKEN` bằng token được cấp. Android Studio đọc file này khi Gradle Sync/build. Có thể dùng biến môi trường `TASKS_API_TOKEN` và `FIREBASE_DATABASE_URL` để ghi đè.

Không đưa token thật vào README, file ví dụ hoặc commit. BuildConfig đưa token vào APK để gọi API bài tập; đây không phải cơ chế giữ bí mật khỏi người có APK.

## Luồng dữ liệu

1. Đăng nhập thành công: đọc `users/{uid}/taskData` bằng UID từ Firebase Authentication.
2. Nếu chưa có `initialized: true`, gọi API rồi khởi tạo collection bằng transaction. Hai thiết bị khởi tạo đồng thời vẫn giữ collection đã được tạo trước.
3. Các lần mở sau đọc Firebase và lắng nghe thay đổi realtime. Không tải lại dữ liệu mẫu đè lên những thay đổi người dùng đã lưu.
4. Hoàn thành/mở lại, sửa tên/ghi chú, thêm/sửa/xóa việc nhỏ gọi PATCH; xóa công việc gọi DELETE trên API. Sau khi API xác nhận, lưu/xóa đúng khóa Firebase và chờ xác nhận. Thông báo lỗi nếu một bước thất bại; không báo đã lưu khi chưa nhận xác nhận.
5. Cờ `initialized` vẫn còn khi xóa hết công việc, nên danh sách trống không tự sinh lại sáu việc từ API.
6. Cache riêng theo UID giúp hiển thị dữ liệu đã tải khi lỗi kết nối. Khi chưa đọc được Firebase, có thể hiển thị API/cache kèm lỗi và nút **Thử lại**; không cho giả vờ lưu lên Firebase.
7. **Dùng thử** lấy API lần đầu, gọi API chi tiết/cập nhật/xóa theo flow mới và giữ thay đổi trong cache riêng trên thiết bị. Không đọc/ghi dữ liệu tài khoản Firebase.
8. Đăng xuất hủy listener, xóa dữ liệu đang hiển thị và trạng thái màn của tài khoản. Đăng nhập tài khoản khác chỉ mở cache/path của UID mới.

Nút **Trang chủ** và **Danh sách** tải lại collection Firebase. Khi đang tải, mở Danh sách tiếp tục dùng lần tải đó. **Thử lại** chạy lại bước tải khi có lỗi. Thêm công việc lớn mới vẫn thuộc phạm vi bài sau; các thao tác hiện có trên danh sách/chi tiết đã dùng dữ liệu chung. Flow Danh sách: [TASK-LIST-FLOW.md](TASK-LIST-FLOW.md).

Mở Chi tiết gọi GET đúng ID; Thử lại trong Chi tiết tải lại chi tiết đó. Dữ liệu đã sửa trong Firebase được ưu tiên trước mẫu GET cũ. Chi tiết về PATCH/DELETE, phản hồi mẫu sai ID của API và xử lý lỗi hai bước: [TASK-DETAIL-FLOW.md](TASK-DETAIL-FLOW.md).

## Cấu trúc database

```text
users
  {FirebaseAuth.uid}
    taskData
      initialized: true
      schemaVersion: 1
      source: https://share.tryapps.store/mock/teach/tasks
      seededAt: <server timestamp>
      tasks
        1
          id: "1"
          title: ...
          category: study
          date: 2026-10-20
          time: 09:00
          priority: high
          remind: true
          done: true
          note: ...
          repetition: Không
          position: 1
          subtasks
            1_0
              id: 1_0
              title: ...
              done: true
              position: 0
```

ID số của API chuyển sang chuỗi để dùng làm khóa Firebase. Thứ tự công việc/việc nhỏ được lưu bằng `position`; việc nhỏ mới dùng UUID. Không lưu Bearer Token hoặc token đăng nhập vào collection.

`database.rules.json` giới hạn đọc/ghi ở `users/$uid/taskData` với điều kiện `auth != null && auth.uid === $uid`, kiểm tra các trường dữ liệu công việc. Root từ chối đọc/ghi. Người dùng đã sao chép rules này vào Console và Publish. `firebase.json` và `.firebaserc` giữ cùng cấu hình cho Firebase CLI khi cần triển khai lại; không cần service-account key trong ứng dụng.

Để xem dữ liệu: Firebase Console → **Realtime Database → Data → users → UID → taskData → tasks**. UID đối chiếu trong **Authentication → Users**. Collection xuất hiện sau lần đăng nhập và tải thành công đầu tiên; dùng thử không tạo collection này.

## Thống kê và ngày trong API

API đã được gọi thật và trả HTTP 200 với sáu công việc. Mẫu ngày 20/10/2026 có bốn việc, hai hoàn thành, tiến độ 50%; toàn bộ sáu việc có hai đã xong, bốn đang chờ, tỷ lệ 33%. Học tập và Cuộc sống đều còn hai việc. Những con số này được tính, không viết cố định trên UI.

`TaskStatisticsCalculator` dùng ngày hiện tại theo múi giờ Việt Nam nếu có công việc trong ngày đó; nếu không, dùng ngày sớm nhất trong dữ liệu. API bài tập chứa các ngày cố định tháng 10/2026, nên UI ghi rõ **Tiến độ ngày 20/10** và **Việc ngày 20/10** khi xem ở ngày khác. Không đổi ngày gốc của công việc. Hồ sơ tổng hợp toàn bộ công việc và biểu đồ tuần chứa ngày đang hiển thị. Danh sách trống hiển thị 0/0 và 0%.

## Các lớp chính

| Lớp/interface | Trách nhiệm |
|---|---|
| `ApiTaskDto`, `ApiSubtaskDto`, `MockTaskApi` | Đọc JSON, kiểm tra schema, ánh xạ sang đối tượng miền |
| `TaskHttpTransport`, `UrlConnectionTaskTransport` | GET HTTPS với Authorization, timeout và mã HTTP |
| `TaskItem`, `Subtask`, `TaskBoard` | Công việc, việc nhỏ, tìm kiếm/lọc và chuyển trạng thái |
| `FirebaseTaskRecord`, `FirebaseSubtaskRecord` | Đối tượng lưu/đọc Realtime Database có constructor mặc định |
| `TaskCloudStore`, `FirebaseTaskStore` | Transaction khởi tạo, listener realtime, ghi/xóa đúng khóa |
| `TaskCache`, `SharedPreferencesTaskCache` | Cache riêng theo chủ sở hữu |
| `TaskSyncRepository`, `ApiFirebaseTaskRepository` | Điều phối API → Firebase và fallback |
| `TaskDataStatus`, `TaskDataException` | Trạng thái tải/lưu, lỗi và thử lại |
| `TaskDates`, `TaskStatisticsCalculator` | Ngày, tiến độ, thống kê danh mục và hồ sơ |
| `TodoViewModel` | Điều phối phiên, UI, lưu dữ liệu, đổi tài khoản và điều hướng |

Composable chỉ nhận dữ liệu/callback, không tự gọi API/Firebase.

## Chạy kiểm tra

```powershell
$env:JAVA_HOME = 'C:/Users/Acer/.jdks/jbr-17.0.14'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Các unit test kiểm tra schema API thực, ánh xạ Firebase, thống kê, lỗi HTTP/mạng, khởi tạo một lần, bảo toàn dữ liệu đã sửa/xóa và cách ly cache. Các test Compose kiểm tra tải/lỗi/thử lại, tiến độ cập nhật, chờ xác nhận ghi, tài khoản khác, dùng thử và các màn công việc/đăng nhập hiện có.

`TaskLiveIntegrationTest` gọi API thật. Hai test Firebase yêu cầu thiết bị đang đăng nhập tài khoản thật: kiểm tra ghi/đọc/xóa một bản ghi tạm duy nhất của chính UID đó và từ chối đọc UID khác. Test không xóa collection, không đăng xuất hoặc tạo tài khoản. Khi chưa đăng nhập, hai test này được bỏ qua bằng JUnit assumption; dòng `OK (3 tests)` của runner một mình không đủ chứng minh cả ba đã chạy.

Kết quả kiểm tra cuối cùng được ghi ở phần cập nhật README; báo cáo phát triển nằm trong `app/build/reports/` (không đưa thư mục build lên Git).

## Kiểm tra trên tài khoản thật

1. Chạy bản app mới và **đăng nhập Google** (không chọn Dùng thử).
2. Chờ hết trạng thái đang tải. Nếu có lỗi quyền Firebase, kiểm tra Rules đã Publish và nhấn **Thử lại**.
3. Vào Realtime Database → Data, mở `users/{UID}/taskData/tasks`. Tài khoản chưa có collection sẽ được nhập sáu công việc từ API.
4. Trên Trang chủ đánh dấu một việc chưa xong; kiểm tra `done` của đúng khóa đổi sang `true`, tiến độ đổi theo. Thử mở chi tiết và sửa ghi chú/việc nhỏ, rồi mở lại app để kiểm tra dữ liệu được giữ.
5. Có thể đổi `done` một công việc của chính tài khoản trong Console và quan sát app cập nhật realtime. Xóa một việc bằng app sẽ xóa đúng khóa; việc đã xóa không tự xuất hiện lại khi mở Trang chủ.

Kết quả sau khi người dùng đăng nhập Google trên máy ảo ngày 08/11/2026: **cả ba test tích hợp thật đều chạy và đạt**, không có test bị bỏ qua. Đã xác nhận gọi API, ghi/đọc/xóa một công việc tạm và bảo toàn tập ID công việc ban đầu; Rules từ chối đọc UID khác. Bản sửa cũng nhận diện đúng lỗi `Permission denied` dạng `Exception` thường do `DatabaseReference.get()` trả về, ngoài lỗi `DatabaseError` của listener/transaction. Báo cáo: `app/build/reports/task-live-integration-signed-in.txt`.

Không cần gửi mật khẩu, ID token hoặc service-account key để kiểm tra: đăng nhập trực tiếp trên thiết bị là đủ.
