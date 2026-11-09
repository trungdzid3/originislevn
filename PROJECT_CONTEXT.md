# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.7.1` (mã bản dựng `versionCode = 10701`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đồng bộ hoàn toàn lên `v2.2.3` / commit `73abb82`).
- **Kho lưu trữ GitHub**: `https://github.com/trungdzid3/originislevn.git` (`auto_github_sync: true`).

## 2. Cây Cấu Trúc Thư Mục Chuẩn
```text
origin-isle-vn/
├── .agent/
│   ├── GEMINI.md                     # Quy tắc toàn cục Antigravity (UTF-8 chuẩn)
│   └── rules.md                      # Rule cục bộ (auto_github_sync: true)
├── app/
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.5.0 / 10500)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── res/
│       │   │   ├── drawable/         # ic_success_tick_green.xml, ic_tiktok_streak_fire.xml, ic_media_heart.xml, ic_quick_share.xml...
│       │   │   └── xml/              # keepalive_accessibility_config.xml, file_paths.xml, network_security_config.xml
│       │   └── java/com/originisle/android/
│       │       ├── bank/             # Bóc tách biến động số dư Ngân hàng VN & SMS
│       │       ├── calendar/         # CalendarEventDetector.kt (nhận diện lịch hẹn, cuộc họp, sự kiện sắp diễn ra & giờ bay/tàu xe)
│       │       ├── call/             # ZaloCallDetector.kt (bóc tách chuyên sâu cuộc gọi & CallStyle)
│       │       ├── cards/            # Các loại thẻ giao diện Đảo Origin
│       │       │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │       │   ├── CalendarEventCard.kt # Cố định ID = 35015, hiển thị nhắc lịch hẹn / đếm ngược sự kiện sắp tới với icon lịch vẽ Canvas
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (thay "100%" bằng icon tích xanh lá ic_success_tick_green khi hoàn tất)
│       │       │   ├── EarbudsCard.kt    # Tai nghe TWS & Wearable: bóc tách pin Trái (L), Phải (R), Hộp sạc (Case) từ Google Fast Pair (xử lý Unicode Bidi), hợp nhất mergeTelemetry và mở rộng thẻ 3 ô khi chạm vào Đảo
│       │       │   ├── MediaCard.kt      # Cố định ID = 35010, oi_scene = "NAVIGATION", 3 nút chuẩn SuperX, hỗ trợ postFromController cho các app nhạc (Spotify, YouTube Music...)
│       │       │   ├── PaymentCard.kt    # Chuẩn hóa giao diện mở rộng: xóa hoàn toàn khối chữ xám to đùng "Tiền ra / Tiền vào", hiển thị tiền gọn gàng sắc nét
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, chuyển trạng thái hoàn tất sang icon tích xanh lá ic_success_tick_green
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   ├── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms)
│       │       │   ├── TikTokStreakCard.kt # Cố định ID = 35012, hiển thị thông báo nhắc chuỗi TikTok ngọn lửa rực rỡ & nút 1 chạm duy trì chuỗi
│       │       │   └── ZaloCallCard.kt   # Cố định ID = 35011, chuẩn phong cách Messenger của OriginOS gốc: icon cuộc gọi trắng, nhãn phải "Zalo", chạm mở cuộc gọi Zalo, bỏ nút nghe (chỉ giữ "Từ chối" / "Kết thúc")
│       │       ├── download/         # DownloadDetector chặn triệt để 100% Quick Share, Google Play Services & app nhạc
│       │       ├── island/           # PlaygroundService, OriginIslandBuilder (hỗ trợ oi_island_click_expand mở rộng thẻ khi chạm vào Đảo, BASE_SUB_INFO = 0 khi subText rỗng)
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── quickshare/       # Nhận diện Quick Share
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN
│       │       ├── service/          # NotificationCastListener (ưu tiên Zalo Call -> Ngân hàng -> Quick Share -> Download; bỏ qua hoàn toàn Messenger để OriginOS 6 tự xử lý)
│       │       ├── smart/            # Bóc tách thông tin màn hình
│       │       ├── sports/           # Tỷ số thể thao trực tiếp
│       │       ├── tiktok/           # TikTokStreakDetector.kt & TikTokStreakActionReceiver.kt
│       │       └── ui/               # Giao diện Compose (công tắc Messenger Call, Zalo Call, TikTok Streak, nút Thử Tai nghe)
│       └── test/                     # Unit Tests (VietnamBankDetectorTest, QuietHoursTest, EarbudsBatteryParserTest)
├── backups/                          # Thư mục sao lưu an toàn khi đại tu
├── docs/
├── gradle/                           # Gradle wrapper & version catalogs
├── DESIGN.md                         # Nguồn chân lý thiết kế giao diện (SSOT)
├── PROJECT_CONTEXT.md                # Lưu trữ ngữ cảnh tinh gọn (Lean Context)
├── task.md                           # Phân quyền công việc & Locked Files
├── origin-isle-vn.code-workspace     # Cấu hình workspace
└── OriginIsle-VN-v1.7.1.apk          # File APK xuất bản siêu nhẹ (~4.3 MB, Thế hệ 1, Nâng cấp 7, Bản vá 1)
```

