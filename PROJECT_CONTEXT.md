# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`1.0.4`** (mã bản dựng `versionCode = 10004`, tuân thủ chuẩn SemVer 2.0.0).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (v2.2.1 / commit `3fe129d`).
- **Cải tiến cốt lõi v1.0.4 (Tối ưu hóa toàn diện cho OriginOS 6, Sửa dứt điểm Download, Quick Share, Pin Tai nghe Thật & Logo Vivo mới)**:
  1. **Đồng bộ toàn diện xuống OriginOS 6**:
     - Gỡ bỏ hoàn toàn nhãn OriginOS 7, đổi tên thẻ Tai nghe Bluetooth chuẩn OriginOS 6.
     - Loại bỏ công tắc và tính năng Bóc tách ảnh chụp màn hình (`smartScreenshotOn` & `screenshotObserver`).
     - Xóa bỏ cơ chế Carousel và nút chuyển thẻ giả lập, trả về cơ chế xếp chồng đa nhiệm tự nhiên của OriginOS 6.
  2. **Sửa dứt điểm thẻ Tải xuống (`DownloadCard.kt`) và Quick Share (`QuickShareCard.kt`) theo chuẩn `GenericCard`**:
     - Giữ nguyên `smallIcon` gốc dạng `TYPE_RESOURCE`, gán `Icon.createWithResource` cho `largeIcon`.
     - Loại bỏ hoàn toàn `oi_card_ring` và `oi_left_icon` Bitmap để tránh xung đột với `TEMPLATE_RIGHT_ISLAND_PROGRESS (2)`.
     - Truyền `oi_left_content` chuẩn ("${pct}%" / "Chia sẻ" / "Tải..." / "Xong").
     - Mở rộng `QuickShareDetector.kt` nhận diện thêm các gói chia sẻ của Vivo/BBK/Bluetooth và channel Google Quick Share.
  3. **Nâng cấp thẻ Tai nghe Bluetooth (`EarbudsCard.kt`)**:
     - Bố cục mở rộng chia 2 dòng rõ ràng: Dòng 1 (Tên tai nghe + Pin Hộp sạc), Dòng 2 (Pin Tai Trái L + Pin Tai Phải R), không bị cắt chữ `...`.
     - Lấy **100% pin thật** qua 3 nguồn: Bóc tách thông báo hệ thống của Google Fast Pair / Vivo TWS, lắng nghe AT Command `+IPHONEACCEV` qua sự kiện HFP Vendor, và quét trực tiếp `BluetoothManager.getConnectedDevices(HEADSET)`.
  4. **Nâng cấp bộ thẻ mẫu thử nghiệm (`IslandSamples.kt`)**:
     - Khi bấm nút mẫu OTP, Download, Quick Share: Phát một Notification hệ thống thực tế lên thanh trạng thái để kiểm tra toàn bộ luồng xử lý tự động từ `NotificationCastListener` lên Đảo.
     - Khi bấm nút Tai nghe: Ưu tiên phát hiện và hiển thị đúng tên cùng pin thật của tai nghe đang kết nối với máy.
  5. **Tái thiết kế Logo ứng dụng chuẩn ngôn ngữ Vivo OriginOS 6**:
     - Nền gradient xanh Vivo Electric Blue (`#2555FF` -> `#0099FF`), thẻ squircle kính mờ Origin Glass và viên thuốc Đảo Nguyên Tử màu đen sâu `#0A0E17` có cảm biến xanh ngọc `#00E5FF`. Đồng bộ toàn bộ XML và PNG `mipmap-*`.

## 2. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\OriginIsle-VN-v1.0.4-debug.apk`

## 3. Bản Đồ Code Trọng Yếu
- `app/build.gradle.kts`: Cấu hình phiên bản (`versionCode = 10004`, `versionName = "1.0.4"`).
- `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`: Lõi điều phối SuperX OriginOS chuẩn gốc.
- `app/src/main/java/com/originisle/android/cards/OtpCard.kt`: Thẻ OTP SuperX capsule template 6, chạm là sao chép.
- `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`: Thẻ tiến độ tải xuống template 4 + vòng tiến độ template 2 quanh camera.
- `app/src/main/java/com/originisle/android/cards/QuickShareCard.kt`: Thẻ Quick Share chuẩn template 4 + template 2.
- `app/src/main/java/com/originisle/android/quickshare/QuickShareDetector.kt`: Bộ nhận diện Quick Share đa kênh và gói hệ thống.
- `app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`: Thẻ tai nghe Bluetooth hiển thị pin Trái/Phải/Hộp sạc thật.
- `app/src/main/java/com/originisle/android/cards/PaymentCard.kt`: Thẻ biến động số dư ngân hàng.
- `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`: Bộ đón bắt thông báo hệ thống và định tuyến thẻ.
- `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`: Màn hình Cài đặt chính OriginOS 6.
- `app/src/main/java/com/originisle/android/ui/samples/IslandSamples.kt`: Bộ thẻ mẫu phát thông báo thực tế để kiểm thử luồng thật.

## 4. Tình Trạng Hiện Tại
- Biên dịch thành công: `BUILD SUCCESSFUL in 1m 31s`.
- File APK: `OriginIsle-VN-v1.0.4-debug.apk` (~76.6 MB).
