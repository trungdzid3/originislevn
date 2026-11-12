# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `2.0.1` (mã bản dựng `versionCode = 20001`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đồng bộ hoàn toàn lên `v2.2.3` / commit `73abb82`).
- **Kho lưu trữ GitHub**: `https://github.com/trungdzid3/originislevn.git` (`auto_github_sync: true`).

## 2. Cây Cấu Trúc Thư Mục Chuẩn
```text
origin-isle-vn/
├── .agent/
│   ├── GEMINI.md                     # Quy tắc toàn cục Antigravity (UTF-8 chuẩn)
│   └── rules.md                      # Rule cục bộ (auto_github_sync: true)
├── app/
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v2.0.1 / 20001)
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
│       │       └── ui/               # Giao diện Compose (DeviceInfoHelper, Device Hero Dashboard, Tùy biến Đảo 2.0.0 & Chặn Pop-up)
│       └── test/                     # Unit Tests (VietnamBankDetectorTest, QuietHoursTest, EarbudsBatteryParserTest, OtpDetectorTest)
├── backups/                          # Thư mục sao lưu an toàn khi đại tu
├── docs/
├── gradle/                           # Gradle wrapper & version catalogs
├── DESIGN.md                         # Nguồn chân lý thiết kế giao diện (SSOT)
├── PROJECT_CONTEXT.md                # Lưu trữ ngữ cảnh tinh gọn (Lean Context)
├── task.md                           # Phân quyền công việc & Locked Files
├── origin-isle-vn.code-workspace     # Cấu hình workspace
├── release/OriginIsle-VN-v2.0.1.apk  # File APK xuất bản v2.0.1 (~4.4 MB, R8 minify + shrinkResources)
```

## 3. Bản Đồ Code (Code Map)
- **Origin Isle VN v2.0.0 — Hợp đồng `experimental_prefs` (`Prefs.kt`), Device Hero UI (`DeviceInfoHelper.kt`, `HomeScreen.kt`, `Components.kt`)**:
  - `DeviceInfoHelper.kt` (`collect`, `isSuperXSupported`): Thu thập `DeviceSnapshot` (Model, OriginOS/Android, SoC, RAM khả dụng/Tổng, Pin & Nhiệt độ, lượt Đảo hôm nay từ `CastLog.entries`).
  - `HomeScreen.kt` (`DeviceHeroCard`, nhóm `Tùy biến Đảo 2.0.0 & Chặn Pop-up`): Mô phỏng viên thuốc Origin Island 2.0.0 với viền phát sáng `animateColorAsState`, bộ lọc chặn pop-up nổi (`cast_suppress_heads_up`, `cast_suppress_mode`: `SNOOZE` / `ISLAND_ONLY`), chọn màu viền (`ColorSwatchPill` trong `Components.kt`), thời lượng viên thuốc (`3s`/`5s`/`7s`/`10s`) và tùy chọn chạm mở rộng thẻ (`cast_tap_expand_card`).
- **Chặn Pop-up nổi & Engine Tùy biến Đảo (`NotificationCastListener.kt`, `PlaygroundService.kt`)**:
  - `NotificationCastListener.kt` (`suppressSystemPopupIfEnabled`, `onNotificationRemoved`): Hỗ trợ 2 chế độ chặn heads-up cho thông báo thoáng qua (bỏ qua cuộc gọi/media/download/Quick Share/ongoing): `SNOOZE` (`snoozeNotification(key, 150L)` kèm guard `recentlySnoozedKeys` TTL 3s & `lastCastPostTime`) và `ISLAND_ONLY` (`cancelNotification(key)` trễ 120ms kèm `islandOnlyCancelledKeys` TTL 15s giữ nguyên thẻ Đảo).
  - `PlaygroundService.kt` (`startCard`): Áp dụng `cast_custom_light_color` (`NONE` -> `0`, hex `#...` trừ `ICON_STATUS_FAIL`), `cast_island_duration_sec` cho `oi_island_show_time` & auto-dismiss thẻ thoáng qua, và `cast_tap_expand_card` cho `oi_island_click_expand` (trừ cuộc gọi/tai nghe/thiết bị đeo bắt buộc).
