# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `2.1.4` (mã bản dựng `versionCode = 20104`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đồng bộ hoàn toàn lên `v2.2.3` / commit `73abb82`).
- **Kho lưu trữ GitHub**: `https://github.com/trungdzid3/originislevn.git` (`auto_github_sync: true`).

## 2. Cây Cấu Trúc Thư Mục Chuẩn
```text
origin-isle-vn/
├── .agent/
│   ├── GEMINI.md                     # Quy tắc toàn cục Antigravity (UTF-8 chuẩn)
│   └── rules.md                      # Rule cục bộ (auto_github_sync: true)
├── app/
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v2.1.4 / 20104, minSdk = 34)
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
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (ẩn khi đang mở Chrome/Edge/Play Store, tự động chui vào Đảo ngay khi thoát app, tích xanh khi hoàn tất)
│       │       │   ├── EarbudsCard.kt    # Tai nghe TWS & Wearable: bóc tách pin Trái (L), Phải (R), Hộp sạc (Case) từ Google Fast Pair (xử lý Unicode Bidi), hợp nhất mergeTelemetry và mở rộng thẻ 3 ô khi chạm vào Đảo
│       │       │   ├── MediaCard.kt      # Cố định ID = 35010, oi_scene = "NAVIGATION", 3 nút chuẩn SuperX, hỗ trợ postFromController cho các app nhạc (Spotify, YouTube Music...)
│       │       │   ├── PaymentCard.kt    # Chuẩn hóa giao diện mở rộng: xóa hoàn toàn khối chữ xám to đùng "Tiền ra / Tiền vào", hiển thị tiền gọn gàng sắc nét
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, chuyển trạng thái hoàn tất sang icon tích xanh lá ic_success_tick_green
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   ├── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms)
│       │       │   ├── TikTokStreakCard.kt # Cố định ID = 35012, hiển thị thông báo nhắc chuỗi TikTok ngọn lửa rực rỡ & nút 1 chạm duy trì chuỗi
│       │       │   └── ZaloCallCard.kt   # Cố định ID = 35011, chuẩn phong cách Messenger của OriginOS gốc: icon cuộc gọi trắng, nhãn phải "Zalo", chạm mở cuộc gọi Zalo, bỏ nút nghe (chỉ giữ "Từ chối" / "Kết thúc")
│       │       ├── download/         # DownloadDetector chặn triệt để 100% Quick Share, Google Play Services & app nhạc
│       │       ├── island/           # PlaygroundService, OriginIslandBuilder (thuần Vivo OriginOS 6 SuperX)
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── quickshare/       # Nhận diện Quick Share
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN
│       │       ├── service/          # NotificationCastListener (ưu tiên Zalo Call -> Ngân hàng -> Quick Share -> Download; bỏ qua hoàn toàn Messenger để OriginOS 6 tự xử lý)
│       │       ├── smart/            # Bóc tách thông tin màn hình
│       │       ├── sports/           # Tỷ số thể thao trực tiếp
│       │       ├── tiktok/           # TikTokStreakDetector.kt & TikTokStreakActionReceiver.kt
│       │       └── ui/               # Giao diện Compose (DeviceInfoHelper, Device Hero Dashboard, Tùy biến Đảo 2.0.0 & Chặn Pop-up)
│       └── test/                     # Unit Tests (VietnamBankDetectorTest, QuietHoursTest, EarbudsBatteryParserTest, OtpDetectorTest, DownloadBrowserDetectionTest)
├── backups/                          # Thư mục sao lưu an toàn khi đại tu
├── docs/
├── gradle/                           # Gradle wrapper & version catalogs
├── DESIGN.md                         # Nguồn chân lý thiết kế giao diện (SSOT)
├── PROJECT_CONTEXT.md                # Lưu trữ ngữ cảnh tinh gọn (Lean Context)
├── task.md                           # Phân quyền công việc & Locked Files
├── origin-isle-vn.code-workspace     # Cấu hình workspace
├── release/OriginIsle-VN-v2.1.4.apk  # File APK xuất bản v2.1.4 (4.40 MB, R8 minify + shrinkResources)
```

