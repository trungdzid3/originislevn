# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.3.1` (mã bản dựng `versionCode = 10301`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.3.1 / 10301)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── res/
│       │   │   ├── drawable/         # ic_media_heart.xml, ic_quick_share.xml, ic_smartband.xml, ic_headphones.xml...
│       │   │   └── xml/keepalive_accessibility_config.xml # Quét node UI pin Wearable (tối giản, đã gỡ bỏ lắng nghe OTP)
│       │   └── java/com/originisle/android/
│       │       ├── bank/             # Bóc tách biến động số dư Ngân hàng VN & SMS (lọc spam/promo 0đ, chặn hạn mức quảng cáo)
│       │       ├── call/             # ZaloCallDetector.kt (bóc tách chuyên sâu RemoteViews cuộc gọi Zalo v26.10.02 & CallStyle)
│       │       ├── cards/            # Các loại thẻ giao diện Đảo Origin
│       │       │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (TEMPLATE_BASE + vòng tiến độ #2B66FF, throttle 1s, oi_show_notify = false)
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Wearable (hiển thị "Đã ngắt" tối giản không khung xám)
│       │       │   ├── MediaCard.kt      # Cố định ID = 35010, oi_scene = "NAVIGATION", 3 nút chuẩn SuperX, hỗ trợ postFromController cho Zing MP3
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, xử lý dứt điểm kẹt 99% (isFinishedEffective khi >= 99% & watchdog 2.5s cho 90..99%), throttle 1s + >=2%, oi_show_notify = false
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   ├── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms, loại bỏ internal intent gây nhảy Im lặng)
│       │       │   └── ZaloCallCard.kt   # Cố định ID = 35011, hiển thị cuộc gọi Zalo (avatar bo tròn + badge Zalo, nút Từ chối / Trả lời / Kết thúc)
│       │       ├── download/         # DownloadDetector chặn triệt để 100% Quick Share, Google Play Services & mọi ứng dụng nghe nhạc (Zing MP3, Spotify, NCT...)
│       │       ├── island/           # PlaygroundService (showNotify = false), OriginIslandBuilder (schema TEMPLATE_BUTTONS = 8 chuẩn SuperX 3 nút + BASE_SUB_INFO = 0)
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── quickshare/       # Nhận diện Quick Share (lọc bỏ notification foreground service chạy ngầm vô nghĩa qua isServiceForegroundNotification)
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener (ưu tiên ZaloCallDetector, isAuxiliaryMusicDownloadNotif, isActualMediaPlaybackNotification, quét dự phòng MediaSessionManager)
│       │       ├── smart/            # Bóc tách thông tin màn hình
│       │       ├── sports/           # Tỷ số thể thao trực tiếp
│       │       └── ui/               # Giao diện Compose đại tu chuẩn Vivo OriginOS 6 (công tắc & mẫu thử Cuộc gọi Zalo)
│       └── test/                     # Unit Tests (VietnamBankDetectorTest, QuietHoursTest)
├── backups/                          # Thư mục sao lưu an toàn khi đại tu
├── docs/
├── gradle/                           # Gradle wrapper & version catalogs
├── DESIGN.md                         # Nguồn chân lý thiết kế giao diện (SSOT)
├── PROJECT_CONTEXT.md                # Lưu trữ ngữ cảnh tinh gọn (Lean Context)
├── task.md                           # Phân quyền công việc & Locked Files
├── origin-isle-vn.code-workspace     # Cấu hình workspace
└── OriginIsle-VN-v1.3.1.apk          # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 3, Bản vá 1)
```

## 3. Bản Đồ Code (Code Map)
- `MediaCard.kt` (`app/src/main/java/com/originisle/android/cards/MediaCard.kt`): Hằng số `ID = 35010`, hàm `post` (lines 110-124), `postFromController` (lines 126-144) và `dispatchMediaCard` (lines 146-350) dùng `oi_scene = "NAVIGATION"`, `oi_force_show = true`, 3 nút điều khiển chuẩn (`Prev`, `Play/Pause`, `Next`) và `TEMPLATE_RIGHT_ISLAND_WAVE` để tương thích 100% với Vivo SuperX Validator.
- `OriginIslandBuilder.kt` (`app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt`): Khối `baseInfos` đặt `template == 8 -> base.putInt(C.BUNDLE_KEY_BASE_SUB_INFO, 0)` (line 206), khối `infos` nhánh `8` giới hạn `.take(3)` với cặp màu chuẩn Vivo `Color.BLACK` / `Color.WHITE` (lines 339-346), và khối `right` nhánh `1` truyền `BUNDLE_KEY_ISLAND_RIGHT_PLAY_PAG` cùng mảng màu sóng mặc định (lines 129-138).
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`): Hàm `isAuxiliaryMusicDownloadNotif` (lines 556-588) chỉ chặn thông báo tải nhạc ngoại tuyến (`"Đang tải nhạc"`, `"6/13 bài"`), hàm `isActualMediaPlaybackNotification` (lines 590-621) nhận diện thông báo phát nhạc Zing MP3 qua `findMediaControllerFallback`, và `pollRunnable` (lines 726-757) quét dự phòng `MediaSessionManager.getActiveSessions`.
- `ZaloCallDetector.kt` (`app/src/main/java/com/originisle/android/call/ZaloCallDetector.kt`): Hàm `isZaloCall` và `parse` đọc trực tiếp `mActions` bên trong `RemoteViews` (`noti_custom_call_*` `0x7f0c04c1..0x7f0c04c3`, view IDs `tv_title = 0x7f091d27`, `tv_content = 0x7f091b86`, `imv_avatar = 0x7f090bbd`, `fl_btn_left = 0x7f090977`, `fl_btn_right = 0x7f090978`, `elapse_time = 0x7f090863`).
- `ZaloCallCard.kt` (`app/src/main/java/com/originisle/android/cards/ZaloCallCard.kt`): Hằng số `ID = 35011` hiển thị cuộc gọi Zalo lên Đảo Động.

## 4. Những Điểm Cần Chú Ý Kỹ Thuật (Gotchas)
- `applicationId = "com.autonavi.minimap"` & `oi_scene = "NAVIGATION"`: Package giả lập AMap trên Vivo OriginOS 6 chỉ được SystemUI cấp quyền hiển thị Đảo cho scene `"NAVIGATION"`. Mọi thẻ truyền `oi_scene = "MUSIC"` đều bị từ chối.
- Giới hạn của `TEMPLATE_BUTTONS (8)` trong Vivo SuperX: Bắt buộc `BASE_SUB_INFO = 0` và tối đa `.take(3)` nút với màu hợp lệ (`Color.WHITE` / `Color.BLACK`); nếu truyền > 3 nút hoặc `BASE_SUB_INFO = 3`, SuperX Validator sẽ đánh dấu `SUPERX_VALUE = "false"` và hủy thẻ.
- `showNotify = false`: Bắt buộc để thông báo chỉ hiển thị trên Đảo, không tạo thêm thông báo shade trên Android.
