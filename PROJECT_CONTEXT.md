# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.0` (mã bản dựng `versionCode = 10200`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.0 / 10200)
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
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Galaxy Fit 3 (Đa kênh Broadcast, ContentProvider, Cache)
│       │       │   ├── MediaCard.kt      # Nhạc sóng động & cơ chế nhường Đảo gốc OriginOS 6 (Zero-Overlap)
│       │       │   ├── OtpCard.kt        # Thẻ OTP ưu tiên cao (is_ongoing=true, nút sao chép mã)
│       │       │   ├── QuickShareCard.kt # Quick Share logo chính thức từ file SVG tải về
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chế độ Chuông / Rung / Im lặng / DND độc lập (showNotify=true)
│       │       ├── download/         # Nhận diện tải xuống nhạy bén
│       │       ├── island/           # Service SuperX, builder, constants, resolveAllowedScene
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP tiếng Việt có dấu & không dấu (SMS Brandname)
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
└── OriginIsle-VN-v1.2.0.apk          # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 0)
```

## 3. Bản Đồ Code (Code Map)
- `MediaCard.kt` (`app/src/main/java/com/originisle/android/cards/MediaCard.kt`):
  - `isOriginNativePlayerActive` (L50-95): Nhận diện Đảo gốc `com.vivo.musicwidgetmix` hoặc các app whitelisted gốc (`Spotify`, `Apple Music`, `Zing MP3`, `Kugou`...). Khi phát hiện Đảo gốc đang chạy, chủ động thu hồi thẻ Origin Isle VN để không bao giờ bị đè 2 bản phát nhạc.
- `RingerModeCard.kt` (`app/src/main/java/com/originisle/android/cards/RingerModeCard.kt`):
  - `show` (L380-410): Đặt mặc định `showNotify = true`, loại bỏ cờ `false` bị Vivo từ chối SuperX.
  - `start` (L300-340): Đăng ký `ContentObserver` cho `mode_ringer` và `vibrate_in_silent` bắt trọn vẹn thao tác đổi chế độ âm thanh trên Vivo Control Center.
- `OtpDetector.kt` & `OtpCard.kt` (`app/src/main/java/com/originisle/android/otp/`):
  - `OTP_HINT` & `OTP_REGEX`: Bổ sung toàn bộ từ khóa tiếng Việt không dấu (`ma xac thuc`, `ma xac nhan`, `ma giao dich`...), đọc thêm `tickerText` và `publicVersion`.
  - `OtpCard.show`: Đặt `oi_fg_color = Color.WHITE`, `oi_bg_color = Teal`, thêm nút sao chép mã.
- `EarbudsCard.kt` & `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/cards/`):
  - `wearable_conn_state`: Lưu bền vững vào `SharedPreferences`. Bỏ qua thông báo nền `isOngoing` cũ (`> 10s`) khi vuốt YouTube Music xuống đa nhiệm, chống bung thẻ kết nối ảo.
  - `querySamsungFit3Battery`: Kênh Broadcast Widget `getwearablebatterystatus.LEVEL` + CapsuleProvider + Quét UI qua `KeepAliveAccessibilityService`.
- `VietnamRideDetector.kt` & `RideCard.kt` (`app/src/main/java/com/originisle/android/ride/`):
  - Nhận diện đầy đủ Grab, Be, Xanh SM, ShopeeFood, Gojek, Ahamove (Food, Bike, Car, Express) với 5 mốc chặng tiến trình (20% -> 45% -> 75% -> 90% -> 100%), hiển thị `TEMPLATE_PROGRESS_VISUAL`.
- `Theme.kt`, `Components.kt`, `HomeScreen.kt` (`app/src/main/java/com/originisle/android/ui/`):
  - Phông chữ hệ thống Vivo đa trọng số tôn trọng `fontScale`.
  - Component `NestedSettingContainer` mở mượt các tùy chọn con lồng nhau.
  - Tái cấu trúc 5 nhóm tính năng chuẩn Vivo Settings, bổ sung công tắc Chuông/Rung/Im lặng và Đèn pin.

## 4. Lưu Ý Kỹ Thuật (Gotchas)
- **ApplicationId & Scene**: Bắt buộc giữ `applicationId = "com.autonavi.minimap"` và ưu tiên `oi_scene = "NAVIGATION"`.
- **Tài nguyên Small Icon**: Mọi thông báo gửi tới Vivo SuperX phải sử dụng mipmap resource (`R.mipmap.ic_launcher_round`).
- **Quy định xuất bản APK**: Luôn lưu APK thành phẩm trực tiếp tại thư mục gốc dự án theo chuẩn `OriginIsle-VN-v<ThếHệ>.<NângCấp>.<VáLỗi>.apk`.

## 5. Việc Đang Mở (Lean Context)
- Không có việc tồn đọng. Bản dựng v1.2.0 hoàn tất toàn diện 5 nhóm yêu cầu.