## 3. Bản Đồ Code (Code Map)
- **Origin Isle VN v2.1.4 — Ẩn tiến trình tải khi đang ở trong Chrome/Edge/Play Store & Tự động chui vào Đảo khi thoát ra**:
  - `DownloadDetector.kt` (`app/src/main/java/com/originisle/android/download/DownloadDetector.kt`): Thêm `isBrowserPackage(pkg)` nhận diện Chrome, Edge, Vivo Browser, Brave, Firefox, Opera, Cốc Cốc, Samsung Browser, DuckDuckGo, Via và các gói chứa `"browser"`; đồng bộ `isDownloadPackage(pkg)` gọi `isBrowserPackage(pkg)`.
  - `KeepAliveAccessibilityService.kt` (`app/src/main/java/com/originisle/android/service/KeepAliveAccessibilityService.kt`): Lưu `instance`, `currentForegroundPackage`, `IGNORED_FOREGROUND_PKGS`, `isIgnoredWindowPkg(pkg)`, `isBrowserCurrentlyOpen(downloadSourcePkg)` (kiểm tra live `rootInActiveWindow`, hỗ trợ cả trình duyệt, `com.android.providers.downloads*`, và `com.android.vending` / `isDownloadPackage`); theo dõi đổi cửa sổ tiền cảnh trong `onAccessibilityEvent` và gọi `DownloadCard.onForegroundPackageChanged`.
  - `DownloadCard.kt` (`app/src/main/java/com/originisle/android/cards/DownloadCard.kt`): Thêm `DeferredDownload`, `deferredDownloads`, `hideTemporarily`, `onForegroundPackageChanged`, và `post(..., forceImmediate: Boolean = false)`. Khi đang tải từ trình duyệt hoặc Play Store (`isDownloadPackage`) mà app nguồn còn ở tiền cảnh thì chỉ lưu `deferredDownloads` và ẩn khỏi Đảo; ngay khi thoát khỏi app nguồn thì `onForegroundPackageChanged` gọi `post(..., forceImmediate = true)` để tiến trình chui ngay vào Đảo.
  - `DownloadBrowserDetectionTest.kt` (`app/src/test/java/com/originisle/android/download/DownloadBrowserDetectionTest.kt`): Unit tests kiểm tra `isBrowserPackage`, `isIgnoredWindowPkg`, và `isBrowserCurrentlyOpen` cho Chrome/Edge/DownloadManager/Play Store.
  - `app/build.gradle.kts` & `release/OriginIsle-VN-v2.1.4.apk`: Đặt `versionCode = 20104`, `versionName = "2.1.4"`, xuất bản APK release (4,610,699 bytes / ~4.40 MB).
- **Origin Isle VN v2.1.3 — Khắc phục báo `%` tải Chrome/Edge & chống khuyết cạnh phải icon tích xanh tải xong**:
  - `DownloadDetector.kt` (`app/src/main/java/com/originisle/android/download/DownloadDetector.kt`): Xóa `"tiếp"` khỏi `MEDIA_ACTION_KEYWORDS`; trong `isMediaStyleNotification`, trả về `false` ngay khi `CATEGORY_PROGRESS`, khi có thanh progress (`EXTRA_PROGRESS_MAX > 0` / `EXTRA_PROGRESS_INDETERMINATE`) mà không có cờ MediaSession/MediaStyle, hoặc khi `actions` chứa nút tải (`"hủy"`, `"huỷ"`, `"cancel"`, `"tiếp tục"`, `"resume"`).
  - `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`): Chặn nhận nhầm tải trình duyệt làm Media trong `isActualMediaPlaybackNotification` khi `token == null && DownloadDetector.isDownloadPackage(sbn.packageName)`; đổi `oi_right_content` / `status_chip_text` thành `"\u2009 "` trong `postDownloadFinished`.
  - `DownloadCard.kt` & `QuickShareCard.kt` (`app/src/main/java/com/originisle/android/cards/`): Trong `createGreenTickCircleIcon` chỉnh `cx = s * 0.44f`, `cy = s * 0.50f`, `r = s * 0.35f`, `ringPaint.strokeWidth = s * 0.072f`, `tickPaint.strokeWidth = s * 0.10f`; cho phép `info.progress in 0..99`, bổ sung `isFirstPost` và ngưỡng `800ms` / `>= 1%` trong `DownloadCard.post`; đặt `oi_right_content = " "` và `status_chip_text = " "` ở trạng thái hoàn tất.
  - `app/build.gradle.kts` & `release/OriginIsle-VN-v2.1.3.apk`: Đặt `versionCode = 20103`, `versionName = "2.1.3"`, xuất bản APK release (4,610,699 bytes / ~4.40 MB).
