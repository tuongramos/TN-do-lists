# Đăng nhập, trang chủ, hồ sơ — 02/11/2026

**Cập nhật xác thực:** Google đã được tích hợp với Credential Manager; Facebook dùng Meta SDK. Cả hai xác thực token qua Firebase Authentication. Xem `GOOGLE-SIGN-IN.md` và `FACEBOOK-SIGN-IN.md` để cấu hình và thử tài khoản thật. Các phần mô tả `UnconfiguredAuthRepository` và chưa thêm SDK bên dưới ghi lại giai đoạn dựng giao diện ban đầu.

## Phạm vi

Giữ nguyên Start và ba onboarding. Bổ sung ba màn theo SVG: `auth/login.svg`, `auth/home.svg`, `auth/profile.svg`. Tất cả chữ, số liệu, thông tin người dùng, công việc, nhóm, thống kê và dữ liệu biểu đồ đi qua các đối tượng OOP từ repository.

Phiên bản này chưa thêm Firebase SDK, Google Services, Facebook SDK, API key, OAuth client hoặc `google-services.json`. Không có lệnh gọi xác thực ra mạng.

## Thông số thiết kế

Các Section rộng 396 và cao 879 px bao gồm nhãn ngoài màn điện thoại. Vùng ứng dụng là **x=3, y=31, rộng=390, cao=844**. Khi chuyển sang Compose, tọa độ phần tử được trừ đúng (3,31). Ứng dụng dùng font Be Vietnam Pro có sẵn, cùng màu, kích thước, độ đậm, tracking và baseline của SVG. Icon và logo được trích từ path gốc ở mật độ 3x.

### Đăng nhập

- Logo: 88 × 88, tại (28,189), bo 26, có bóng theo SVG.
- Tiêu đề: 30 px, đậm 700, dòng 38, tracking −0,4; baseline đầu tại y=335,025.
- Mô tả: 16/24 px, baseline đầu y=408,88.
- Hai dòng tính năng: 14 px, baseline y=481,145 và 509,145.
- **Bổ sung Facebook theo yêu cầu:** Google ở (28,528), Facebook ở (28,598), cả hai 334 × 58, bo 16, viền 1. SVG gốc chỉ có Google tại y=598; Google được dời lên 70 px để giữ nguyên các phần còn lại.
- Dấu phân cách y=677,5; dùng thử baseline y=729,88; điều khoản baseline y=780,91 và 798,91.

### Trang chủ

- Nền #F5F5F7; avatar 48 × 48 ở (20,61).
- Giữ ngày **Thứ Ba, 20 tháng 10** và lời chào **Chào UTH!** ở chế độ dùng thử để khớp SVG.
- Tiến độ 350 × 121 ở (20,130), bo 22, hiển thị **3/7 việc**, **43%**. Dữ liệu `DailyProgress` đóng gói tổng, số hoàn thành, tỷ lệ và nhãn.
- Nhóm Học tập/Cuộc sống: 169 × 123 ở y=271, hiển thị **4** và **3 việc còn lại** theo đúng SVG.
- Bốn công việc hiển thị tại y=464,540,616,692; mỗi thẻ 350 × 66; giữ nguyên tiêu đề, trạng thái, nhóm và giờ trong file.
- Thanh dưới ở y=758, cao 86; nút thêm tròn 56 ở (167,745).

### Hồ sơ

- Header baseline y≈83; avatar 88 × 88 tại (151,114).
- Bản tham chiếu: **Minh Anh**, **minhanh@gmail.com**, liên kết Google.
- Thống kê **128**, **12 ngày**, **86%**; ba thẻ 110 × 75 tại y=319.
- Biểu đồ 350 × 161 tại (20,410), **18/22 việc**. Dữ liệu bảy ngày là **3,4,2,4,3,1,1**, T3 được chọn. Chiều cao mỗi đơn vị là 18 px.
- Hai hàng đồng bộ/quyền riêng tư dùng khối 350 × 104 tại (20,587).
- Đăng xuất 350 × 52 tại (20,756).
- Phiên dùng thử có nhãn **Chế độ dùng thử**, không tuyên bố đã liên kết tài khoản Google/Facebook thật.

## Luồng hiện tại

1. Start → onboarding → Bắt đầu ngay → đăng nhập.
2. Nhấn Google hoặc Facebook → `UnconfiguredAuthRepository` trả `AuthResult.Failure` → hộp thoại lỗi; màn đăng nhập vẫn dùng được.
3. Nhấn Dùng thử → thông báo dữ liệu mẫu → trang chủ.
4. Nhấn avatar hoặc Cài đặt → hồ sơ; Back về trang chủ.
5. Đăng xuất → xóa phiên dùng thử → màn đăng nhập.

Nhánh thành công đã được chuẩn bị: `AuthResult.Success(user)` → thông báo thành công → trang chủ → hồ sơ từ dữ liệu `user`. `AuthResult.Cancelled` giữ nguyên màn đăng nhập; exception hiển thị lỗi có thể thử lại.

## Nối Firebase ở bước sau

- Thực hiện cấu hình dự án Firebase và nhà cung cấp Google/Facebook khi người dùng yêu cầu.
- Cài đặt `AuthRepository` bằng adapter Firebase, ánh xạ kết quả tài khoản sang `UserProfile`. `signOut()` cần đăng xuất cả Firebase và phiên provider nếu có.
- Tiêm adapter thay `UnconfiguredAuthRepository` khi khởi tạo `TodoViewModel`. Giữ hoạt động cấp credential của Google/Facebook ở lớp Activity/UI, không giữ Activity trong ViewModel.
- Khôi phục người dùng từ trạng thái Firebase để vào Home sau khi tiến trình được khởi tạo lại. Phần này được chủ động để lại cùng giai đoạn Authentication thật.
- `TodoRepository` hiện cung cấp dữ liệu mẫu theo Figma; chưa có Firestore/Realtime Database.

Các test giao diện dùng fake repository để xác nhận hợp đồng thành công/hủy. Fake chỉ nằm trong `androidTest`, không nằm trong APK ứng dụng.

## Kết quả kiểm tra

- Build debug thành công.
- Android Lint: **0 lỗi**.
- **12 unit test đạt**: 5 test điều hướng bài trước và 7 test hợp đồng Auth/dữ liệu mới.
- **12 test Android đạt**: 6 test bài trước và 6 test đăng nhập/trang chủ/hồ sơ, gồm nhánh thành công với fake Facebook, hủy, lỗi Google/Facebook và dùng thử/đăng xuất.
- Ảnh chụp Android API 35, độ phân giải 780 × 1688 ở 320 dpi, chuẩn hóa về 390 × 844.

| Màn | Tương đồng RGB trung bình | Pixel sai khác tối đa mỗi kênh ≤24/255 |
|---|---:|---:|
| Đăng nhập (đã bổ sung Facebook) | 98,83% | 96,46% |
| Trang chủ | 99,15% | 97,08% |
| Hồ sơ Google theo SVG | 99,46% | 98,22% |

Tương đồng RGB = `100 × (1 − MAE/255)`, là chỉ số sai khác màu sau chuẩn hóa, không phải thang điểm cảm nhận. Đã đối chiếu trực quan các ảnh đặt cạnh nhau. `auth-comparison.png` là ảnh đối chiếu; `auth-preview.png` là 3 màn Android thực tế. Nhãn tài khoản Facebook tự rộng hơn để vừa tên provider; bản Google giữ nguyên chiều rộng 208 px của SVG.
