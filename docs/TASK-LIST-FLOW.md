# Flow Danh sách — UI + API

Triển khai trong dự án UTH Todo ngày 08/11/2026, tiếp tục dùng Kotlin/Jetpack Compose, các đối tượng OOP và Firebase Realtime Database của bài Trang chủ.

## Dữ liệu và điều hướng

- API đầu vào giữ nguyên: **GET `https://share.tryapps.store/mock/teach/tasks`**, header **`Authorization: Bearer <token được cấp>`**. Token thật cấu hình trong `local.properties`, không nằm trong Git.
- API trả về đối tượng công việc và việc nhỏ. ID số chuyển thành chuỗi; không dùng ID hoặc công việc mẫu Figma khi chạy thật.
- **Danh sách**, **Xem tất cả** và thẻ danh mục trên Trang chủ mở màn Danh sách. Khi mở/chạm tab Danh sách, ứng dụng tải lại dữ liệu qua repository; không tạo thêm yêu cầu nếu một lần tải đang chạy.
- Repository nhập API một lần cho tài khoản chưa được khởi tạo, sau đó đọc/lắng nghe collection riêng `users/{uid}/taskData` trên Firebase. Mở lại Danh sách không ghi đè những việc đã sửa/xóa bằng dữ liệu mẫu API.
- Nhấn thẻ mở chi tiết theo đúng ID. Quay lại giữ tìm kiếm và bộ lọc; không gọi API mỗi lần gõ ký tự hoặc đổi bộ lọc.
- Chế độ dùng thử giữ thay đổi trong cache của khách trên thiết bị. Tài khoản đăng nhập ghi thay đổi vào Firebase của chính UID đó.

Chi tiết cấu hình database, Rules và token: [API-REALTIME-DATABASE.md](API-REALTIME-DATABASE.md).

## Giao diện và hành vi

Giữ khung **390 × 844**, font Be Vietnam Pro, màu sắc, tọa độ, kích thước thẻ và thanh điều hướng của SVG đã cung cấp. Các trạng thái bổ sung nằm trong vùng danh sách; không thay bố cục các thẻ khi dữ liệu sẵn sàng.

| Tình huống | Hiển thị / hành vi |
|---|---|
| Tải lần đầu, chưa có dữ liệu | Vòng tải và “Đang tải công việc…”, không hiện danh sách mẫu hoặc thông báo trống sai |
| Tải thất bại, chưa có dữ liệu | Lỗi và **Thử lại** trong thân danh sách |
| Collection trống | “Chưa có công việc nào.” |
| Tìm kiếm/bộ lọc không có kết quả | “Không tìm thấy công việc” và **Xóa tìm kiếm và bộ lọc** |
| Có cache khi tải/lỗi | Giữ các thẻ đang có, hiển thị trạng thái phía trên; chỉ cho sửa khi dữ liệu sẵn sàng và có quyền ghi |
| Có dữ liệu | Nhóm **CẦN LÀM** và **ĐÃ XONG**, thẻ danh mục, hạn chót, nhãn **Gấp** cho ưu tiên cao |
| Đang lưu thay đổi | Hiển thị đang lưu, khóa checkbox để không gửi thao tác trùng; chờ xác nhận máy chủ |
| Ghi bị từ chối | Hiển thị lỗi, không báo lưu thành công, cho tải/thử lại |

Ô tìm kiếm hỗ trợ không dấu và không phân biệt chữ hoa/thường, kết hợp cùng bộ lọc Tất cả/Học tập/Cuộc sống. Đổi tìm kiếm hoặc danh mục đưa vùng cuộn về đầu. Danh sách dài cuộn trong vùng nội dung; thanh điều hướng vẫn cố định.

Số **“N việc chưa xong”** trên tiêu đề là tổng số chưa xong trong toàn bộ collection, không bị thay đổi chỉ vì lọc/tìm kiếm. Sau khi hoàn thành/mở lại công việc, nhóm hiển thị, số đếm, tiến độ Trang chủ và thống kê Hồ sơ cùng cập nhật từ một `TaskBoard`.