## 3. Bản Đồ Code (Code Map)
- `EarbudsCard.kt` (`ID_EARBUDS = 35004`, `ID_WEARABLE = 35014`), `OriginIslandBuilder.kt` & `PlaygroundService.kt`:
  - Làm sạch ký tự ẩn Unicode Bidi (`\u2068`, `\u2069`, `\u200E`, `\u200F`, `\u00A0`) trong `cleanBatteryText` và `parseBatteryText` để bóc tách chính xác 100% pin Tai Trái (L), Tai Phải (R) và Hộp sạc (Case) từ thông báo Google Play Services Fast Pair (`com.google.android.gms` - "Thiết bị", ví dụ: `"Tai nghe bên trái 50% Hộp sạc 20% Tai nghe bên phải 100%"`), Vivo TWS, Samsung Buds.
  - Hợp nhất thông minh qua `mergeTelemetry` & `storeTelemetry` để các sự kiện Bluetooth HFP (`ACTION_BATTERY_LEVEL_CHANGED`) không ghi đè xóa mất thông số `left`, `right`, `case` đã biết; tự động quét `activeNotifications` qua `scanActiveNotificationsForEarbuds`.
  - Hiển thị rõ bên trái `L:50%` và bên phải viên thuốc xanh `R:100%` trên viên nang thu gọn; khi ấn vào viên nang Đảo (`oi_island_click_expand = true`), mở rộng thẻ trên Đảo hiển thị rõ `"Tai trái: 50%  ·  Tai phải: 100%  ·  Hộp sạc: 20%"` cùng 3 ô tách biệt `[ Trái: 50% ]`, `[ Phải: 100% ]`, `[ Hộp: 20% ]` (dùng `Notification.Action` với `icon = null` tương thích 100% Vivo SuperX).