- **Origin Isle VN v2.2.0 — Thuần hóa Vivo OriginOS 6 SuperX (Gỡ bỏ hoàn toàn One UI Overlay sang dự án độc lập `oneui-isle-vn`)**:
  - `app/build.gradle.kts` & `AndroidManifest.xml`: Trả về `minSdk = 34`, nâng `versionCode = 20200`, `versionName = "2.2.0"`, gỡ bỏ quyền `SYSTEM_ALERT_WINDOW`.
  - `OneUiOverlayIsland.kt`: Đã xóa hoàn toàn khỏi `origin-isle-vn`.
  - `PlaygroundService.kt`: Trả về thuần Vivo SuperX (`lightColor = 0`), bỏ nhánh `OneUiOverlayIsland`.
  - `Prefs.kt`, `StatusHelpers.kt`, `HomeScreen.kt`, `OnboardingScreen.kt`, `PermissionsSheet.kt`: Gỡ bỏ toàn bộ cấu hình, nút tinh chỉnh X/Y, helper và mục cấp quyền One UI Overlay; giữ `testerExpanded = false` và không có viền phát sáng.
  - `NotificationCastListener.kt`: Giữ `reload()` chuẩn và `pollRunnable` `1000L` cho Vivo SuperX.
  - `release/OriginIsle-VN-v2.2.0.apk`: Xuất bản APK release (4,610,695 bytes / ~4.40 MB, `versionCode = 20200`, `versionName = "2.2.0"`).
- **Origin Isle VN v2.1.2 — Xóa màu viền phát sáng & Diệt triệt để giật lag One UI 5.1+**:
  - `OneUiOverlayIsland.kt` (`app/src/main/java/com/originisle/android/island/OneUiOverlayIsland.kt`): Xóa `addOnLayoutChangeListener` gây vòng lặp `updateViewLayout`, thay bằng hiệu chuẩn trục `Y` 1 lần theo `(offsetY, pillScalePct, orientation)` + ngưỡng `>= 2px` cho X/Y; thêm `contentSignature` chống spam `postOrUpdate`; đặt `currentCardState = null` trong `hideOverlayLocked()` và `refreshActiveCardLocked()` khi hiện lại; xóa hoàn toàn màu viền phát sáng (dùng viền hairline cố định `#2EFFFFFF` / `#2E2E32`).
  - `PlaygroundService.kt` (`app/src/main/java/com/originisle/android/island/PlaygroundService.kt`): Gán cố định `lightColor = 0` (bỏ đọc `cast_custom_light_color`).
  - `Prefs.kt` & `HomeScreen.kt` (`app/src/main/java/com/originisle/android/ui/`): Xóa hoàn toàn mục chọn màu viền phát sáng trong `Prefs.kt` và `HomeScreen.kt` (giữ viền tối giản `DeviceHeroCard`), thu gọn mặc định Phòng thử nghiệm mẫu Đảo (`testerExpanded = false`), giảm `DeviceInfoHelper.collect` xuống 1 lần lúc khởi động & giãn poll `connected` lên `2500ms` chỉ gán khi đổi.
  - `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`): Chặn `reload()` lúc `onListenerConnected` và giãn `pollRunnable` lên `2500ms` trên thiết bị không có SuperX.
  - `release/OriginIsle-VN-v2.1.2.apk`: Xuất bản APK release (4,717,485 bytes / ~4.50 MB, `versionCode = 20102`, `versionName = "2.1.2"`).
- **Origin Isle VN v2.1.2 — Phát hành hợp nhất (One UI 5.1+ v2.1.1 + Vá dứt điểm lỗi báo pin cũ & tách dòng RemoteViews Xiaomi Earbuds `com.mi.earphone`)**:
  - `app/build.gradle.kts`: Nâng `versionCode = 20102`, `versionName = "2.1.2"` (thay thế bản đánh nhầm `v2.0.4` / `20004`).