Ngày gốc của API được giữ nguyên. Ví dụ mẫu chứa 20/10/2026, 21/10/2026 và 25/10/2026; ở ngày khác ứng dụng hiển thị ngày thực thay vì ghi sai là “Hôm nay”.

## OOP

- `TaskItem`, `Subtask`, `TaskBoard`: collection, đối tượng được chọn, tìm kiếm/lọc và trạng thái hoàn thành.
- `TaskListState`, `TaskListBody`: phân biệt nội dung, tải, lỗi, trống và không có kết quả; tính nhóm và tổng việc chưa xong.
- `TaskApi`, `MockTaskApi`, các DTO: gọi/kiểm tra/ánh xạ API.
- `TaskSyncRepository`, `ApiFirebaseTaskRepository`, `FirebaseTaskStore`: nạp, cache, lưu và đồng bộ theo UID.
- `TodoViewModel`: điều hướng, chống thao tác trong lúc tải/lưu và chia sẻ dữ liệu với Trang chủ/Chi tiết/Hồ sơ.
- `TaskListScreen`: nhận đối tượng và callback, không tự tạo request hoặc truy cập Firebase.

## Kiểm tra

`TaskListStateTest` kiểm tra phân biệt trạng thái trống/lỗi/tìm kiếm và số đếm tổng không bị thay đổi bởi bộ lọc. Các test trong `TaskSyncUiTest` kiểm tra tải, API ID, tìm kiếm/lọc, thử lại, chờ lưu, lỗi quyền, quay lại từ chi tiết và cuộn danh sách 30 công việc.

`TaskListLiveUiTest` mở ứng dụng bằng phiên Google/Facebook hiện có, tải collection Firebase thật, tìm kiếm và mở chi tiết rồi lưu ảnh `task_list_api.png`. Test không sửa/xóa công việc hoặc đăng xuất. Các test tích hợp API/Firebase kiểm tra GET thật, ghi/đọc/xóa một bản ghi tạm của chính UID và chặn UID khác.

## Kết quả xác minh ngày 08/11/2026

- APK debug và APK test build thành công; **41 unit test đạt**, Lint **0 lỗi, 16 cảnh báo**.
- **24 test UI đạt**: 14 test đồng bộ Trang chủ/Danh sách, 9 test công việc/hồi quy và 1 test UI dùng phiên Google cùng Firebase thật.
- **3 test tích hợp thật đạt**: GET API bằng token đã cấp, ghi/đọc/xóa đúng một công việc tạm và chặn đọc UID khác. Không có test tích hợp bị bỏ qua.
- Danh sách thật hiển thị 4 việc chưa xong, 2 việc đã xong, đúng bộ dữ liệu API/collection hiện có. Test UI thật chỉ đọc công việc; test ghi Firebase đã dọn bản ghi tạm.
- Đối chiếu lại màn Danh sách bằng dữ liệu SVG: sai số RGB trung bình **2.384/255**, tương đồng RGB **99.07%**. Đây là phép so sánh pixel của bố cục dùng dữ liệu tham chiếu; ảnh API chứa nội dung/ngày thực khác bản SVG.
- Báo cáo đầy đủ trong `app/build/reports/task-list-flow.txt`; test tìm kiếm sau khi chỉnh assertion placeholder được chạy lại đạt trong `task-list-search-final.txt`. Tổng cộng 27 trường hợp Android đã đạt.

Ảnh dữ liệu Firebase thật: [task_list_api.png](screenshots/task_list_api.png). Đối chiếu bố cục SVG/Android: [tasks-comparison.png](tasks-comparison.png).

Mã nguồn, tài liệu, rules và test nằm trong dự án để nộp lên GitHub; thư mục `build/` và cấu hình token local bị Git bỏ qua.
