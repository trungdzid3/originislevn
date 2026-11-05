# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 và OriginOS 7 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`0.0.8`** (mã bản dựng `versionCode = 105`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (v2.2.1).
- **Cải tiến cốt lõi v0.0.8 (Carousel & Deck Stacking, Viễn Trắc Tai Nghe Toàn Diện, Sửa Triệt Để OTP & Chuyển Tiền)**:
  1. **Cử chỉ duyệt đa tác vụ (Carousel & Deck Stacking)**:
     - Xây dựng kiến trúc `CardSession` và hàng đợi `activeDeck` trong `PlaygroundService.kt`.
     - Hỗ trợ luân chuyển mượt mà giữa các tác vụ đang chạy đồng thời (Tải xuống <-> Nhạc <-> OTP <-> Đếm giờ <-> Sạc pin).
     - Bổ sung nút action `[ ⟲ Đổi tác vụ (x/n) ]` và hiển thị chỉ số deck trên viên thuốc.
     - Khắc phục triệt để lỗi xung đột scene trên Vivo OriginOS: dọn dẹp sạch thẻ cũ trước khi post thẻ mới giúp loại bỏ 100% tình trạng Vivo reject thẻ (`SUPERX_VALUE = false`).
  2. **Thẻ Chuyển Tiền / Biến Động Số Dư (PaymentCard)**:
     - Bên trái viên thuốc: Hiển thị tên ngân hàng (Vietcombank, MB Bank, Techcombank,...).
     - Bên phải viên thuốc: Hiển thị số tiền chuyển khoản sạch sẽ, loại bỏ dấu `+` riêng biệt ở góc phải.
     - Thẻ to khi mở rộng: Giữ đầy đủ định dạng số tiền `+<tiền>` / `-<tiền>` cùng nội dung chi tiết giao dịch.
  3. **Mã OTP Không Dấu & Hiển Thị Người Gửi (OtpDetector & NotificationCastListener)**:
     - Nâng cấp bộ nhận diện OTP bắt trọn mọi biến thể tiếng Việt không dấu: "ma xac thuc", "ma xac nhan", "ma xac minh", "ma dang nhap", "ma bao mat", "otp cua ban la",... (chính xác với SMS nhà thuốc Long Châu, FPT,...).
     - Tinh chỉnh `PHONE_PREFIX` để không nuốt nhầm OTP khi tin nhắn chứa số hotline hỗ trợ (1800/1900).
     - Đặt ưu tiên xử lý OTP lên vị trí cao nhất và hiển thị tên Brandname/Sender thực tế trên thẻ.
  4. **Viễn Trắc Tai Nghe Bluetooth Chi Tiết L / R / Case (EarbudsCard)**:
     - Bóc tách toàn diện các trường viễn trắc từ intent extras Vivo TWS, Google Fast Pair và metadata AOSP.
     - Hiển thị đầy đủ Tai Trái (L), Tai Phải (R) và Hộp sạc (Case) kể cả khi chưa rút tai nghe ra khỏi hộp.
  5. **Tiến Trình Tải Xuống (DownloadCard)**:
     - Sử dụng Bitmap app icon và mipmap an toàn, loại bỏ hoàn toàn việc truyền XML Vector Drawable khiến SuperX bị reject trên OriginOS.
  6. **Nút Switch OriginOS 6 Chuẩn Xác 100% (IsleSwitch)**:
     - Kích thước chuẩn: Track capsule 50x22dp bo góc 11dp (Mint ON `#AFD3D2`, Slate OFF `#E2E5E8`), Thumb Ring 30dp nhô đều 4dp ruột trắng `#FFFFFF`, viền dày 3.6dp màu Teal `#00696E`.

## 2. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\Dao-Origin-0.0.8-VN.apk`

## 3. Bản Đồ Code Trọng Yếu
- `app/build.gradle.kts`: Cấu hình phiên bản (`versionCode = 105`, `versionName = "0.0.8"`).
- `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`: Động cơ Carousel & Deck Stacking quản lý đa tác vụ song song, giải phóng scene sạch.
- `app/src/main/java/com/originisle/android/cards/PaymentCard.kt`: Thẻ biến động số dư ngân hàng (trái: ngân hàng, phải: số tiền sạch).
- `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`: Thẻ tiến trình tải xuống an toàn với bitmap app icon.
- `app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`: Viễn trắc pin tai nghe Bluetooth chi tiết L, R và Case.
- `app/src/main/java/com/originisle/android/otp/OtpDetector.kt`: Bộ bóc tách mã OTP toàn diện hỗ trợ tiếng Việt có dấu và không dấu.
- `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`: Điều phối thông báo ưu tiên hàng đầu, gửi tên sender OTP thực tế.
- `app/src/main/java/com/originisle/android/ui/Components.kt`: Nút Switch OriginOS 6 chuẩn pixel theo ảnh thực tế.
- `app/src/main/java/com/originisle/android/ui/samples/IslandSamples.kt`: Bổ sung mẫu thử Carousel đa tác vụ & Tai nghe Vivo TWS 4.

## 4. Tình Trạng Hiện Tại
- Biên dịch thành công: `BUILD SUCCESSFUL`.
- File APK: `Dao-Origin-0.0.8-VN.apk` (76.8 MB) đã được đóng gói tại thư mục gốc.
- Git Remote: `https://github.com/trungdzid3/originislevn.git` (branch `main`).