- **Bản vá TWS & Xiaomi Earbuds (đã gộp vào `v2.1.2`)**:
  - `EarbudsCard.kt` (`app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`):
    + Thêm `clearAllAudioTelemetry(context)` xóa sạch `deviceTelemetry` và `SharedPreferences("wearable_battery_cache")` của mọi thiết bị âm thanh (giữ nguyên wearable) khi ngắt kết nối (`disconnectTask`, `onDeviceDisconnected`, `onFastPairNotificationRemoved`) và khi bắt đầu phiên kết nối mới (`!wasAlreadyConnectedBefore && !isWearableDevice`).
    + Bảo vệ `pending` khi `A2DP`/`HEADSET STATE_CONNECTED` bắn nối đuôi `ACTION_ACL_CONNECTED` (`hasPendingAudioWait && !initial.hasDetails` không hủy `pending` cũ và không đọc cache cũ).
    + Trong `show(...)`, khi `!isWearableDevice && telemetry.hasDetails`, không trộn `stored` cũ (giữ nguyên `case = -1` khi đã lấy tai nghe ra khỏi hộp).
    + Trong `scanActiveNotificationsForEarbuds`, thêm `maxAgeMs = 12_000L` bỏ qua thông báo cũ tồn đọng từ lần mở trước lúc mới kết nối.
    + Trong `extractAllNotificationText`, nối thêm `rvTexts.joinToString(" ")` bên cạnh `" | "` cho `RemoteViews` để bóc tách chuẩn `ConnectNotificationService` của `com.mi.earphone`.
    + Mở rộng cửa sổ cập nhật pin tươi lên `30_000L` trong `tryParseBatteryFromNotification` và `handle(else -> BATTERY_LEVEL_CHANGED)`.
  - `EarbudsBatteryParserTest.kt`: Bổ sung test bóc tách chuỗi `RemoteViews` của Xiaomi Earbuds (`"Trái 80% Phải 90% Hộp sạc 50%"` và `"Left 85% Right 90%"`).
- **Origin Isle VN v2.1.1 — Hoàn thiện Đảo One UI 5.1+ (Khóa tâm lỗ camera, BAL API 34+, Ẩn xoay ngang, Cử chỉ tự nhiên, Xem thử căn chỉnh)**:
  - `OneUiOverlayIsland.kt` (`app/src/main/java/com/originisle/android/island/OneUiOverlayIsland.kt`): Khóa tâm thực của vùng `Spacer` (`cutoutSpacerCenterOffsetPx` qua `onGloballyPositioned`) vào `cutoutCenterX + offsetX`; nâng cấp `sendPendingIntentSafe` hỗ trợ `ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED` trên Android 14+ (API 34+); tự động ẩn khi xoay ngang (`Configuration.ORIENTATION_LANDSCAPE`) nếu `oneui_island_hide_landscape == true`; bổ sung cử chỉ chạm ngoài (`FLAG_WATCH_OUTSIDE_TOUCH`) thu gọn, vuốt lên thu gọn, vuốt ngang chuyển/đóng thẻ và trạng thái `isCardExpanded`.
  - `PlaygroundService.kt` (`app/src/main/java/com/originisle/android/island/PlaygroundService.kt`): Lắng nghe `onConfigurationChanged` để cập nhật ẩn/hiện khi xoay ngang và hoãn auto-dismiss 2 giây khi thẻ đang mở rộng (`isCardExpanded`).
  - `Prefs.kt` & `HomeScreen.kt` (`app/src/main/java/com/originisle/android/ui/`): Bổ sung `PREF_ONEUI_HIDE_LANDSCAPE = "oneui_island_hide_landscape"` (mặc định `true`), hàm `postOneUiCalibrationPreview(id = 35019)` tự động xem thử khi bấm chip X/Y, nút `PillButton` xem thử căn chỉnh và công tắc `ToggleRow` ẩn khi xoay ngang.
  - `app/build.gradle.kts`: Nâng `versionCode = 20101`, `versionName = "2.1.1"`.
