# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 và OriginOS 7 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`1.0.2`** (mã bản dựng `versionCode = 10002`, tuân thủ chuẩn SemVer 2.0.0).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (v2.2.1).
- **Cải tiến cốt lõi v1.0.2**:
  1. **Hỗ trợ toàn diện Quick Share / Chia sẻ lân cận (`QuickShareDetector.kt` & `QuickShareCard.kt`)**:
     - Nhận diện 4 nguồn: Google Play Services GMS (`com.google.android.gms`), Google Quick Share (`com.google.android.apps.sharing`), Vivo EasyShare (`com.vivo.easyshare`), Samsung Share (`com.samsung.android.app.share`).
     - Hỗ trợ cả 2 trạng thái:
       + **Yêu cầu nhận tệp**: Đảo hiển thị dạng `TEMPLATE_BUTTONS` (8) với 2 nút "Từ chối" và "Chấp nhận" tương tác trực tiếp.
       + **Tiến trình truyền tệp**: Đảo hiển thị vòng xoay tiến độ dạng `TEMPLATE_PROGRESS_VISUAL` (2) quanh camera kèm % tiến độ và tốc độ truyền.
     - Đặt cờ `oi_show_notify = false` để không tạo thông báo thừa trong khay hệ thống (Shade).
  2. **Khắc phục triệt để lỗi thẻ mẫu Demo (`IslandSamples.kt`)**:
     - Bổ sung `oi_show_notify = false` và `oi_island_show_time = 15` cho toàn bộ các hàm gọi thẻ mẫu thử nghiệm.
     - Sửa mẫu thử OTP sang `TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT` (template 6) để không bị Vivo reject.
     - Bổ sung 2 mẫu thử Quick Share (Yêu cầu nhận tệp & Đang nhận tệp).
  3. **Ngăn chặn thông báo rác OTP / Tải xuống (`OtpCard.kt` & `DownloadCard.kt`)**:
     - Đặt `oi_show_notify = false`: chặn OriginOS tạo thêm thông báo thường trong khay thông báo khi đảo hiển thị.
     - Sử dụng `TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT` (6) cho OTP và `TEMPLATE_RIGHT_ISLAND_PROGRESS` (2) cho tải xuống, kèm `oi_force_show = true`.

## 2. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\Dao-Origin-1.0.2-VN.apk`

## 3. Bản Đồ Code Trọng Yếu
- `app/build.gradle.kts`: Cấu hình phiên bản (`versionCode = 10002`, `versionName = "1.0.2"`).
- `app/src/main/java/com/originisle/android/quickshare/QuickShareDetector.kt`: Phát hiện thông báo chia sẻ Quick Share / Nearby Share.
- `app/src/main/java/com/originisle/android/cards/QuickShareCard.kt`: Hiển thị Quick Share trên Đảo (trạng thái yêu cầu nhận tệp & tiến trình).
- `app/src/main/java/com/originisle/android/cards/OtpCard.kt`: Thẻ OTP SuperX capsule template 6, chạm là copy, `oi_show_notify = false`.
- `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`: Thẻ tiến độ tải xuống kèm throttle chống spam Android rate-limit.
- `app/src/main/java/com/originisle/android/cards/PaymentCard.kt`: Thẻ chuyển tiền ngân hàng (bên trái tên ngân hàng, bên phải số tiền).
- `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`: Động cơ lắng nghe ưu tiên: Ngân hàng -> OTP -> Quick Share -> Tải xuống.
- `app/src/main/java/com/originisle/android/ui/samples/IslandSamples.kt`: Danh mục thẻ mẫu thử nghiệm chuẩn hóa SuperX không shade.

## 4. Tình Trạng Hiện Tại
- Biên dịch thành công: `BUILD SUCCESSFUL in 3m 43s`.
- Chữ ký số: `apksigner verify` đạt `v2 scheme: true`.
- File APK: `Dao-Origin-1.0.2-VN.apk` (65.8 MB, 69,037,354 bytes) sẵn sàng cài đặt.
