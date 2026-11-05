# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 và OriginOS 7 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`0.0.7`** (mã bản dựng `versionCode = 104`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (v2.2.1).
- **Cải tiến cốt lõi v0.0.7 (Nâng Cấp Tính Năng OriginOS 7 & Hoàn Thiện)**:
  1. **Tự động bóc tách thông tin từ ảnh chụp màn hình (AI Screenshot Recognition - OriginOS 7)**:
     - Tích hợp Google ML Kit OCR on-device chạy offline 100% không tốn mạng.
     - Tự động nhận diện khi chụp màn hình vé máy bay (Vietnam Airlines, Vietjet Air, Bamboo, Vietravel), vé tàu xe (Đường sắt VN, FUTA), vé sự kiện (CGV, Lotte) hoặc mã bưu kiện / lấy đồ ăn (GrabFood, ShopeeFood, SPX Express, GHTK, Viettel Post,...).
     - Ghim thông tin then chốt lên Đảo Origin, hỗ trợ sao chép mã nhanh và đồng bộ lịch trình vào Google Calendar / Lịch hệ thống chỉ với một chạm.
  2. **Viễn trắc pin tai nghe Bluetooth đa thành phần (Deep Accessory Telemetry - OriginOS 7)**:
     - Tự động đọc Metadata Bluetooth AOSP và broadcast mở rộng để trích xuất chi tiết mức pin của từng tai nghe (Tai Trái L, Tai Phải R) và Hộp sạc (Case), kèm trạng thái đang sạc ⚡.
     - Hiển thị viễn trắc đa thành phần sống động trực tiếp lên viên thuốc và thẻ mở rộng trên Đảo Origin.
  3. **Khắc phục triệt để tiến độ tải xuống (Download Progress)**:
     - Đồng bộ cấu hình 100% với mẫu chuẩn: dùng vector `R.drawable.ic_download`, bỏ can thiệp icon gói ngoài và màu gượng ép, đảm bảo thanh tiến trình tải tệp của Chrome, Edge Canary và Play Store luôn hiển thị mượt mà.
  4. **Nút Switch OriginOS 6 Chuẩn Pixel**:
     - Thân Track 48x16dp (`#BAD8D9`), Thumb tròn 28dp nhô đều 6dp ruột trắng `#FFFFFF`, viền dày 3.8dp màu Teal `#00696E`.
  5. **Tích hợp phông chữ hệ thống Vivo Sans**:
     - Tự động kế thừa font `vivo-sans` trên OriginOS.
  6. **Bắt mã OTP FPT Long Châu & Biến động số dư Ngân hàng Việt Nam**:
     - Bắt chuẩn xác mã OTP dạng SMS không bị chặn bởi hotline.

## 2. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm bắt buộc xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\Dao-Origin-0.0.7-VN.apk`
(và duy trì `Dao-Origin-0.0.6-VN.apk` đồng bộ).

## 3. Bản Đồ Code Trọng Yếu
- `app/build.gradle.kts`: Cấu hình phiên bản (`versionCode = 104`, `versionName = "0.0.7"`), dependency ML Kit Text Recognition.
- `app/src/main/java/com/originisle/android/smart/ScreenshotObserver.kt`: ContentObserver giám sát MediaStore phát hiện ảnh chụp màn hình mới.
- `app/src/main/java/com/originisle/android/smart/ScreenshotSmartExtractor.kt`: Bóc tách thông minh vé máy bay, vé tàu, mã nhận hàng bằng ML Kit OCR.
- `app/src/main/java/com/originisle/android/smart/ScreenshotCard.kt`: Thẻ thông minh hiển thị vé/mã đơn trên Đảo kèm nút sao chép và thêm vào Lịch.
- `app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`: Viễn trắc pin tai nghe Bluetooth chi tiết L, R và Case.
- `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`: Thẻ tiến trình tải xuống chuẩn SuperX template 2.
- `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`: Bóc tách thông báo SMS, điều phối OTP, Ngân hàng, Tải xuống và quản lý ScreenshotObserver.
- `app/src/main/java/com/originisle/android/ui/Components.kt`: Switch OriginOS 6 chuẩn pixel.
- `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`: Màn hình Cài đặt chuẩn OriginOS kèm toggle tính năng OriginOS 7.
- `app/src/main/java/com/originisle/android/ui/samples/IslandSamples.kt`: Bổ sung mẫu thử nghiệm Vé máy bay và Tai nghe TWS OriginOS 7.

## 4. Tình Trạng Hiện Tại
- Biên dịch thành công: `BUILD SUCCESSFUL`.
- File APK: `Dao-Origin-0.0.7-VN.apk` (76.7 MB kèm ML Kit OCR offline) đã được cập nhật tại thư mục gốc.
- Git Remote: `https://github.com/trungdzid3/originislevn.git` (branch `main`).