- **Origin Isle VN v2.1.0 — Mở rộng hỗ trợ Samsung One UI 5.1+ (WindowManager + Compose Overlay, Không Trợ năng)**:
  - `OneUiOverlayIsland.kt` (`app/src/main/java/com/originisle/android/island/OneUiOverlayIsland.kt`): Engine đảo nổi trực tiếp qua `WindowManager` (`TYPE_APPLICATION_OVERLAY`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`) + `ComposeView`, lắng nghe trực tiếp `SharedPreferences("experimental_prefs")` (`oneui_island_offset_x_px`, `oneui_island_offset_y_px`, `oneui_island_pill_scale_pct`, `island_position`, `cast_custom_light_color`). Chống lệch 4 lớp: đọc `currentWindowMetrics` & `displayCutout.boundingRectsTop`, kết hợp offset X/Y, tự bù `deltaY` qua `getLocationOnScreen`, và chừa khoảng trống `cutoutWidthDp + 12.dp` giữa viên thuốc khi ở `CENTER`.
  - `PlaygroundService.kt`: Định tuyến giữa Vivo SuperX gốc và `OneUiOverlayIsland` qua `DeviceInfoHelper.isSuperXSupported(this)`; `ensureForeground()` chỉ truyền `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` khi `SDK_INT >= 34`.
  - `app/build.gradle.kts` & `AndroidManifest.xml`: Hạ `minSdk = 33`, nâng `versionCode = 20100`, `versionName = "2.1.0"`, bổ sung quyền `SYSTEM_ALERT_WINDOW`.
  - `DeviceInfoHelper.kt`, `StatusHelpers.kt`, `Prefs.kt`, `HomeScreen.kt`, `OnboardingScreen.kt`, `PermissionsSheet.kt`: Nhận diện One UI >= 5.1, thêm helper kiểm tra/xin quyền Overlay (`Settings.canDrawOverlays`), bỏ yêu cầu Trợ năng trên One UI và thêm UI tinh chỉnh tâm Đảo X/Y.
  - `EarbudsCard.kt` & `NotificationCastListener.kt`: Bổ sung các package Samsung Galaxy Buds (`com.samsung.accessory.*`, `com.samsung.android.hearablemgr`) để hỗ trợ thiết bị đeo & tai nghe Samsung trên One UI 5.1+.
- **Origin Isle VN v2.0.3 — Diệt lỗi báo pin cũ lần trước khi mở tai nghe & Hỗ trợ Xiaomi Earbuds (`com.mi.earphone`)**:
  - `EarbudsCard.kt` (`clearCachedTelemetry`, `isWithinAntiFlapCooldown`, `handle`, `tryParseBatteryFromNotification`, `TWS_NOTIFICATION_PACKAGES`):
    + Thêm `clearCachedTelemetry(context, vararg keysOrNull)` xóa sạch cache RAM (`deviceTelemetry`) và `SharedPreferences("wearable_battery_cache")` khi ngắt kết nối và khi mới kết nối tai nghe TWS (`!wasAlreadyConnectedBefore && !isWearableDevice`).
    + Không đọc lại `getStoredTelemetry` / `getCachedBattery` từ lần mở trước khi mới kết nối TWS; chờ `BATTERY_WAIT_MS = 2500L` đón gói tin Fast Pair / HFP mới nhất.
    + Mở rộng `isWithinAntiFlapCooldown(..., windowMs = 15_000L)`: trong 15 giây đầu vừa kết nối, nếu nhận được pin mới thật (`changed == true`) qua thông báo hoặc `BATTERY_LEVEL_CHANGED`, luôn cho phép `show(..., refreshDuration = true)` cập nhật ngay lên Đảo.
    + Thêm `"com.mi.earphone"` vào `TWS_NOTIFICATION_PACKAGES`.
  - `NotificationCastListener.kt` (`pollRunnable`): Bổ sung `"com.mi.earphone"` và `"com.xiaomi.wearable"` vào `isEarbudsPkg` để chủ động quét thông báo pin Xiaomi Earbuds.
- **Origin Isle VN v2.0.2 — Đại tu Đảo tai nghe TWS bằng `oi_sub_image` Full-Width & Tắt viền phát sáng mặc định**:
  - `BatteryCanvasRenderer.kt` (`createEarbudsExpandedBanner`, `drawHorizontalBatteryBadge`):
    + Tạo Bitmap `600 x 210 px` (`ARGB_8888`, ~492 KB, an toàn Binder IPC) vẽ thẻ banner mở rộng nền kính tối `#EB16181E` bo góc 28dp, viền hairline 1.2dp.
    + Hỗ trợ 3 kịch bản: 3 cột đối xứng có Hộp sạc (`hasCase`), 2 cột lớn cân đối khi đã lấy tai ra khỏi hộp (`!hasCase`), và 1 bên tai nghe (ghép hộp sạc hoặc tai đơn phóng to giữa).
    + Huy hiệu pin ngang chuẩn mẫu Vivo: số % chữ trắng đậm kèm viên pin ngang 30x16dp bo tròn góc (`#00D084` hoặc đỏ `#FF5247` khi <= 15%), tia sét vàng `#FFD54F` khi đang sạc.
  - `EarbudsCard.kt`: Chuyển thẻ mở rộng tai nghe sang `TEMPLATE_BASE = 4` kèm `oi_sub_image = banner`, `title = name.take(22)`, `text = if (lowWarning) ... else "Đã kết nối"`, `subtext = ""` (xóa sạch các extra thừa của Template 3). Xóa bỏ toàn bộ `putExtra("oi_light_color", ...)` để không còn viền phát sáng trên thẻ tai nghe.
  - `Prefs.kt`, `PlaygroundService.kt`, `HomeScreen.kt`: Đặt giá trị mặc định `cast_custom_light_color = "NONE"`. Trong `PlaygroundService.kt`, khi `"NONE"` gán `lightColor = 0` tắt hoàn toàn viền hào quang phát sáng xung quanh Đảo (trừ khi người dùng chủ động vào Cài đặt chọn màu khác).
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
