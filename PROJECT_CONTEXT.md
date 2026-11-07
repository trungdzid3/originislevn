# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.17` (mã bản dựng `versionCode = 10217`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.17 / 10217)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── res/
│       │   │   ├── drawable/         # ic_quick_share.xml, ic_smartband.xml, ic_headphones.xml...
│       │   │   └── xml/keepalive_accessibility_config.xml # Quét node UI pin Wearable
│       │   └── java/com/originisle/android/
│       │       ├── bank/             # Bóc tách biến động số dư Ngân hàng VN & SMS (lọc spam/promo 0đ, chặn hạn mức quảng cáo)
│       │       ├── cards/            # 15 loại thẻ giao diện Đảo Origin
│       │       │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (TEMPLATE_BASE + vòng tiến độ #2B66FF, throttle 1s, oi_show_notify = false)
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Wearable (hiển thị "Đã ngắt" tối giản không khung xám)
│       │       │   ├── MediaCard.kt      # Sóng âm HSV rực rỡ, PAG wave cờ, fallback icon nốt nhạc chống trống, bo góc squircle bìa album
│       │       │   ├── OtpCard.kt        # Thẻ OTP chuẩn Ngân hàng (trái "OTP", phải mã OTP màu #00D084, chạm chép clipboard)
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, xử lý dứt điểm kẹt 99% (isFinishedEffective khi >= 99% & watchdog 2.5s cho 90..99%), throttle 1s + >=2%, oi_show_notify = false
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms, loại bỏ internal intent gây nhảy Im lặng)
│       │       ├── download/         # DownloadDetector chặn triệt để 100% Quick Share & Google Play Services (com.google.android.gms)
│       │       ├── island/           # PlaygroundService (showNotify = false chỉ hiện trên Đảo, không hiện trên thanh thông báo), builder, constants
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP Bank-Style (STRONG_OTP_MARKER, quét MessagingStyle/publicVersion)
│       │       ├── quickshare/       # Nhận diện Quick Share (lọc bỏ notification foreground service chạy ngầm vô nghĩa qua isServiceForegroundNotification)
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener (ưu tiên Quick Share trước Download trong pollRunnable, dọn sạch cả 2 ID trong onNotificationRemoved)
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
└── OriginIsle-VN-v1.2.17.apk         # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 17)
```

## 3. Bản Đồ Code (Code Map)
- `DownloadDetector.kt` (`app/src/main/java/com/originisle/android/download/DownloadDetector.kt`): Hàm `isDownload` (lines 140-165) chặn tuyệt đối `QuickShareDetector.isQuickShare(sbn)` và package `com.google.android.gms`, triệt tiêu hoàn toàn việc sinh thẻ download "Trung's PC (48%)".
- `QuickShareDetector.kt` (`app/src/main/java/com/originisle/android/quickshare/QuickShareDetector.kt`): Thêm `isServiceForegroundNotification` (lines 48-75) lọc bỏ thông báo chạy ngầm không tiến độ, không để sinh thẻ phụ "Quick Share · Đang nhận tệp" gây 2 tác vụ trên đảo.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`): Đảo thứ tự ưu tiên Quick Share trước Download trong `pollRunnable` (lines 575-605); trong `onNotificationRemoved` (lines 1500-1535) gửi cancel và xóa cache cho cả `id` và `QuickShareCard.ID`.
- `QuickShareCard.kt` (`app/src/main/java/com/originisle/android/cards/QuickShareCard.kt`): Kích hoạt `isFinishedEffective` khi `progress >= 99%` (lines 153-155), thêm Auto-Finish Watchdog 2.5s khi tiến độ đạt 90..99% (lines 330-348) tự động gọi `postFinished()` bung thẻ 100% và ẩn sau 3.5s.

## 4. Những Điểm Cần Chú Ý Kỹ Thuật (Gotchas)
- `applicationId = "com.autonavi.minimap"`: Tuyệt đối giữ nguyên để vượt whitelist SuperX trên Vivo OriginOS 6.
- `showNotify = false`: Bắt buộc để thông báo chỉ hiển thị trên Đảo, không tạo thêm thông báo shade trên Android.
- `QuickShare ID = 35009`: Định danh duy nhất, không dùng song song với `sbnId` để tránh OriginOS hiển thị chế độ 2 ô đa nhiệm (1 thanh + 1 ô nhỏ).
