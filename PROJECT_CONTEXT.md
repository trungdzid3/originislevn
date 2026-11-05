# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`0.0.6`** (mã bản dựng `versionCode = 103`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (v2.2.1).
- **Cải tiến cốt lõi v0.0.6**:
  1. **Nhận diện mã OTP triệt để**:
     - Khắc phục lỗi `OtpCard`: Thay thế hoàn toàn `R.drawable.ic_key` (Vector XML) bằng `R.mipmap.ic_launcher_round` (Bitmap mipmap), ngăn chặn việc hệ thống Vivo SuperX âm thầm từ chối/drop thông báo.
     - Cập nhật giải thuật ngữ cảnh `OtpDetector`: Sử dụng lookaround và context anchor `(?i)(?:\bOTP\b|\bma\s*xac\s*thuc\b|\bmã\s*xác\s*thực\b|\bverification\s*code\b|\bsecurity\s*code\b|\bcode\b)[^\d]{0,35}?(?<!\d)(\d{4,8})(?!\d)` để bắt chính xác mã OTP (ví dụ FPT Long Châu "Ma xac thuc OTP cua ban la 918119") và loại trừ số hotline/tổng đài (18006928).
     - Bổ sung các từ khóa tiền tố số điện thoại: `liên hệ`, `lien he`, `tổng đài`, `tong dai`, `cskh`.
  2. **Tiến trình tải xuống (Download Live Activity)**:
     - Nâng cấp `NotificationCastListener`: Đưa nhánh tải xuống (`DownloadDetector.isDownload`) lên hàng ưu tiên cao (Live Activity) ngay sau OTP, không còn bị chặn bởi bộ lọc thông báo tĩnh hoặc công tắc ứng dụng bị tắt.
     - Bổ sung tùy chọn `cast_downloads` mặc định kích hoạt (`true`).
     - Đặt mặc định `cast_notifications = true` trên toàn hệ thống để ứng dụng hoạt động ngay sau khi cấp quyền.
  3. **Giao diện chuẩn Vivo OriginOS 6 (dựa trên ảnh chụp thực tế)**:
     - Nền đen AMOLED thuần `#000000` (thay thế nền xám obsidian cũ).
     - Thẻ Atomic Card squircle `#17181C`, bề mặt nổi `#22242A`.
     - Màu chủ đạo & icon: 100% chuyển sang **Teal / Cyan Mint** (`#00BFA5`).
     - Bỏ khung vuông có màu xung quanh icon danh mục: Render glyph vector thuần túy màu Teal trên nền trong suốt.
     - Bổ sung thanh tìm kiếm con nhộng (Pill Search Bar 42dp) trên đỉnh màn hình chính.
     - Công tắc (Switch): Bật là Teal `#00BFA5`, tắt là `#2C2D35`, nút gạt trắng.

## 2. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm bắt buộc xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\Dao-Origin-0.0.6-VN.apk`

## 3. Kiến Trúc & Cấu Trúc Thư Mục Chuẩn Hóa
- `app/`: Toàn bộ module ứng dụng Android (Kotlin, Jetpack Compose, Android Views).
- `scripts/`:
  - `release.sh`: Kịch bản Bash xuất bản bản dựng release và tạo tag git.
  - `README.md`: Hướng dẫn chi tiết chức năng toàn bộ công cụ.
  - `tools/`: Bộ công cụ Python chuyên biệt phục vụ vá lỗi, đối soát chuỗi, bản địa hóa và đồng bộ style OriginOS.
- `docs/`: Tài liệu kiến trúc upstream (`DEV.md`, `PROTOCOL.md`, ảnh màn hình).
- `backups/`: Lưu trữ các bản snapshot sao lưu dự phòng theo Major Overhaul Backup Protocol.
- `DESIGN.md`: Nguồn chân lý duy nhất (SSOT) về giao diện, màu sắc, typography và animation.

## 4. Bản Đồ Code Trọng Yếu
- `app/build.gradle.kts`: Cấu hình phiên bản (`versionCode = 103`, `versionName = "0.0.6"`).
- `app/src/main/java/com/originisle/android/OriginIsleApp.kt`: Khởi tạo Application kèm ghi tệp chẩn đoán `last_crash.txt`.
- `app/src/main/java/com/originisle/android/MainActivity.kt`: Điều hướng Activity chính và giao diện chẩn đoán lỗi trực quan bằng Android Views thuần.
- `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`: Trích xuất tin nhắn SMS an toàn, điều phối Đảo và nhận diện OTP/Download/Bank theo thứ tự ưu tiên cao.
- `app/src/main/java/com/originisle/android/otp/OtpDetector.kt`: Phân tích mã OTP song ngữ, hỗ trợ Vivo Verification Reminder, lọc hotline và từ khóa ngữ cảnh.
- `app/src/main/java/com/originisle/android/cards/OtpCard.kt`: Sử dụng icon mipmap bitmap chuẩn SuperX.
- `app/src/main/java/com/originisle/android/download/DownloadDetector.kt`: Nhận diện tải xuống cho Chrome, Edge mọi nhánh, CH Play, Download Manager.
- `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`: Thẻ hiển thị tiến trình tải xuống chuẩn OriginOS SuperX.
- `app/src/main/java/com/originisle/android/ui/Theme.kt`: Bảng màu chuẩn OriginOS 6 (AMOLED `#000000`, Atomic Card `#17181C`, Teal `#00BFA5`).
- `app/src/main/java/com/originisle/android/ui/Components.kt`: IconTile vector thuần túy không nền màu, IsleSwitch chuẩn Teal.
- `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`: Màn hình chính phong cách OriginOS 6 kèm ô tìm kiếm con nhộng và công tắc Tiến trình tải xuống.

## 5. Tình Trạng Hiện Tại
- Biên dịch thành công với OpenCode CLI & Gradle: `BUILD SUCCESSFUL`.
- Tệp APK `Dao-Origin-0.0.6-VN.apk` (31.6 MB) đã sẵn sàng tại thư mục nguồn.
