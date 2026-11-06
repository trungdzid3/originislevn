# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.1.0` (mã bản dựng `versionCode = 10100`, chuẩn SemVer 2.0.0).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đồng bộ hoàn toàn lên `v2.2.3` / commit `73abb82`).
- **Kho lưu trữ GitHub**: `https://github.com/trungdzid3/originislevn.git` (`auto_github_sync: true`).

## 2. Cây Cấu Trúc Thư Mục Chuẩn
```text
origin-isle-vn/
├── .agent/
│   ├── GEMINI.md                     # Quy tắc toàn cục Antigravity (UTF-8 chuẩn)
│   └── rules.md                      # Rule cục bộ (auto_github_sync: true)
├── app/
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.1.0 / 10100)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/java/com/originisle/android/
│       │   ├── bank/                 # Bóc tách biến động số dư Ngân hàng VN & SMS (lọc spam/promo 0đ)
│       │   ├── cards/                # 15 loại thẻ giao diện Đảo Origin
│       │   │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │   │   ├── DownloadCard.kt   # Tiến độ tải xuống (Scene: NAVIGATION, oi_show_notify=false)
│       │   │   ├── EarbudsCard.kt    # Tai nghe & Galaxy Fit 3 (wearable_battery_cache, báo % pin, sạc đầy 100%, nền tối #FF1C1C1E)
│       │   │   ├── MediaCard.kt      # Nhạc sóng động (Scene: NAVIGATION, tự thu gọn sau 3s)
│       │   │   ├── OtpCard.kt        # Thẻ OTP ưu tiên cao (is_ongoing=true, nút sao chép)
│       │   │   ├── QuickShareCard.kt # Quick Share logo chính thức Canvas 192x192
│       │   │   └── RingerModeCard.kt # 4 chế độ Chuông / Rung / Im lặng / DND độc lập + polling 1s
│       │   ├── download/             # Nhận diện tải xuống nhạy bén (loại trừ Maps/Media/Wearable/System)
│       │   ├── island/               # Service SuperX, builder, constants, resolveAllowedScene
│       │   ├── log/                  # Nhật ký hoạt động bền vững (CastLog)
│       │   ├── otp/                  # Nhận diện mã xác thực OTP tiếng Việt (quét toàn bộ match, phân tách \n)
│       │   ├── quickshare/           # Nhận diện Quick Share & EasyShare & actions
│       │   ├── ride/                 # Nhận diện chuyến đi gọi xe & giao đồ ăn VN
│       │   ├── service/              # NotificationCastListener & Receivers
│       │   ├── smart/                # Bóc tách thông tin màn hình
│       │   ├── sports/               # Tỷ số thể thao trực tiếp
│       │   └── ui/                   # Giao diện Compose chuẩn OriginOS 6
│       └── test/                     # Unit Tests
├── backups/                          # Thư mục sao lưu an toàn khi đại tu
├── docs/
│   ├── DEV.md                        # Hướng dẫn build, ký APK và AMap spoof
│   ├── PROTOCOL.md                   # Đặc tả kỹ thuật SuperX của OriginOS
│   ├── ECOSYSTEM_REFERENCE.md        # Tổng hợp hệ sinh thái Đảo Origin & HyperIsland
│   ├── architecture/
│   │   └── ARCHITECTURE.md           # Sơ đồ kiến trúc & luồng dữ liệu (Mermaid)
│   └── screenshots/
├── gradle/                           # Gradle wrapper & version catalogs
├── scripts/
│   ├── README.md
│   ├── generate_origin_icons.py
│   └── release.sh
├── DESIGN.md                         # Nguồn chân lý thiết kế giao diện (SSOT)
├── PROJECT_CONTEXT.md                # Lưu trữ ngữ cảnh tinh gọn (Lean Context)
├── task.md                           # Phân quyền công việc & Locked Files
├── origin-isle-vn.code-workspace     # Cấu hình workspace
└── OriginIsle-VN-v1.1.0.apk          # File APK xuất bản chuẩn chuyên nghiệp
```

