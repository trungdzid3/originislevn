# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.16` (mã bản dựng `versionCode = 10216`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.16 / 10216)
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
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, xóa ACTION_CANCEL trong post(), throttle 1s + >=2%, oi_show_notify = false
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms, loại bỏ internal intent gây nhảy Im lặng)
│       │       ├── download/         # DownloadDetector chặn triệt để 100% Gmail, tin nhắn, app chat (return false ngay khi in NON_DOWNLOAD_APPS)
│       │       ├── island/           # PlaygroundService (showNotify = false chỉ hiện trên Đảo, không hiện trên thanh thông báo), builder, constants
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP Bank-Style (STRONG_OTP_MARKER, quét MessagingStyle/publicVersion)
│       │       ├── quickshare/       # Nhận diện Quick Share (loại bỏ phán đoán 100% khi max<=0, mở rộng từ khóa nhận tệp)
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener (chặn lọt GenericCard, thống nhất ID 35009)
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
└── OriginIsle-VN-v1.2.16.apk         # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 16)
```

## 3. Bản Đồ Code (Code Map)
- `QuickShareCard.kt` (`app/src/main/java/com/originisle/android/cards/QuickShareCard.kt`): Hàm `post` (lines 140-230) xóa bỏ hoàn toàn `context.startService(ACTION_CANCEL, id = sbnId)` (triệt tiêu việc co rút đảo mỗi khi tăng %), siết chặt throttle `now - lastTime >= 1000L && abs(info.progress - lastProg) >= 2`, khóa cứng `oi_show_notify = false`.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`): Thêm `return` dứt điểm khi `isQuickShare(sbn) == true`, ngăn chặn notification rơi xuống `GenericCard.post` sinh ô thứ 2; đồng bộ ID `QuickShareCard.ID` (35009).
- `DownloadDetector.kt` (`app/src/main/java/com/originisle/android/download/DownloadDetector.kt`): Hàm `isDownload` (lines 138-160) trả về `false` ngay lập tức nếu `pkg in NON_DOWNLOAD_APPS` (chặn 100% Gmail, Outlook, Zalo, Telegram, Messenger bắt nhầm thành download).
- `DownloadCard.kt` (`app/src/main/java/com/originisle/android/cards/DownloadCard.kt`): Hàm `post` (lines 64-135) siết throttle 1s + >=2% và bổ sung `putExtra("oi_show_notify", false)`.
- `PlaygroundService.kt` (`app/src/main/java/com/originisle/android/island/PlaygroundService.kt`): Mặc định `showNotify = false` để thẻ SuperX chỉ hiển thị trên Đảo Origin, không tạo thêm thông báo shade trên Android.

## 4. Những Điểm Cần Chú Ý Kỹ Thuật (Gotchas)
- `applicationId = "com.autonavi.minimap"`: Tuyệt đối giữ nguyên để vượt whitelist SuperX trên Vivo OriginOS 6.
- `showNotify = false`: Bắt buộc để thông báo chỉ hiển thị trên Đảo, không tạo thêm thông báo shade trên Android.
- `QuickShare ID = 35009`: Định danh duy nhất, không dùng song song với `sbnId` để tránh OriginOS hiển thị chế độ 2 ô đa nhiệm (1 thanh + 1 ô nhỏ).
