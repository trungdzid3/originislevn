# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 và OriginOS 7 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`1.0.3`** (mã bản dựng `versionCode = 10003`, tuân thủ chuẩn SemVer 2.0.0).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (v2.2.1 / commit `3fe129d`).
- **Cải tiến cốt lõi v1.0.3 (Khôi phục chuẩn Lõi bản 0.0.1, OTP và Tải xuống bung Đảo 100%, Quick Share đầy đủ)**:
  1. **Khôi phục hoàn toàn Lõi `PlaygroundService.kt` theo chuẩn gốc `3fe129d`**:
     - Gỡ bỏ hàng đợi `activeDeck` và lệnh đóng `endOrigin` tức thì đối với các thông báo thực tế (nguyên nhân khiến Vivo đóng sập Đảo mới vừa gửi và đẩy rớt xuống khay thông báo).
     - Chỉ thực hiện `endOrigin` khi chuyển đổi giữa các thẻ mẫu thử nghiệm (`id >= 40000`).
     - Giữ `showNotify = true` mặc định cùng `displays = STATUSBAR | AOD` (chỉ hiển thị Đảo quanh camera, không kích hoạt bit thông báo khay).
  2. **Chuẩn hóa thẻ OTP (`OtpCard.kt`)**:
     - Khung cơ bản `TEMPLATE_BASE` (4) kết hợp viên nang `TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT` (6).
     - Đặt `oi_left_content = ""` (để trống chữ bên trái để icon app hiển thị gọn gàng, tránh tràn giới hạn ký tự capsule khiến Vivo reject).
     - Chạm vào Đảo tự động sao chép mã OTP vào Clipboard và đóng thẻ.
  3. **Khôi phục thẻ Tải xuống (`DownloadCard.kt`) về chuẩn bản 0.0.1**:
     - Chuyển từ `TEMPLATE_PROGRESS_VISUAL` (2) sang **`TEMPLATE_BASE` (4)** + **`TEMPLATE_RIGHT_ISLAND_PROGRESS` (2)** với vòng tròn phần trăm `oi_card_ring` và icon launcher mipmap an toàn (100% hiển thị trên OriginOS).
  4. **Tích hợp Quick Share / Chia sẻ lân cận (`QuickShareCard.kt`)**:
     - Hỗ trợ cả 2 trạng thái: Yêu cầu nhận tệp (nút Từ chối / Chấp nhận) và Tiến trình truyền tệp (vòng xoay tiến độ quanh camera kèm % và tốc độ MB/s).
  5. **Sửa toàn diện các nút Thử nghiệm Demo (`IslandSamples.kt` & `ActivityScreen.kt`)**:
     - Chuẩn hóa toàn bộ thẻ mẫu theo đúng thông số kỹ thuật SuperX của Vivo, bấm là bung Đảo ngay lập tức.
     - Giữ nguyên vẹn bố cục thẻ Ngân hàng `PaymentCard.kt` (bên trái tên ngân hàng, bên phải số tiền).

## 2. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\Dao-Origin-1.0.3-VN.apk`

## 3. Bản Đồ Code Trọng Yếu
- `app/build.gradle.kts`: Cấu hình phiên bản (`versionCode = 10003`, `versionName = "1.0.3"`).
- `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`: Lõi điều phối SuperX OriginOS chuẩn gốc 0.0.1.
- `app/src/main/java/com/originisle/android/cards/OtpCard.kt`: Thẻ OTP SuperX capsule template 6, chạm là copy, `left_content = ""`.
- `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`: Thẻ tiến độ tải xuống template 4 + vòng tiến độ template 2 quanh camera.
- `app/src/main/java/com/originisle/android/cards/QuickShareCard.kt`: Thẻ Quick Share (yêu cầu nhận tệp & tiến trình truyền).
- `app/src/main/java/com/originisle/android/cards/PaymentCard.kt`: Thẻ biến động số dư ngân hàng (bên trái tên ngân hàng, bên phải số tiền).
- `app/src/main/java/com/originisle/android/ui/samples/IslandSamples.kt`: Bộ thẻ mẫu thử nghiệm chuẩn hóa SuperX.
- `app/src/main/java/com/originisle/android/ui/ActivityScreen.kt`: Màn hình Nhật ký hoạt động với các nút thử nghiệm trực tiếp.

## 4. Tình Trạng Hiện Tại
- Biên dịch thành công: `BUILD SUCCESSFUL in 1m 37s`.
- Chữ ký số: `apksigner verify` đạt `v2 scheme: true`.
- File APK: `Dao-Origin-1.0.3-VN.apk` (65.8 MB, 69,037,354 bytes).
