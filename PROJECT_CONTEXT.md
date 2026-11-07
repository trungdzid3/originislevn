# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.18` (mã bản dựng `versionCode = 10218`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.18 / 10218)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── res/
│       │   │   ├── drawable/         # ic_quick_share.xml, ic_smartband.xml, ic_headphones.xml...
│       │   │   └── xml/keepalive_accessibility_config.xml # Quét node UI pin Wearable & bắt TYPE_NOTIFICATION_STATE_CHANGED cho OTP
│       │   └── java/com/originisle/android/
│       │       ├── bank/             # Bóc tách biến động số dư Ngân hàng VN & SMS (lọc spam/promo 0đ, chặn hạn mức quảng cáo)
│       │       ├── cards/            # 15 loại thẻ giao diện Đảo Origin
│       │       │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (TEMPLATE_BASE + vòng tiến độ #2B66FF, throttle 1s, oi_show_notify = false)
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Wearable (hiển thị "Đã ngắt" tối giản không khung xám)
│       │       │   ├── MediaCard.kt      # Chuẩn OriginOS 6: Viên nang (bìa squircle trái + sóng âm thuần phải), Thẻ mở rộng (bìa squircle có App Badge tròn góc dưới phải + 3 nút trắng nền tối)
│       │       │   ├── OtpCard.kt        # Thẻ OTP chuẩn PaymentCard (TEMPLATE_BASE + TEMPLATE_RIGHT_ISLAND_TEXT_ICON, không truyền actions để không bị từ chối SuperX, postFromAccessibility)
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, xử lý dứt điểm kẹt 99% (isFinishedEffective khi >= 99% & watchdog 2.5s cho 90..99%), throttle 1s + >=2%, oi_show_notify = false
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms, loại bỏ internal intent gây nhảy Im lặng)
│       │       ├── download/         # DownloadDetector chặn triệt để 100% Quick Share, Google Play Services & mọi ứng dụng nghe nhạc (Zing MP3, Spotify, NCT...)
│       │       ├── island/           # PlaygroundService (showNotify = false), OriginIslandBuilder (chuẩn hóa TEMPLATE_RIGHT_ISLAND_WAVE = 1 không đè text/icon)
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP Bank-Style (ưu tiên tin nhắn mới nhất asReversed() trong MessagingStyle)
│       │       ├── quickshare/       # Nhận diện Quick Share (lọc bỏ notification foreground service chạy ngầm vô nghĩa qua isServiceForegroundNotification)
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener (cô lập sớm trình phát nhạc trước DownloadDetector) & KeepAliveAccessibilityService (bắt OTP vượt Redaction Android 15)
│       │       ├── smart/            # Bóc tách thông tin màn hình
│       │       ├── sports/           # Tỷ số thể thao trực tiếp
│       │       └── ui/               # Giao diện Compose đại tu chuẩn Vivo OriginOS 6 (5 nhóm, lồng nhau)
│       └── test/                     # Unit Tests (16 bank tests, OtpDetectorTest, QuietHoursTest)
├── backups/                          # Thư mục sao lưu an toàn khi đại tu
├── docs/
├── gradle/                           # Gradle wrapper & version catalogs
├── DESIGN.md                         # Nguồn chân lý thiết kế giao diện (SSOT)
├── PROJECT_CONTEXT.md                # Lưu trữ ngữ cảnh tinh gọn (Lean Context)
├── task.md                           # Phân quyền công việc & Locked Files
├── origin-isle-vn.code-workspace     # Cấu hình workspace
└── OriginIsle-VN-v1.2.18.apk         # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 18)
```

## 3. Bản Đồ Code (Code Map)
- `OtpCard.kt` (`app/src/main/java/com/originisle/android/cards/OtpCard.kt`): Hàm `post` & `postFromAccessibility` (lines 25-115) dùng `TEMPLATE_BASE` + `TEMPLATE_RIGHT_ISLAND_TEXT_ICON`, bỏ hoàn toàn `actions` để không bị Vivo SystemUI từ chối SuperX (`cleanUpRejectedSuperX`), chống trùng lặp 8 giây.
- `OtpDetector.kt` (`app/src/main/java/com/originisle/android/otp/OtpDetector.kt`): Hàm `extractNotificationText` (lines 118-175) duyệt `style.messages.asReversed()` lên đầu danh sách để luôn lấy mã OTP ở tin nhắn mới nhất.
- `KeepAliveAccessibilityService.kt` (`app/src/main/java/com/originisle/android/service/KeepAliveAccessibilityService.kt`): Hàm `onAccessibilityEvent` & `handleNotificationOtpEvent` (lines 55-145) bắt `TYPE_NOTIFICATION_STATE_CHANGED`, bóc tách OTP trực tiếp để vượt qua Sensitive Notification Redaction trên Android 15 / OriginOS 6.
- `DownloadDetector.kt` (`app/src/main/java/com/originisle/android/download/DownloadDetector.kt`): Hàm `isMusicOrMediaApp` & `isDownload` (lines 90-275) khóa cứng loại trừ `com.zing.mp3` và mọi ứng dụng nhạc/MediaStyle ngay đầu hàm, đồng thời siết chặt điều kiện `max > 0`.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`): Cô lập luồng thông báo nhạc trước `DownloadDetector` trong `onNotificationPosted` (lines 815-865) và `pollRunnable` (lines 585-620).
- `MediaCard.kt` (`app/src/main/java/com/originisle/android/cards/MediaCard.kt`) & `OriginIslandBuilder.kt` (`app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt`): Tách biệt `pillLeftIcon` (squircle thuần) + sóng nhạc thuần (`oi_show_right_icon = false`, `oi_right_content = ""`) trên viên nang thu gọn; vẽ App Badge tròn ở góc dưới phải của ảnh bìa album (`createBadgedAlbumArt`) trên thẻ mở rộng.

## 4. Những Điểm Cần Chú Ý Kỹ Thuật (Gotchas)
- `applicationId = "com.autonavi.minimap"`: Tuyệt đối giữ nguyên để vượt whitelist SuperX trên Vivo OriginOS 6.
- `showNotify = false`: Bắt buộc để thông báo chỉ hiển thị trên Đảo, không tạo thêm thông báo shade trên Android.
- Không gắn `actions` có resource icon của `minimap` kèm `source_pkg` của ứng dụng khác vào `TEMPLATE_BASE` (`OtpCard`, `PaymentCard`) vì Vivo SystemUI sẽ từ chối gói SuperX (`SUPERX_VALUE = false`).