- `CalendarEventDetector.kt` & `CalendarEventCard.kt` (`ID = 35015`): Nhận diện thông báo nhắc lịch hẹn, cuộc họp, sự kiện sắp diễn ra và chuyến bay/tàu xe từ Google Calendar, Lịch Vivo, Samsung Calendar, Microsoft Outlook và các thông báo `CATEGORY_EVENT` / `CATEGORY_REMINDER`; trích xuất khung giờ / thời gian đếm ngược và hiển thị viên nang nhắc lịch kèm icon lịch vẽ trực tiếp bằng Canvas 100% tương thích Vivo SuperX SDK.
- `app/build.gradle.kts`, `proguard-rules.pro` & `ScreenshotSmartExtractor.kt`: Gỡ bỏ phụ thuộc `com.google.mlkit:text-recognition` (~55 MB native `.so`), gỡ quyền thừa `READ_MEDIA_IMAGES` và bật R8 `isMinifyEnabled = true` + `isShrinkResources = true` giúp giảm dung lượng APK từ 69.1 MB xuống còn **4.3 MB**.
- `UpdateChecker.kt` (`app/src/main/java/com/originisle/android/ui/UpdateChecker.kt`): Gia cố bảo mật toàn diện: lọc `sanitizeApkFileName` & kiểm tra `canonicalPath` chống Path Traversal, khóa `HttpsURLConnection` & whitelist tên miền GitHub (`isAllowedGithubUrl`) khi redirect, xác thực `packageName` và chứng chỉ ký (`verifyDownloadedApk`) trước khi cài đặt, tự động dọn dẹp APK cũ (`cleanupOldApks`).
- `CastLog.kt`, `OtpCard.kt`, `PaymentCard.kt`: Che dấu thông tin nhạy cảm (`redactSensitive`) khi lưu `cast_logs.json` xuống đĩa; gắn cờ `ClipDescription.EXTRA_IS_SENSITIVE = true` kèm tự động xóa Clipboard sau 60s cho mã OTP; hỗ trợ ẩn số dư ngân hàng và mã OTP trên màn hình khóa (`KeyguardManager.isDeviceLocked`, khóa `cast_lockscreen_redact_sensitive`).
- `VietnamBankDetector.kt` (`app/src/main/java/com/originisle/android/bank/VietnamBankDetector.kt`): Bổ sung `CHAT_SOCIAL_BLACKLIST` chặn triệt để `com.zing.zalo` và mọi ứng dụng mạng xã hội/OTT chat; kiểm tra nghiêm ngặt chỉ ngân hàng trong `BANK_PACKAGES` và SMS ngân hàng hợp lệ mới được phân tích giao dịch.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`): Gỡ bỏ vĩnh viễn `com.zing.zalo` khỏi `PAYMENT_APPS`; khóa chết `detectPayment` theo Whitelist (`sbn.packageName in PAYMENT_APPS`), xóa bỏ điều kiện nhận diện tiền tệ lỏng lẻo đối với ứng dụng lạ ngoài danh mục.
- `OriginIslandBuilder.kt` (`app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt`): Sửa nhánh `template == 4` chỉ đặt `BASE_SUB_INFO = 1` khi `subText` thực sự có nội dung; nếu rỗng thì đặt `BASE_SUB_INFO = 0` (loại bỏ hoàn toàn khối chữ xám to thô kệch bên phải thẻ mở rộng).
- `PaymentCard.kt` (`app/src/main/java/com/originisle/android/cards/PaymentCard.kt`): Bỏ chuỗi `subtext = "Tiền vào · ..."` / `"Tiền ra · ..."`, hiển thị số tiền to rõ nét, bố cục thẻ mở rộng chuẩn phong cách OriginOS 6.
- `QuickShareCard.kt` & `DownloadCard.kt`: Khi tải/truyền xong, hiển thị icon dấu tích tròn xanh lá viền rỗng (`createGreenTickCircleIcon` Canvas Bitmap + `ic_success_tick_green.xml`) chuẩn hình học ảnh tham chiếu thay cho chuỗi ký tự thô `"100%"` / `"Xong"`. Chống drop icon trên OriginOS 6 SuperX.
- `TikTokStreakCard.kt`: Nâng cấp giao diện chuẩn OriginOS: Template 6 viên thuốc cam mờ `#33FF5E00` ghi rõ số ngày (`🔥 15 ngày` / `🔥 Giữ chuỗi`), huy hiệu ngọn lửa bo tròn sắc nét (`createGlowingFlameBadge`), hiển thị tên bạn bè ở bên trái, hỗ trợ `postSample(context)`.
- `ZaloCallCard.kt` & `ZaloCallDetector.kt`: Chuẩn hóa giao diện cuộc gọi Zalo theo đúng phong cách Messenger của OriginOS gốc: viên thuốc hiển thị icon cuộc gọi trắng và nhãn phải "Zalo", chạm vào mở ngay màn hình cuộc gọi Zalo toàn màn hình (ưu tiên `fullScreenIntent`), bỏ nút nghe (chỉ giữ nút "Từ chối" hoặc "Kết thúc"), hỗ trợ `postSample(context)`.
- `NotificationCastListener.kt` & `MediaCard.kt`: Bỏ qua hoàn toàn cuộc gọi Facebook Messenger (`com.facebook.orca`) để nhường trọn vẹn cho Đảo gốc OriginOS 6; hỗ trợ chuẩn các app nhạc quốc tế (Spotify, YouTube Music, Apple Music).
- `UpdateChecker.kt` & `UpdateDialog.kt`: Kiểm tra và tự động cập nhật trực tiếp từ GitHub Releases (`trungdzid3/originislevn`), tải APK hiển thị thanh tiến độ %, gọi trình cài đặt Android qua FileProvider an toàn.

## 4. Những Điểm Cần Chú Ý Kỹ Thuật (Gotchas)
- Cuộc gọi Facebook Messenger: Đã được OriginOS 6 hỗ trợ trực tiếp từ gốc (native island). OriginIsle chủ động bỏ qua thông báo cuộc gọi `com.facebook.orca` (`category == CATEGORY_CALL`) để nhường trọn vẹn cho hệ thống gốc, tránh xung đột hiển thị 2 tiến trình/2 đảo đồng thời.
- Khối chữ xám to trong thẻ mở rộng: Bắt nguồn từ `BUNDLE_KEY_BASE_SUB_INFO = 1` của SuperX Bundle khiến hệ thống OriginOS render một sub-widget chữ xám lớn chiếm nửa phải thẻ mở rộng. Chỉ cần đặt `BASE_SUB_INFO = 0` khi `subText.isNullOrBlank()` là giao diện thẻ mở rộng trở nên sạch sẽ, cân đối 100%.
