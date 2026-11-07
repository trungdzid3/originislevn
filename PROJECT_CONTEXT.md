# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.3.2` (mã bản dựng `versionCode = 10302`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đồng bộ hoàn toàn lên `v2.2.3` / commit `73abb82`).
- **Kho lưu trữ GitHub**: `https://github.com/trungdzid3/originislevn.git` (`auto_github_sync: true`).

## 2. Cây Cấu Trúc Thư Mục Chuẩn
```text
origin-isle-vn/
├── .agent/
│   ├── GEMINI.md                     # Quy tắc toàn cục Antigravity (UTF-8 chuẩn)
│   └── rules.md                      # Rule cục bộ (auto_github_sync: true)
├── app/
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.3.2 / 10302)
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
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, loại bỏ hoàn toàn đoán mò 99%/80%, phân biệt chính xác Thất bại/Đã hủy (đỏ #FF5247) và Hoàn tất thực tế 100% (xanh lá #00D084)
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   ├── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms, loại bỏ internal intent gây nhảy Im lặng)
│       │       │   └── ZaloCallCard.kt   # Cố định ID = 35011, hiển thị cuộc gọi Zalo (avatar bo tròn + badge Zalo, nút Từ chối / Trả lời / Kết thúc)
│       │       ├── download/         # DownloadDetector chặn triệt để 100% Quick Share, Google Play Services & mọi ứng dụng nghe nhạc (Zing MP3, Spotify, NCT...)
│       │       ├── island/           # PlaygroundService (showNotify = false), OriginIslandBuilder (schema TEMPLATE_BUTTONS = 8 chuẩn SuperX 3 nút + BASE_SUB_INFO = 0)
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── quickshare/       # Nhận diện Quick Share (tách biệt rạch ròi isFailed, isCanceled, isFinished; loại bỏ foreground service vô nghĩa)
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener (ưu tiên ZaloCallDetector, isAuxiliaryMusicDownloadNotif, onNotificationRemoved không đoán mò)
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
└── OriginIsle-VN-v1.3.2.apk          # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 3, Bản vá 2)
```

## 3. Bản Đồ Code (Code Map)
- `QuickShareDetector.kt` (`app/src/main/java/com/originisle/android/quickshare/QuickShareDetector.kt`): `isFailed` và `isCanceled` (lines 340-358) phân tách rõ ràng từ khóa lỗi ("thất bại", "lỗi", "không thể gửi", "failed") và hủy ("đã hủy", "từ chối", "declined"), chặn tuyệt đối không cho `isFinished` nhận nhầm khi có lỗi.
- `QuickShareCard.kt` (`app/src/main/java/com/originisle/android/cards/QuickShareCard.kt`): `isFinishedEffective` (lines 153-155) chỉ chấp nhận `info.isFinished || info.progress == 100`, gỡ bỏ watchdog tự bung 100% ở 90..99%, xử lý hiển thị thất bại màu đỏ `#FF5247` tự hủy sau 2.5s.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`): Xử lý `onNotificationRemoved` (lines 1707-1758) chỉ bung hoàn tất khi thực sự đạt 100% hoặc `isFinishedEffective`, nếu có từ khóa lỗi/hủy thì hiển thị `postCanceled`, dở dang thì đóng đảo trong im lặng.

## 4. Những Điểm Cần Chú Ý Kỹ Thuật (Gotchas)
- Cơ chế Quick Share: Tuyệt đối KHÔNG đoán mò trạng thái hoàn tất dựa vào mốc 90..99% hoặc khi notification bị gỡ ngang chừng. Mọi trường hợp lỗi mạng hoặc hủy truyền phải được phản ánh đúng là Thất bại/Đã hủy, không được ép thành 100%.
