# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 và OriginOS 7 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`1.0.0`** (mã bản dựng `versionCode = 10000`, tuân thủ chuẩn SemVer 2.0.0).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (v2.2.1).
- **Cải tiến cốt lõi v1.0.0 (Bố cục Ngân hàng Chuẩn, Ưu tiên Số 1 Biến động số dư & Nút Test Đảo Trực Tiếp)**:
  1. **Khôi Phục & Tinh Chỉnh Đúng Bố Cục Thẻ Ngân Hàng (`PaymentCard.kt`)**:
     - Phục hồi nền tảng code ổn định từ bản 2 lượt trước (`de69b20`), áp dụng đúng bố cục người dùng yêu cầu:
       - **Bên trái viên nang (leftContent)**: Chỉ hiển thị tên Ngân hàng (`transaction.bankName`, ví dụ `Vietcombank`, `MB Bank`).
       - **Bên phải viên nang (rightContent)**: Hiển thị số tiền giao dịch (`title`, ví dụ `+2.500.000 đ` hoặc `-50.000 đ`).
       - **Bỏ dấu `+` riêng lẻ bên phải**: Dùng `TEMPLATE_RIGHT_ISLAND_TEXT_ICON` (template 4) với `showRightIcon = false`, `fgColor = accentColor` (xanh ngọc `#00D084` khi tiền vào, đỏ `#FF5247` khi tiền ra).
  2. **Đưa Biến Động Số Dư Ngân Hàng Lên Ưu Tiên Số 1 (`NotificationCastListener.kt`)**:
     - Đảo thứ tự: `VietnamBankDetector.parse(sbn)` chạy ĐẦU TIÊN để tin nhắn biến động số dư SMS/App không bao giờ bị bộ lọc OTP nuốt chửng bởi các chuỗi số tài khoản hay mã giao dịch.
     - `OtpDetector.find(notificationText(sbn), actionTitles)` chạy thứ hai, nhận diện chính xác các action "Sao chép mã" / "Copy code" của SMS OTP.
     - `DownloadDetector.isDownload(sbn)` chạy thứ ba, hỗ trợ cả thông báo tải xuống im lặng (Silent) từ Edge Canary, Chrome và CH Play.
  3. **Bộ Nút Test Trực Tiếp Đảo Origin (`ActivityScreen.kt`)**:
     - Màn hình Nhật ký hoạt động bổ sung 3 nút test phát thẳng lên đảo:
       - **Thử Số dư**: Bắn ngay thẻ Vietcombank +2.500.000 đ lên Đảo Origin.
       - **Thử OTP**: Bắn ngay thẻ OTP FPT Long Châu (682941) lên Đảo Origin.
       - **Thử Tải về**: Bắn ngay thanh tiến độ tải xuống Edge Canary (65%) lên Đảo Origin.

## 2. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\Dao-Origin-1.0.0-VN.apk`

## 3. Bản Đồ Code Trọng Yếu
- `app/build.gradle.kts`: Cấu hình phiên bản (`versionCode = 10000`, `versionName = "1.0.0"`).
- `app/src/main/java/com/originisle/android/cards/PaymentCard.kt`: Thẻ chuyển tiền ngân hàng (bên trái tên ngân hàng, bên phải số tiền, không capsule +).
- `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`: Động cơ lắng nghe ưu tiên Ngân hàng -> OTP -> Tải xuống.
- `app/src/main/java/com/originisle/android/ui/ActivityScreen.kt`: Màn hình Nhật ký hoạt động với 3 nút test đảo trực tiếp.
- `app/src/main/java/com/originisle/android/ui/Components.kt`: Hàm `IsleSwitch` chuẩn 1:1 pixel-perfect Vivo OriginOS switch ring.
- `app/src/main/java/com/originisle/android/cards/OtpCard.kt`: Thẻ OTP SuperX template 4 màu Cyan neon.
- `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`: Thẻ tiến độ tải xuống kèm throttle chống spam Android rate-limit.

## 4. Tình Trạng Hiện Tại
- Biên dịch thành công: `BUILD SUCCESSFUL in 24s`.
- File APK: `Dao-Origin-1.0.0-VN.apk` (76.90 MB) đã được đóng gói tại thư mục gốc.
- Git Remote: `https://github.com/trungdzid3/originislevn.git` (commit `cdc5cd0`, branch `main`).
