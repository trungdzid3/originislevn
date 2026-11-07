# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.4.0` (mã bản dựng `versionCode = 10400`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đồng bộ hoàn toàn lên `v2.2.3` / commit `73abb82`).
- **Kho lưu trữ GitHub**: `https://github.com/trungdzid3/originislevn.git` (`auto_github_sync: true`).

## 2. Cây Cấu Trúc Thư Mục Chuẩn
```text
origin-isle-vn/
├── .agent/
│   ├── GEMINI.md                     # Quy tắc toàn cục Antigravity (UTF-8 chuẩn)
│   └── rules.md                      # Rule cục bộ (auto_github_sync: true)
├── app/
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.4.0 / 10400)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── res/
│       │   │   ├── drawable/         # ic_tiktok_streak_fire.xml, ic_media_heart.xml, ic_quick_share.xml...
│       │   │   └── xml/keepalive_accessibility_config.xml
│       │   └── java/com/originisle/android/
│       │       ├── bank/             # Bóc tách biến động số dư Ngân hàng VN & SMS
│       │       ├── call/             # ZaloCallDetector.kt (bóc tách chuyên sâu RemoteViews cuộc gọi Zalo & CallStyle)
│       │       ├── cards/            # Các loại thẻ giao diện Đảo Origin
│       │       │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (TEMPLATE_BASE + vòng tiến độ #2B66FF, throttle 1s, oi_show_notify = false)
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Wearable (hiển thị "Đã ngắt" tối giản không khung xám)
│       │       │   ├── MediaCard.kt      # Cố định ID = 35010, oi_scene = "NAVIGATION", 3 nút chuẩn SuperX, hỗ trợ postFromController cho Zing MP3
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, loại bỏ hoàn toàn đoán mò 99%/80%, phân biệt Thất bại/Hủy và Hoàn tất thực tế 100%
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   ├── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms)
│       │       │   ├── TikTokStreakCard.kt # Cố định ID = 35012, hiển thị thông báo nhắc chuỗi TikTok ngọn lửa rực rỡ & nút 1 chạm duy trì chuỗi
│       │       │   └── ZaloCallCard.kt   # Cố định ID = 35011, hiển thị cuộc gọi Zalo (avatar bo tròn + nút Từ chối / Trả lời / Kết thúc)
│       │       ├── download/         # DownloadDetector chặn triệt để 100% Quick Share, Google Play Services & app nhạc
│       │       ├── island/           # PlaygroundService (showNotify = false), OriginIslandBuilder
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── quickshare/       # Nhận diện Quick Share
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN
│       │       ├── service/          # NotificationCastListener (nhóm ưu tiên: Zalo -> TikTok Streak -> Ngân hàng -> Quick Share)
│       │       ├── smart/            # Bóc tách thông tin màn hình
│       │       ├── sports/           # Tỷ số thể thao trực tiếp
│       │       ├── tiktok/           # TikTokStreakDetector.kt & TikTokStreakActionReceiver.kt
│       │       └── ui/               # Giao diện Compose (công tắc & ô nhập nội dung tin nhắn gửi chuỗi TikTok)
│       └── test/                     # Unit Tests (VietnamBankDetectorTest, QuietHoursTest)
├── backups/                          # Thư mục sao lưu an toàn khi đại tu
├── docs/
├── gradle/                           # Gradle wrapper & version catalogs
├── DESIGN.md                         # Nguồn chân lý thiết kế giao diện (SSOT)
├── PROJECT_CONTEXT.md                # Lưu trữ ngữ cảnh tinh gọn (Lean Context)
├── task.md                           # Phân quyền công việc & Locked Files
├── origin-isle-vn.code-workspace     # Cấu hình workspace
└── OriginIsle-VN-v1.4.0.apk          # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 4, Bản vá 0)
```

## 3. Bản Đồ Code (Code Map)
- `TikTokStreakDetector.kt` (`app/src/main/java/com/originisle/android/tiktok/TikTokStreakDetector.kt`): Bóc tách thông báo chuỗi TikTok (`com.ss.android.ugc.trill`, `com.zhiliaoapp.musically`), nhận diện kênh `dm_streaks_push` và từ khóa chuỗi/lửa 🔥.
- `TikTokStreakCard.kt` (`app/src/main/java/com/originisle/android/cards/TikTokStreakCard.kt`): Hằng số `ID = 35012`, hiển thị đảo ngọn lửa cam vàng rực rỡ `#FFFF5E00`, nút Duy trì chuỗi gọi qua `TikTokStreakActionReceiver`.
- `TikTokStreakActionReceiver.kt` (`app/src/main/java/com/originisle/android/tiktok/TikTokStreakActionReceiver.kt`): Tự động copy nội dung tùy chỉnh vào Clipboard, hiện Toast và mở cuộc trò chuyện TikTok.
- `HomeScreen.kt` & `ActivityScreen.kt`: Bổ sung Switch `cast_tiktok_streak` và trường nhập văn bản `tiktok_streak_custom_message` (mặc định `"🔥"`).
- `NotificationCastListener.kt`: Xử lý ưu tiên thông báo chuỗi TikTok ngay sau cuộc gọi Zalo.

## 4. Những Điểm Cần Chú Ý Kỹ Thuật (Gotchas)
- TikTok Streak: Do TikTok không cấp action trả lời ngầm `RemoteInput`, giải pháp tối ưu nhất là `TikTokStreakActionReceiver` copy sẵn tin nhắn vào Clipboard và mở thẳng cuộc trò chuyện để người dùng bấm gửi nhanh trong 1 chạm.
