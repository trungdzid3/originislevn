# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.19` (mã bản dựng `versionCode = 10219`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.19 / 10219)
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
│       │       │   ├── MediaCard.kt      # Cố định ID = 35010, chống nháy qua bộ đệm trạng thái & chữ ký ảnh bìa, 3 nút chuẩn Vivo thích ứng Light/Dark Mode
│       │       │   ├── OtpCard.kt        # Thẻ OTP chuẩn PaymentCard (TEMPLATE_BASE + TEMPLATE_RIGHT_ISLAND_TEXT_ICON, không truyền actions để không bị từ chối SuperX, postFromAccessibility)
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, xử lý dứt điểm kẹt 99% (isFinishedEffective khi >= 99% & watchdog 2.5s cho 90..99%), throttle 1s + >=2%, oi_show_notify = false
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms, loại bỏ internal intent gây nhảy Im lặng)
│       │       ├── download/         # DownloadDetector chặn triệt để 100% Quick Share, Google Play Services & mọi ứng dụng nghe nhạc (Zing MP3, Spotify, NCT...)
│       │       ├── island/           # PlaygroundService (showNotify = false, hủy triệt để ID cũ khi đổi trong cùng scene), OriginIslandBuilder (TEMPLATE_BUTTONS không hiện subText góc phải, cặp màu nút chuẩn Vivo)
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP Bank-Style (ưu tiên tin nhắn mới nhất asReversed() trong MessagingStyle)
│       │       ├── quickshare/       # Nhận diện Quick Share (lọc bỏ notification foreground service chạy ngầm vô nghĩa qua isServiceForegroundNotification)
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener (tách biệt isActualMediaPlaybackNotification và thông báo phụ của app nhạc) & KeepAliveAccessibilityService (bắt OTP vượt Redaction Android 15)
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
└── OriginIsle-VN-v1.2.19.apk         # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 19)
```

## 3. Bản Đồ Code (Code Map)
- `MediaCard.kt` (`app/src/main/java/com/originisle/android/cards/MediaCard.kt`): Hằng số `ID = 35010` và hàm `post` (lines 40-318) đảm bảo duy nhất 1 thẻ nhạc, chống spam `ACTION_START` qua bộ nhớ đệm (`lastTrack`, `lastArtist`, `lastPlaying`, `lastPkg`, `lastUiNightMode`, `lastArtSignature`), bỏ `subtext`/`oi_bg_color`/`oi_fg_color`, và vẽ 3 icon điều khiển thích ứng màu `#FF1C1C1E` (Light Mode) / `#FFFFFFFF` (Dark Mode).
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`): Hàm `isActualMediaPlaybackNotification` (lines 549-558) tách biệt thông báo phát nhạc thực sự (`token != null || CATEGORY_TRANSPORT || compactActions || isMediaStyleOrTransport`) với thông báo phụ của ứng dụng nhạc (`isMusicApp && !isActualMedia`, như `"Đang tải nhạc — 6/13 bài"` của Zing MP3).
- `OriginIslandBuilder.kt` (`app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt`): Nhánh `template == 8` (line 203) đặt `BUNDLE_KEY_BASE_SUB_INFO = 0` xóa bỏ chữ thời lượng bị cắt ở góc phải; `buildButtonList` (lines 338-340) dùng cặp màu chuẩn `Color.WHITE` + `Color.BLACK` của Vivo để không bị nhuộm Xanh Cổ Vịt.
- `PlaygroundService.kt` (`app/src/main/java/com/originisle/android/island/PlaygroundService.kt`): Khối `if (prevInScene != null && prevInScene != id)` (lines 365-380) gọi `endOrigin` và hủy thông báo cũ khỏi `NotificationManager` sau 200ms.
- `OtpCard.kt` (`app/src/main/java/com/originisle/android/cards/OtpCard.kt`) & `KeepAliveAccessibilityService.kt` (`app/src/main/java/com/originisle/android/service/KeepAliveAccessibilityService.kt`): Bắt OTP trực tiếp qua cả `NotificationListener` và `AccessibilityService` (`TYPE_NOTIFICATION_STATE_CHANGED`).

## 4. Những Điểm Cần Chú Ý Kỹ Thuật (Gotchas)
- `applicationId = "com.autonavi.minimap"`: Tuyệt đối giữ nguyên để vượt whitelist SuperX trên Vivo OriginOS 6.
- `showNotify = false`: Bắt buộc để thông báo chỉ hiển thị trên Đảo, không tạo thêm thông báo shade trên Android.
- Cặp màu nút bấm trên Vivo OriginOS 6: Bắt buộc để mặc định `btnColor = Color.WHITE` (`#FFFFFFFF`) + `textColor = Color.BLACK` (`#FF000000`) trong `OriginIslandBuilder.buildButtonList` để Vivo tự động đổi nền nút theo chế độ Sáng/Tối; nếu truyền màu opaque khác (`#FF1F1F1F`), Vivo sẽ nhuộm toàn bộ nút theo System Theme Color (Xanh Cổ Vịt).
