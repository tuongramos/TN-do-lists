# Flow Chi tiết — UI + API

Tiếp tục dự án Kotlin/Jetpack Compose UTH Todo, dùng OOP và Firebase Realtime Database. Theo yêu cầu người dùng, **nút “Danh sách” và Back của Android luôn quay về Danh sách**, cả khi Chi tiết được mở từ Trang chủ. Giữ tìm kiếm và bộ lọc của danh sách khi quay lại.

## API

URL gốc giữ nguyên `https://share.tryapps.store/mock/teach/tasks`; cả ba thao tác dùng header `Authorization: Bearer <token được cấp>` từ `local.properties`/BuildConfig hiện có.

| Thao tác | Request |
|---|---|
| Mở hoặc thử tải lại chi tiết | `GET /mock/teach/tasks/{id}` |
| Sửa tên/ghi chú, hoàn thành/mở lại, CRUD việc nhỏ | `PATCH /mock/teach/tasks/{id}` với JSON công việc đã thay đổi |
| Xác nhận xóa công việc | `DELETE /mock/teach/tasks/{id}` |

PATCH gửi các trường `title`, `note`, `category`, `date`, `time`, `priority`, `remind`, `done`, `subtasks`. Mỗi việc nhỏ gửi `title`/`done` theo hợp đồng API; Firebase giữ thêm ID ổn định cho việc nhỏ. Xóa hết việc nhỏ gửi `subtasks: []`.

Đã kiểm tra endpoint thật: GET ID 1/2 trả đúng công việc, GET ID không tồn tại trả 404. PATCH trả HTTP 200 nhưng nội dung là mẫu cố định (có thể mang ID 2 khi request gửi ID 1). DELETE trả 204 ngay cả với ID không tồn tại. Vì vậy:

- GET phải có JSON hợp lệ và ID trùng ID yêu cầu; không hiển thị nhầm chi tiết.
- PATCH/DELETE dùng HTTP 2xx làm xác nhận, không thay công việc đang sửa bằng đối tượng mẫu trong response PATCH.
- DELETE 404 cũng được chấp nhận khi thử lại một thao tác đã xóa trước đó.
- Mock API không được dùng làm bằng chứng rằng các thay đổi cá nhân đã được lưu bền vững. Firebase là nơi giữ dữ liệu riêng của từng tài khoản.

## API và Firebase

1. Mở chi tiết: gọi GET đúng ID, sau đó đọc dữ liệu đã lưu của UID. Công việc Firebase được ưu tiên để giữ tên/ghi chú/trạng thái và ID việc nhỏ đã sửa, không bị mẫu API cũ ghi đè.
2. Cập nhật: gửi PATCH; khi API xác nhận, ghi đúng công việc vào `users/{uid}/taskData/tasks/{id}` và đợi Firebase xác nhận.
3. Xóa: cần xác nhận trong hộp thoại; gửi DELETE rồi xóa đúng khóa Firebase. Thành công trở về Danh sách; dữ liệu Trang chủ/Danh sách/Hồ sơ cùng cập nhật.
4. API lỗi thì không ghi/xóa Firebase. Nếu API đã xử lý nhưng Firebase thất bại, ứng dụng báo rõ **“API đã xử lý, nhưng chưa lưu được vào Firebase. Hãy thử lại.”** Hai hệ thống không có một transaction nguyên tử chung.
5. Trong lúc đang ghi, buffer các sự kiện cục bộ của RTDB. Không coi sự kiện optimistic là xác nhận máy chủ, không rời Chi tiết sớm chỉ vì listener vừa báo bản ghi bị xóa cục bộ.
6. Dùng thử cũng gọi GET/PATCH/DELETE nhưng chỉ lưu thay đổi vào cache khách, không ghi collection tài khoản Firebase.
7. API/Firebase chưa sẵn sàng: có thể xem bản đã tải ở chế độ chỉ đọc; **Thử lại** trên Chi tiết tải lại đúng chi tiết đó. 404 có trạng thái riêng cùng nút về Danh sách và Thử lại, không tự xóa bản lưu Firebase.

Các lần mở sau vẫn tôn trọng `initialized`, không nhập lại công việc đã xóa. Cấu hình/rules: [API-REALTIME-DATABASE.md](API-REALTIME-DATABASE.md).

## UI và OOP