- `BatteryCanvasRenderer.kt`, `EarbudsCard.kt`, `OriginIslandBuilder.kt` & `NotificationCastListener.kt` (`ID_EARBUDS = 35004`, `ID_WEARABLE = 35014`):
  - Đại tu giao diện tai nghe TWS 3D & Ngắt kết nối tức thì (v2.0.1):
    1. `OriginIslandBuilder.kt`: Đổi `shortImage = baseHeaderIcon ?: defaultIcon` trong `shortInfos` để tránh nhân đôi Bitmap lớn gây `TransactionTooLargeException` Binder IPC.
    2. `BatteryCanvasRenderer.kt` (`createChargingCase3DIcon`, `createSingleEarbudIcon`, `createTwsExpandedCompositeIcon`): Vẽ Hộp sạc 3D Pearl White mở nắp vuông 1:1 `192x192` (`ARGB_8888`) với 2 đầu tai nghe trong hốc và đèn LED phát sáng; `createSingleEarbudIcon` dựng tai nghe đơn chiếc 3D `192x192` nghiêng vào tâm, bỏ vạch pin xanh; `createTwsExpandedCompositeIcon` trả thẳng về `createChargingCase3DIcon` (không lặp 2 tai nghe hai bên).
    3. `EarbudsCard.kt` (`post`, `isDeviceHardwareConnected`, `onFastPairNotificationRemoved`): Khi `hasDualBuds` dùng `TEMPLATE_TEXT_SYMMETRY` (3), dùng `createSingleEarbudIcon` cho 2 bên và `createTwsExpandedCompositeIcon` ở giữa khi `hasCase` (hoặc `createTransparentIcon()` + `oi_mid_text = ""` khi `!hasCase`). Giảm `DISCONNECT_DEBOUNCE_MS = 1200L` cho tai nghe (`WEARABLE_DISCONNECT_DEBOUNCE_MS = 6000L`), đăng ký `BluetoothA2dp`/`BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED`, đồng bộ `lastConnectedAudioDeviceName`/`Address`, chống spam 5s, và `isDeviceHardwareConnected(..., isWearable = false)` chỉ kiểm tra profile âm thanh (`HEADSET`/`A2DP`/`LE_AUDIO`, bỏ GATT/`isConnected()`).
    4. `NotificationCastListener.kt` (`onNotificationRemoved`): Gọi `EarbudsCard.onFastPairNotificationRemoved(title, text)` ngay khi thông báo pin tai nghe / Fast Pair bị hệ thống xóa.
    5. Chặn triệt để lỗi pin ảo 3% và pop-up ngầm thiết bị đeo (`EarbudsCard.kt`, `NotificationCastListener.kt`, `KeepAliveAccessibilityService.kt`):
       - Loại bỏ hoàn toàn việc đọc key `"battery_status"` và `"value"` (vốn chứa hằng số hệ thống `BATTERY_STATUS_DISCHARGING = 3`).
       - Bỏ vòng lặp duyệt mù cột SQLite trong `queryProviderForBattery` (tránh đọc nhầm cột `_id = 3` hay `status = 3`).
       - Sửa regex trích xuất pin: bắt buộc phải có ký tự `%` ở cuối; loại bỏ các từ khóa tên thiết bị (`fit 3`, `galaxy fit`, `watch`) khỏi danh sách nhãn pin; bỏ qua quét regex nếu văn bản không chứa ký tự `%`.
       - Dọn dẹp cache `wearable_battery_cache` bị lưu nhầm `3%` cũ khi khởi động `start()`.
       - Khóa chặn logic hiển thị: chỉ bật thẻ Đảo khi có sự kiện kết nối/ngắt kết nối phần cứng thực tế (`isDeviceHardwareConnected`), không tự ý bật thẻ từ thông báo nền, nội dung thường trực hay quét Accessibility ngầm.
