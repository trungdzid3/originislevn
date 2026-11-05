# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`0.0.6`** (mã bản dựng `versionCode = 103`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (v2.2.1).
- **Cải tiến cốt lõi v0.0.6 (Đợt Đại tu Hoàn thiện)**:
  1. **Nhận diện mã OTP triệt để**:
     - Mở rộng `NotificationCastListener.kt`: `notificationText()` đọc cả `EXTRA_TEXT_LINES` (InboxStyle), `actions` (nút sao chép mã) và các trường riêng của Vivo MMS. Bắt 100% tin nhắn OTP dạng FPT Long Châu / Vivo Verification Reminder.
     - Lọc bỏ triệt để số điện thoại tổng đài hotline (18006928, 1900...) không bao giờ bị nhầm thành mã OTP.
  2. **Biến động số dư Ngân hàng Việt Nam**:
     - `VietnamBankDetector.kt`: Bổ sung `SMS_PACKAGES` (`com.vivo.mms`, `com.android.mms`, `com.google.android.apps.messaging`) và brandname các ngân hàng Việt Nam (Vietcombank, MB, Techcombank, BIDV, VPBank, ACB, TPBank, MoMo,...).
     - Hỗ trợ cả thông báo từ app ngân hàng và tin nhắn SMS biến động số dư.
     - Loại bỏ time-guard gây chặn nhầm các giao dịch liên tiếp.
  3. **Tiến trình tải xuống & Chống rơi thẻ SuperX**:
     - `DownloadCard.kt`: Đặt `oi_left_content` ngắn gọn chuẩn "Đang tải", ngăn vỡ layout viên thuốc.
     - `PlaygroundService.kt`: Tự động dọn dẹp kết thúc thẻ cũ trong scene `NAVIGATION` trước khi hiển thị thẻ mới, loại bỏ hoàn toàn việc SuperX âm thầm nuốt thẻ.
  4. **Giao diện chuẩn OriginOS 6 (Sáng & Tối)**:
     - Thẻ Phone Mockup kép ("Chế độ sáng" và "Chế độ tối") với radio button chuyển đổi tức thì theme ở đầu màn hình cài đặt giống 100% ảnh chụp thực tế.
     - Công tắc gạt OriginOS Teal `#00BFA5`, icon glyph vector thanh mảnh không hộp nền vuông.
  5. **Triệt tiêu bẫy màn hình đen khi khởi động**:
     - `MainActivity.kt` & `OriginIsleApp.kt`: Xóa bỏ việc kẹt cứng vào màn hình đen chẩn đoán lỗi, người dùng không bao giờ phải xóa dữ liệu ứng dụng.

## 2. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm bắt buộc xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\Dao-Origin-0.0.6-VN.apk`

## 3. Bản Đồ Code Trọng Yếu
- `app/build.gradle.kts`: Cấu hình phiên bản (`versionCode = 103`, `versionName = "0.0.6"`).
- `app/src/main/java/com/originisle/android/OriginIsleApp.kt`: Khởi tạo Application an toàn không bẫy treo app.
- `app/src/main/java/com/originisle/android/MainActivity.kt`: Điều hướng Activity chính, nạp Theme và phục hồi nhanh.
- `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`: Bóc tách thông báo SMS (InboxStyle, actions), điều phối OTP, Ngân hàng, Tải xuống lên Đảo.
- `app/src/main/java/com/originisle/android/bank/VietnamBankDetector.kt`: Nhận diện biến động số dư ngân hàng Việt Nam từ app và SMS.
- `app/src/main/java/com/originisle/android/otp/OtpDetector.kt`: Phân tích mã OTP song ngữ, hỗ trợ Vivo Verification Reminder, lọc hotline.
- `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`: Thẻ tiến trình tải xuống tối ưu layout và icon SuperX.
- `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`: Quản lý hiển thị SuperX, tránh xung đột cùng scene.
- `app/src/main/java/com/originisle/android/ui/Theme.kt`: Hệ thống màu Sáng (#F2F4F7) và Tối (#000000), màu Teal (#00BFA5).
- `app/src/main/java/com/originisle/android/ui/Components.kt`: Switch Teal, IconTile vector không viền nền, Squircle card.
- `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`: Màn hình Cài đặt chuẩn OriginOS 6 kèm 2 Phone Mockup Sáng/Tối.

## 4. Tình Trạng Hiện Tại
- Biên dịch thành công: `BUILD SUCCESSFUL`.
- File APK: `Dao-Origin-0.0.6-VN.apk` (32.2 MB) đã được cập nhật tại thư mục gốc.
- Git commit: `de69b20` an toàn tại branch `main`.
