# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 và OriginOS 7 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`0.0.9`** (mã bản dựng `versionCode = 106`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (v2.2.1).
- **Cải tiến cốt lõi v0.0.9 (Logo Mới, Khắc Phục Chuyển Tiền, Pin Hộp Sạc & Nhật Ký Persistent)**:
  1. **Khắc Phục Lỗi Thẻ Chuyển Tiền / Biến Động Số Dư (PaymentCard)**:
     - Chuyển sang `TEMPLATE_RIGHT_ISLAND_TEXT_ICON` (4) với `showRightIcon = false`, `leftContent = bankName` (Vietcombank, MB Bank, Techcombank,...), `rightContent = rawAmountClean` (số tiền chuyển khoản định dạng sạch).
     - Loại bỏ việc nhồi chuỗi dài vào `TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT` (6) làm tràn layout capsule SuperX của Vivo.
     - Trong `PlaygroundService.kt`: giữ nguyên `baseChip`, không tự ý ghép chỉ số deck làm biến dạng hoặc kéo dài chip trên đảo; chuyển nút chuyển tác vụ sang action button riêng biệt `[ ⟲ Đổi tác vụ (x/n) ]`.
  2. **Bóc Tách Pin Hộp Sạc Tai Nghe Bluetooth Chuẩn Xác (EarbudsCard)**:
     - Viết lại bộ giải mã `parseMetadataInt` & `parseMetadataBoolean` hỗ trợ cả byte nhị phân (`bytes[0] and 0xFF`) lẫn chuỗi ASCII từ `BluetoothDevice.getMetadata()`, khắc phục việc đọc byte 95 (0x5F) thành ký tự '_' khiến giá trị pin hộp sạc bị null.
     - Lắng nghe đầy đủ các intent action của Vivo (`vivo.intent.action.BLUETOOTH_BATTERY`, `com.vivo.upslide.BLUETOOTH_BATTERY`, `com.vivo.bluetooth.BATTERY_LEVEL_CHANGED`).
     - Khi mở rộng thẻ: Hiển thị đầy đủ Tai Trái, Tai Phải và Hộp sạc; khi tai nghe đang nằm trong hộp thì hiển thị `Tai nghe: Trong hộp   |   Hộp sạc: $case% ⚡`.
  3. **Nhật Ký Hoạt Động Persistent & Bộ Chẩn Đoán Hệ Thống (CastLog & ActivityScreen)**:
     - Nâng cấp `CastLog` lưu trữ persistent vào file JSON trên bộ nhớ máy (`cast_logs_persistent.json`), bảo toàn lịch sử thông báo kể cả khi app tắt hoặc khởi động lại.
     - Thêm Banner Đỏ cảnh báo nổi bật trên `ActivityScreen` và `HomeScreen` khi `NotificationCastListener` bị ngắt hoặc chưa cấp quyền, hướng dẫn trực tiếp mở "Cho phép cài đặt bị hạn chế" (Restricted Settings) trên Vivo OriginOS.
     - Thêm nút "Kiểm tra gửi thông báo thử nghiệm" ngay trong `ActivityScreen` cho phép người dùng test nhanh OTP và Biến động số dư để kiểm chứng listener.
  4. **Thiết Kế Logo Mới Chuẩn Phong Cách OriginOS 7 Atomic Dynamic Island**:
     - Thiết kế mới vector `ic_launcher_foreground.xml`: Viên thuốc Đảo Origin nổi bật với 3 tầng hào quang neon cyan/teal (`#00E5FF`, `#00BFA5`, `#00696E`), chấm viễn trắc thông minh, sóng lượng tử đa tác vụ.
     - Đồng bộ monochrome silhouette Android 13+ Themed Icons.
     - Nâng cấp nền launcher `ic_launcher_background` sang Deep Space Black (`#0B0E14`).

## 2. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\Dao-Origin-0.0.9-VN.apk`

## 3. Bản Đồ Code Trọng Yếu
- `app/build.gradle.kts`: Cấu hình phiên bản (`versionCode = 106`, `versionName = "0.0.9"`).
- `app/src/main/java/com/originisle/android/cards/PaymentCard.kt`: Thẻ chuyển tiền ngân hàng hiển thị tự nhiên, không tràn layout.
- `app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`: Viễn trắc pin tai nghe Bluetooth chi tiết L, R và Case hỗ trợ giải mã nhị phân AOSP/Vivo.
- `app/src/main/java/com/originisle/android/log/CastLog.kt`: Bộ quản lý nhật ký hoạt động persistent JSON trên đĩa.
- `app/src/main/java/com/originisle/android/ui/ActivityScreen.kt`: Màn hình Nhật ký hoạt động có bộ chẩn đoán listener & nút test thông báo.
- `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`: Động cơ Carousel & Deck Stacking và bảo toàn contract SuperX.
- `app/src/main/res/drawable/ic_launcher_foreground.xml`: Logo vector mới Đảo Origin OriginOS 7.

## 4. Tình Trạng Hiện Tại
- Biên dịch thành công: `BUILD SUCCESSFUL`.
- File APK: `Dao-Origin-0.0.9-VN.apk` (76.88 MB) đã được đóng gói tại thư mục gốc.
- Git Remote: `https://github.com/trungdzid3/originislevn.git` (branch `main`).
