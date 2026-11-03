# Kết quả kiểm tra

Ngày thực hiện: 26/10/2026.

## Build và phân tích tĩnh

- `:app:assembleDebug`: thành công.
- `:app:lintDebug`: **0 lỗi**, 7 cảnh báo. Các cảnh báo về phiên bản thư viện mới hơn và khóa màn hình dọc; không có lỗi tương thích API.
- `:app:testDebugUnitTest`: **5/5 test đạt**.
- `:app:connectedDebugAndroidTest`: **6/6 test đạt**, Gradle báo `BUILD SUCCESSFUL` trên Android 15 / API 35.
- Chạy trực tiếp `adb shell am instrument -w -r vn.edu.uth.todo.test/androidx.test.runner.AndroidJUnitRunner`: **6/6 test đạt** trên Android 15 / API 35.

Các test Android kiểm tra nút Tiếp tục, Bỏ qua, Bắt đầu ngay, dấu chấm, vuốt, Back và giữ trang qua Activity recreation. Bốn test chụp ảnh từng màn để kiểm tra hình thức.

## Đối chiếu hình ảnh

Ảnh Android được chụp ở 780 × 1688 px, mật độ 320 dpi, tương đương vùng thiết kế 390 × 844 dp. Riêng Start được đặt đúng tỷ lệ 400 × 860 trong vùng màn hình đó. Sau đó ảnh được chuẩn hóa về kích thước SVG để so sánh.

| Màn hình | Độ tương đồng RGB trung bình | Pixel có sai khác mỗi kênh ≤24/255 |
|---|---:|---:|
| Start | 99,80% | 99,15% |
| Onboarding 1 | 99,44% | 98,46% |
| Onboarding 2 | 99,37% | 98,36% |
| Onboarding 3 | 99,70% | 99,00% |

Độ tương đồng RGB tính bằng `100 × (1 − MAE/255)`. Đây là thước đo sai khác màu, không phải thang điểm cảm nhận thị giác. Đã kiểm tra trực quan ảnh đặt cạnh nhau: hình minh họa, logo, nội dung, màu, vị trí, ngắt dòng, dấu trang và nút khớp thiết kế; khác biệt nhỏ chủ yếu ở cách raster hóa chữ và bo góc giữa SVG và Android.

- `comparison.png`: bản SVG và Android đặt cạnh nhau cho đủ 4 màn.
- `screenshots/`: ảnh chụp Android và bản chuẩn hóa.
- `visual-comparison.json`: số liệu so sánh.
- `figma/`: SVG gốc và PNG tham chiếu.

Lần chạy emulator đầu tiên với đồ họa phần mềm bị timeout trước khi test khởi động. Chạy với GPU phần cứng đã giải quyết lỗi môi trường này. Các kết quả đạt ở trên thuộc lần chạy ổn định sau đó.