- `DownloadCard.kt` & `NotificationCastListener.kt`:
  - Đồng bộ trạng thái tải xuống hoàn tất chuẩn như Quick Share (chuyển tệp): loại bỏ hoàn toàn các chuỗi ký tự thô `"Tải xong"` và `"100%"` trên viên nang thu gọn; thay bằng biểu tượng dấu tích tròn xanh lá Canvas rực rỡ (`createGreenTickCircleIcon`) ở bên phải (`oi_show_right_icon = true`), bên trái giữ logo ứng dụng nguồn, tiêu đề mở rộng `"$appLabel · Đã tải xong"`. Chống drop icon trên Vivo OriginOS 6 SuperX.
- `QuickShareDetector.kt` & `NotificationCastListener.kt`:
  - Chặn triệt để thông báo kết nối Bộ công cụ văn phòng Vivo (Vivo Office Suite, PC Suite, EasyShare, Link to Windows): bổ sung `isOfficeOrPCConnection` và `OFFICE_PC_PACKAGES` (`com.vivo.easyshare`, `com.vivo.pcsuite`, `com.vivo.smartoffice`, `com.vivo.office`, `com.vivo.connectivity`, `com.vivo.multiscreen`, `com.microsoft.appmanager`), lọc triệt để các thông báo "Đã kết nối với 'LAPTOP-...' / Bộ công cụ văn phòng" ngay từ đầu `onNotificationPosted` và trong `QuickShareDetector.isQuickShare` để không bao giờ chiếm dụng Đảo vì OriginOS 6 đã có thông báo và Đảo gốc mặc định.
- `EarbudsCard.kt` (`ID_EARBUDS = 35004`, `ID_WEARABLE = 35014`), `OriginIslandBuilder.kt` & `PlaygroundService.kt`:
  - Tách bạch tuyệt đối 100% giữa Tai nghe Bluetooth và Thiết bị đeo (Đồng hồ / Vòng đeo tay Galaxy Fit 3, Galaxy Watch, Smart Band):
    1. Quét độc lập trong `pollConnectedDeviceNow`: phân rã 2 luồng riêng biệt `audioDevice` cho `ID_EARBUDS` và `wearableDevice` cho `ID_WEARABLE`, tuyệt đối không dùng chung một biến `targetDevice`.
    2. Cô lập hoàn toàn bộ nhớ viễn trắc telemetry và cache pin giữa 2 nhóm: hàm `getStoredTelemetry` có cờ `isWearable`, cấm tuyệt đối việc gán chi tiết Left/Right/Case của tai nghe sang cho Thiết bị đeo (Wearable).
    3. Cưỡng chế thẻ Thiết bị đeo luôn dùng `TEMPLATE_BASE` (4) với icon Smartband/Đồng hồ và vòng pin tròn, không bao giờ nhảy nhầm sang bố cục đối xứng 3 cột tai nghe (`TEMPLATE_TEXT_SYMMETRY`).
    4. Sửa cờ `currentlyShowing` và kiểm tra slot độc lập (`showingWearableAddress` vs `showingEarbudsAddress`) trong `tryParseBatteryFromNotification`.
  - Thẻ mở rộng tai nghe TWS đối xứng 3 cột cao cấp (`TEMPLATE_TEXT_SYMMETRY` 3): Cột trái hiển thị % pin Tai trái to rõ + nhãn "Tai trái", Cột giữa đặt biểu tượng tai nghe lớn + thông số "Hộp sạc: xx%", Cột phải hiển thị % pin Tai phải to rõ + nhãn "Tai phải".
  - Thiết bị đơn/thiết bị đeo giữ chuẩn `TEMPLATE_BASE` (4) với vòng tròn tiến độ pin (`cardRing = effectivePct`).
  - Viên nang thu gọn hiển thị `[Icon] L:xx%` bên trái và `R:xx%` (template 4) bên phải cân xứng 100%, không viền xanh lệch màu.
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