- Giữ giao diện SVG chi tiết hiện có: font, màu, thẻ hạn/nhắc/lặp, ghi chú, việc nhỏ và các nút cuối. Nội dung dài cuộn theo chiều dọc của khung chi tiết.
- Khóa Sửa, checkbox, nhập/thêm/sửa/xóa việc nhỏ, hoàn thành và xóa trong lúc tải/lưu; nút quay lại vẫn hoạt động.
- Hủy hộp thoại xóa giữ nguyên công việc. Ghi/xóa thất bại giữ dữ liệu đang hiển thị và thông báo lỗi.
- Hủy lần đọc cũ khi quay lại/đổi công việc/đăng xuất; kết quả tải trễ không được đổi sang sai công việc hoặc cập nhật phiên khác.

| Lớp | Trách nhiệm |
|---|---|
| `TaskHttpRequest`, `TaskHttpMethod`, `TaskHttpTransport` | Request GET/PATCH/DELETE, JSON UTF-8, Authorization, timeout |
| `ApiTaskDto`, `ApiSubtaskDto`, `ApiTaskUpdate`, `MockTaskApi` | Ánh xạ đối tượng và hợp đồng API chi tiết |
| `TaskDetailSnapshot`, `TaskDetailStatus` | Chi tiết theo ID, nguồn dữ liệu, tải/lỗi/404 và quyền sửa |
| `ApiFirebaseTaskRepository` | Thứ tự API → Firebase, ưu tiên dữ liệu đã lưu và cache khách |
| `TodoViewModel` | Điều hướng về Danh sách, chặn thao tác trùng, bỏ kết quả cũ, chờ xác nhận |
| `TaskDetailScreen`, `TaskDetailUnavailableScreen` | Nội dung/điều khiển và trạng thái không tìm thấy |

Token không được ghi vào Git, database hoặc log request. `TaskHttpRequest.toString()` không in Bearer Token hay nội dung ghi chú.

## Kiểm tra

- Unit test: GET đúng/sai ID, PATCH JSON gồm mảng việc nhỏ trống, bỏ qua response PATCH mẫu sai ID, DELETE 204/404, lỗi API không chạm Firebase, dữ liệu đã sửa không bị GET mẫu ghi đè, lỗi Firebase sau API và cache khách.
- UI test: quay lại Danh sách từ Trang chủ bằng nút và Android Back, 404/lỗi/thử lại, sửa công việc, CRUD việc nhỏ, chờ ACK khi hoàn thành/xóa, xác nhận/hủy xóa và bỏ kết quả tải trễ.
- Test thực: API GET/PATCH/DELETE trên Android, phiên Google đọc/ghi/xóa một bản ghi Firebase tạm, Rules chặn UID khác và mở Chi tiết thật. PATCH kiểm tra bằng cách gửi lại giá trị hiện tại; DELETE API chỉ thử ID không tồn tại. Không xóa một công việc gốc chỉ để kiểm thử.

## Kết quả xác minh ngày 08/11/2026

- **49 unit test đạt**, build APK/debug Android test thành công; Lint **0 lỗi, 16 cảnh báo**.
- **34 test UI và 4 test tích hợp đạt**: gồm 9 test flow Chi tiết, 14 test đồng bộ Danh sách/Trang chủ, 9 test công việc/hồi quy, 2 test UI dùng dữ liệu Firebase thật và 4 test API/Firebase thật. Không có test tích hợp bị bỏ qua.
- Sau bổ sung buffer sự kiện optimistic, đã chạy lại 23 test Chi tiết/đồng bộ; hoàn thành và xóa chỉ thay đổi màn/dữ liệu sau ACK, các test đều đạt.
- GET/PATCH/DELETE thật chạy thành công từ Android bằng cấu hình URL/token hiện có. Test PATCH gửi lại giá trị hiện tại; DELETE chỉ gọi ID không tồn tại. Test ghi Firebase dùng bản ghi tạm duy nhất rồi dọn lại.
- Đối chiếu lại Chi tiết với dữ liệu SVG: sai số RGB trung bình **2.243/255**, tương đồng RGB **99.12%**. Phép so sánh này kiểm tra bố cục với dữ liệu tham chiếu; ảnh API chứa trạng thái/ngày/ghi chú thực của tài khoản.
- Báo cáo: `app/build/reports/task-detail-flow.txt`, `task-detail-ack-final.txt`, `task-detail-live-ui-final.txt`. Thư mục build bị Git bỏ qua.

[Ảnh Chi tiết dùng GET API và Firebase thật](screenshots/task_detail_api.png) · [Đối chiếu SVG/Android](tasks-comparison.png).
