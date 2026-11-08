# Danh sách và chi tiết công việc

Nguồn người dùng gửi ngày 07/11/2026: `Section.svg` và `Background.svg`, được lưu nguyên bản ở `figma/tasks/list.svg` và `figma/tasks/detail.svg`.

## Kích thước và tọa độ

- Danh sách: vùng điện thoại **390 × 844**, bỏ nhãn Section và bóng ngoài (`x=3, y=31` trong SVG).
- Chi tiết: **390 × 941**. Co giãn đồng đều theo chiều rộng và cuộn dọc trên thiết bị thấp hơn để giữ đúng cỡ chữ/tỷ lệ.
- Font Be Vietnam Pro đi kèm ứng dụng; nền `#F5F5F7`, màu chính `#2F5BEA`.
- 9 icon mới được trích từ đường vector gốc ở 3x. Nút thêm và các icon có sẵn được tái sử dụng.

| Thành phần | Tọa độ/kích thước trên khung gốc |
|---|---|
| Tiêu đề danh sách | x=20, baseline=87.79, 28px/700, tracking −0.4 |
| Ô tìm kiếm | (20,111), 350×44, bo góc 12 |
| Bộ lọc | y=171; rộng 77/90/108, cao 36, bo góc 18 |
| Thẻ cần làm đầu tiên | (20,253), 350×66, bo góc 16; cách nhau 10 |
| Thẻ đã xong đầu tiên | (20,593), 350×50 |
| Thanh điều hướng | y=758, cao 86; Danh sách được chọn |
| Tiêu đề chi tiết | x=20, baseline=169.555, 26px/700, line-height 34, tracking −0.3 |
| Thông tin hạn/nhắc/lặp | (20,227), 350×152, bo góc 16 |
| Ghi chú | (20,419), 350×94, bo góc 16 |
| Việc nhỏ mặc định | (20,571), 350×211; 3 việc, 2 việc đã xong |
| Tiến độ việc nhỏ | (20,553), 350×6; ban đầu 67% = 234.5px |
| Nút hoàn thành | (15,825), 350.66×56, bo góc 16 |

Các tọa độ chi tiết đúng mẫu khi mở công việc “Nộp bài tập Lập trình di động”. Với công việc khác, nội dung, loại công việc, hạn và việc nhỏ lấy từ đúng đối tượng được chọn. Thêm/xóa việc nhỏ làm chiều cao nội dung và vị trí nút cuối thay đổi theo số dòng.

## Cách sử dụng

1. Đăng nhập hoặc dùng thử, mở tab **Danh sách** hoặc **Xem tất cả** trên Trang chủ.
2. Tìm công việc, chọn Tất cả/Học tập/Cuộc sống. Tìm kiếm hỗ trợ chữ không dấu.
3. Nhấn vòng tròn để đổi trạng thái; nhấn phần còn lại của thẻ để mở chi tiết.
4. Trên chi tiết, vòng chọn việc nhỏ cập nhật số lượng và thanh tiến độ. Ô nhập/nút Thêm tạo việc nhỏ; icon bút và thùng rác sửa/xóa việc nhỏ.
5. **Sửa** chỉnh tên/ghi chú trong hộp thoại. **Đánh dấu hoàn thành** chuyển trạng thái công việc. **Xóa công việc** cần xác nhận và quay về danh sách.
6. Nút Danh sách/Back quay về danh sách; Back từ danh sách trở về Trang chủ. Trang chủ mở được chi tiết từng công việc và nhận thay đổi trạng thái/xóa cùng bộ dữ liệu.

Từ ngày 08/11/2026, công việc lấy từ API và lưu riêng từng tài khoản bằng Firebase Realtime Database. Thao tác danh sách/chi tiết dùng cùng dữ liệu với thống kê Trang chủ/Hồ sơ. Chế độ dùng thử giữ thay đổi trong cache cục bộ. Xem cấu hình và luồng dữ liệu tại [API-REALTIME-DATABASE.md](API-REALTIME-DATABASE.md). Các con số/ảnh đối chiếu phía dưới là kết quả của dữ liệu SVG ngày 07/11, trước khi nối API.

## OOP

- `TaskRepository` cung cấp `TaskContent`; `FigmaTaskRepository` chứa nội dung, nhãn và 6 đối tượng mẫu.
- `TaskItem`, `Subtask`, `TaskPriority` đóng gói dữ liệu mỗi công việc.
- `TaskBoard` đóng gói bộ lọc, tìm kiếm không dấu và các nhóm cần làm/đã xong.
- `TodoViewModel` điều phối thay đổi, lưu trạng thái và chuyển `AppScreen.TaskList` / `AppScreen.TaskDetail(taskId)`.
- `TaskListScreen` và `TaskDetailScreen` nhận đối tượng nội dung/callback; không gọi Firebase hoặc tự tạo thông tin công việc khác với dòng được chọn.

## Đối chiếu

- `figma/tasks/reference_list.png`, `reference_detail.png`: bản tham chiếu render từ SVG.
- `screenshots/task_list.png`, `task_detail.png`: ảnh Android ở đúng tỷ lệ từng bản thiết kế.
- `tasks-comparison.png`: SVG và Android đặt cạnh nhau.
- `tasks-preview.png`: xem nhanh hai màn Android.
- `tasks-visual-comparison.json`: sai số RGB; đây là số liệu pixel, không phải bảo đảm cảm nhận giống nhau.

Tái xuất bằng `node tools/export-task-assets.cjs`; sau khi chụp hai màn và lấy ảnh về `docs/screenshots`, chạy `python tools/compare-task-screens.py`.

## Kết quả kiểm tra ngày 07/11/2026

- Build APK và APK test thành công; **25 unit test đạt**, lint **0 lỗi, 12 cảnh báo**.
- **9 test công việc đạt** trên Android 15: mở đúng từng công việc từ danh sách/Trang chủ, lọc và tìm kiếm, hoàn thành/mở lại, thêm/sửa/xóa việc nhỏ, sửa/xóa công việc, khôi phục SavedState/Activity và chụp hai màn.
- **14 test hồi quy Google/Facebook đạt**, dùng fake repository và fake kết quả nhà cung cấp.
- Màn danh sách được kiểm tra ở tỷ lệ 390×844; màn chi tiết được chụp đủ 390×941 và kiểm tra cuộn trên khung thấp hơn.
- Cả 40 tài nguyên ảnh/icon/font của ứng dụng có tham chiếu sử dụng.

| Màn | Sai số RGB trung bình (0–255) | Tương đồng theo RGB | Pixel có mọi kênh lệch ≤24 |
|---|---|---|---|
| Danh sách (đối chiếu lại 08/11) | 2.384 | 99.07% | 97.22% |
| Chi tiết (đối chiếu lại 08/11) | 2.243 | 99.12% | 97.33% |

Sai khác nhỏ còn lại chủ yếu ở cách làm mịn chữ/vector giữa bộ render SVG và Android. Ảnh đặt cạnh nhau nằm trong `tasks-comparison.png`; APK nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

Flow Danh sách/API đã được kiểm tra bổ sung ngày 08/11/2026; cách dùng, các trạng thái tải/lỗi/trống và kết quả trên tài khoản thật nằm trong [TASK-LIST-FLOW.md](TASK-LIST-FLOW.md).

Flow Chi tiết/API GET/PATCH/DELETE, quy tắc quay lại **Danh sách** và xử lý ghi/xóa có xác nhận được mô tả tại [TASK-DETAIL-FLOW.md](TASK-DETAIL-FLOW.md).