## 3. Bản Đồ Code (Code Map)
- `PlaygroundService.kt` (`app/src/main/java/com/originisle/android/island/PlaygroundService.kt`):
  - `resolveAllowedScene` (L190-204): Kiểm tra quyền `getSceneStatus` trên Vivo SystemUI, tự động fallback về `"NAVIGATION"` để thẻ không bao giờ bị từ chối.
  - `readAndPost` (L206-402): Quản lý `sceneCurrentId` và đóng gói SuperX bundle.
  - `cancelNotification` (L459-477): Dọn dẹp thẻ và scene tương ứng sạch sẽ.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`):
  - `pollRunnable` (L301-337): Định kỳ mỗi 1 giây làm mới Media, Chronometer, Tỷ số thể thao và gọi `RingerModeCard.poll(applicationContext)`.
  - `onNotificationPosted` (L464-812): Thứ tự ưu tiên nghiêm ngặt: Ngân hàng -> Quick Share -> Mã OTP -> Tải xuống -> Galaxy Fit 3 / Wearable (kèm quét `activeNotifications` và `wearable_battery_cache`) -> Media -> Ride -> Dẫn đường.
  - `onInterruptionFilterChanged` (L406-411): Gọi trực tiếp `RingerModeCard.onInterruptionFilterChanged`.
- `Cards` (`app/src/main/java/com/originisle/android/cards/`):
  - `EarbudsCard.kt`: Lưu đệm pin bền vững `wearable_battery_cache` (`getCachedBattery`, `saveCachedBattery`), chặn pop-up kết nối ảo, hiển thị thẳng `% pin` lên viên con nhộng và vòng tròn tiến độ camera khi kết nối, báo sạc đầy 100%, và dùng nền kính mờ tối `#FF1C1C1E` chữ trắng với viền đỏ khi ngắt kết nối.
  - `RingerModeCard.kt`: Tách biệt 3 trạng thái `lastRingerMode`, `lastDndState`, `lastVibrateSetting` (đọc `vibrate_when_ringing`, `vibrate_in_silent`, `vivo_vibrate_state`) để DND không bao giờ nuốt sự kiện Chuông/Rung/Im lặng; chuẩn hóa payload SuperX đồng bộ `ChargingCard.kt` và ghi nhận vào `CastLog`.
  - `DownloadCard.kt` & `DownloadDetector.kt`: Sử dụng scene `"NAVIGATION"` hợp lệ 100%, loại trừ các gói Bản đồ/Media/Wearable/Hệ thống ở đầu hàm rồi bắt nhạy bén mọi thông báo có `max > 0 || indeterminate` hoặc `CATEGORY_PROGRESS`.
  - `VietnamBankDetector.kt`: Lọc triệt để tin nhắn rác / khuyến mãi / gói cước viễn thông `+0đ`.
  - `QuickShareCard.kt` & `IconCache.kt`: `createQuickShareOfficialIcon` vẽ logo Quick Share chính thức.

## 4. Lưu Ý Kỹ Thuật (Gotchas)
- **ApplicationId & Scene**: Bắt buộc giữ `applicationId = "com.autonavi.minimap"` và ưu tiên `oi_scene = "NAVIGATION"` (scene duy nhất được Vivo cấp quyền mặc định cho gói giả lập AMap).
- **Tài nguyên Small Icon**: Mọi thông báo gửi tới Vivo SuperX phải sử dụng mipmap resource (`R.mipmap.ic_launcher_round`).
- **Quy định xuất bản APK**: Luôn lưu APK thành phẩm trực tiếp tại thư mục gốc dự án theo chuẩn SemVer không hậu tố debug: `OriginIsle-VN-v1.1.0.apk`.

## 5. Việc Đang Mở (Lean Context)
- Toàn bộ các hạng mục của bản phát hành chuyên nghiệp **v1.1.0** đã hoàn tất, vượt qua kiểm thử và đóng gói thành công.
