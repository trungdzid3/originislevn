# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 và OriginOS 7 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`0.0.10`** (mã bản dựng `versionCode = 107`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (v2.2.1).
- **Cải tiến cốt lõi v0.0.10 (Pixel-perfect Switch Vivo, Khắc Phục OTP & Tiến Trình Tải Nền)**:
  1. **Nút Switch Chuẩn 1:1 Vật Lý Vivo OriginOS 6/7 (`IsleSwitch` trong `Components.kt`)**:
     - Track: Thanh trượt hình con nhộng capsule (48x24dp, bo góc `RoundedCornerShape(12.dp)`).
       - Khi ON: Màu xanh ngọc pastel `#A0D0CF`.
       - Khi OFF: Màu xám sáng `#E2E6E8` (Light) / `#2C2F36` (Dark).
     - Thumb: Hình tròn đường kính 32dp (lớn hơn track 24dp, nhô đều trên dưới mỗi bên 4dp).
     - ĐẶC BIỆT: Thumb dạng **Vòng Ring viền siêu dày (4.8dp)** với viền màu xanh ngọc đậm `#006D77` (khi ON) / `#8A9096` (khi OFF), và **ruột bên trong màu trắng tinh `#FFFFFF`**.
     - Hoạt họa chuyển động mượt mà với `tween(180ms)`.
  2. **Khắc Phục Thẻ OTP Bị Reject & Bóc Tách 100% Tin Nhắn FPT Long Châu (`OtpCard` & `OtpDetector`)**:
     - Đổi `oi_right_template` từ `TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT` (6) sang `TEMPLATE_RIGHT_ISLAND_TEXT_ICON` (4), `showRightIcon = false`, `oi_fg_color = "#00E5FF"`. Chấm dứt tình trạng Vivo SuperX daemon reject mã OTP dài hoặc có khoảng trắng (`SUPERX_VALUE = false`).
     - Nâng cấp `OtpDetector`: Tự động phát hiện qua Notification Action ("Sao chép mã", "Copy code") để trích xuất số OTP chuẩn xác 100%, bổ sung regex bắt gọn mẫu SMS Brandname FPT Long Châu (`Ma xac thuc OTP cua ban la 490603`), chặn triệt để hotline 1800/1900.
  3. **Khắc Phục Tiến Trình Tải Xuống Edge Canary, Chrome & CH Play (`DownloadCard` & `DownloadDetector`)**:
     - Xóa bỏ `oi_right_content` trong `DownloadCard` khi dùng `TEMPLATE_RIGHT_ISLAND_PROGRESS` (2) tránh mâu thuẫn bundle gây reject SuperX.
     - Bổ sung cơ chế Throttle chống spam notification rate-limit (chỉ notify khi hoàn tất, hoặc sau >= 500ms, hoặc tiến độ thay đổi >= 2%).
     - Chống tràn số nguyên 32-bit (`rawProgress.toLong() * 100 / rawMax`) khi tải tệp dung lượng lớn tính theo byte.
     - Hỗ trợ đầy đủ các biến thể Edge Canary (`com.microsoft.emmx.canary`), Chrome Canary/Beta/Dev và Google Play Store (`com.android.vending`).

## 2. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\Dao-Origin-0.0.10-VN.apk`

## 3. Bản Đồ Code Trọng Yếu
- `app/build.gradle.kts`: Cấu hình phiên bản (`versionCode = 107`, `versionName = "0.0.10"`).
- `app/src/main/java/com/originisle/android/ui/Components.kt`: Hàm `IsleSwitch` chuẩn 1:1 pixel-perfect Vivo OriginOS switch ring.
- `app/src/main/java/com/originisle/android/cards/OtpCard.kt`: Thẻ OTP SuperX template 4 màu Cyan neon không bị reject.
- `app/src/main/java/com/originisle/android/otp/OtpDetector.kt`: Bộ bóc tách OTP thông minh hỗ trợ Notification Copy Action & Brandname.
- `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`: Thẻ tiến độ tải xuống kèm throttle chống spam Android rate-limit.
- `app/src/main/java/com/originisle/android/download/DownloadDetector.kt`: Bộ phát hiện tiến trình tải cho Chrome, Edge Canary & Google Play.
- `app/src/main/java/com/originisle/android/cards/PaymentCard.kt`: Thẻ chuyển tiền ngân hàng hiển thị tự nhiên, không tràn layout.
- `app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`: Viễn trắc pin tai nghe Bluetooth chi tiết L, R và Case.
- `app/src/main/java/com/originisle/android/log/CastLog.kt`: Bộ quản lý nhật ký hoạt động persistent JSON trên đĩa.

## 4. Tình Trạng Hiện Tại
- Biên dịch thành công: `BUILD SUCCESSFUL in 25s`.
- File APK: `Dao-Origin-0.0.10-VN.apk` (76.90 MB) đã được đóng gói tại thư mục gốc.
- Git Remote: `https://github.com/trungdzid3/originislevn.git` (commit `345317b`, branch `main`).
