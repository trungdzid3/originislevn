# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.3` (mã bản dựng `versionCode = 10203`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
- **Quy chuẩn đánh số phiên bản**:
  - `X` (Số đầu): Thế hệ hiện tại.
  - `Y` (Số tiếp theo): Nâng cấp tính năng mới trong cùng thế hệ.
  - `Z` (Số tiếp nữa): Bản vá lỗi (bug fixes, hotfix).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đồng bộ hoàn toàn lên `v2.2.3` / commit `73abb82`).
- **Kho lưu trữ GitHub**: `https://github.com/trungdzid3/originislevn.git` (`auto_github_sync: true`).

## 2. Cây Cấu Trúc Thư Mục Chuẩn
```text
origin-isle-vn/
├── .agent/
│   ├── GEMINI.md                     # Quy tắc toàn cục Antigravity (UTF-8 chuẩn)
│   └── rules.md                      # Rule cục bộ (auto_github_sync: true)
├── app/
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.3 / 10203)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── res/
│       │   │   ├── drawable/         # ic_quick_share.xml, ic_smartband.xml, ic_headphones.xml...
│       │   │   └── xml/keepalive_accessibility_config.xml # Quét node UI pin Wearable
│       │   └── java/com/originisle/android/
│       │       ├── bank/             # Bóc tách biến động số dư Ngân hàng VN & SMS (lọc spam/promo 0đ)
│       │       ├── cards/            # 15 loại thẻ giao diện Đảo Origin
│       │       │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (Scene: NAVIGATION, oi_show_notify=false)
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Wearable (findBluetoothDevice, GATT API 33+, bondedDevices batteryOf)
│       │       │   ├── MediaCard.kt      # Nhạc sóng động & cơ chế nhường Đảo gốc OriginOS 6 (Zero-Overlap)
│       │       │   ├── OtpCard.kt        # Thẻ OTP tối ưu (trái "OTP", phải "$code · Chép", chạm chép clipboard)
│       │       │   ├── QuickShareCard.kt # Quick Share logo chính thức từ file SVG tải về
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chế độ Chuông / Rung / Im lặng / DND độc lập (showNotify=true)
│       │       ├── download/         # Nhận diện tải xuống nhạy bén
│       │       ├── island/           # Service SuperX, builder, constants, resolveAllowedScene
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP MessagingStyle (EXTRA_MESSAGES), Action Title, regex (OTP)
│       │       ├── quickshare/       # Nhận diện Quick Share & EasyShare & actions
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener & Receivers & KeepAliveAccessibilityService
│       │       ├── smart/            # Bóc tách thông tin màn hình
│       │       ├── sports/           # Tỷ số thể thao trực tiếp
│       │       └── ui/               # Giao diện Compose đại tu chuẩn Vivo OriginOS 6 (5 nhóm, lồng nhau)
│       └── test/                     # Unit Tests
├── backups/                          # Thư mục sao lưu an toàn khi đại tu
├── docs/
├── gradle/                           # Gradle wrapper & version catalogs
├── DESIGN.md                         # Nguồn chân lý thiết kế giao diện (SSOT)
├── PROJECT_CONTEXT.md                # Lưu trữ ngữ cảnh tinh gọn (Lean Context)
├── task.md                           # Phân quyền công việc & Locked Files
├── origin-isle-vn.code-workspace     # Cấu hình workspace
└── OriginIsle-VN-v1.2.3.apk          # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 3)
```

## 3. Bản Đồ Code (Code Map)
- `OtpDetector.kt` (`app/src/main/java/com/originisle/android/otp/OtpDetector.kt`):
  - `extractMessagingTexts`: Bóc tách text từ `NotificationCompat.EXTRA_MESSAGES` và `android.messages` (cấu trúc MessagingStyle chuẩn Android 14 Vivo MMS / Google Messages).
  - Trích xuất trực tiếp mã OTP từ Notification Action titles ("Sao chép 490603", "490603").
  - Nâng cấp regex bắt `(OTP)`, brand name ở giữa ("Mã xác minh Google của bạn là 490603").
- `OtpCard.kt` (`app/src/main/java/com/originisle/android/cards/OtpCard.kt`):
  - Trái hiển thị "OTP", phải hiển thị "$code · Chép". Chạm vào sao chép ngay lập tức vào clipboard và thu gọn đảo.
- `EarbudsCard.kt` (`app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`):
  - `findBluetoothDevice`: Tự động tìm `BluetoothDevice` từ `bondedDevices` khi notification kết nối không truyền device object.
  - `batteryOf(targetDevice)`: Đọc trực tiếp mức pin từ Bluetooth stack phần cứng (`BluetoothDevice.getBatteryLevel()`).
  - `readGattBatteryAsync`: Override phương thức 4 tham số chuẩn Android 13+ API 33 (`value: ByteArray`).
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`):
  - Quét sâu đa trường notification của Samsung Galaxy Wearable, đồng bộ pin ngay khi nhận tín hiệu kết nối.

## 4. Việc Đang Mở
- Không còn việc mở. Bản vá lỗi v1.2.3 đã hoàn thành và sẵn sàng sử dụng.
