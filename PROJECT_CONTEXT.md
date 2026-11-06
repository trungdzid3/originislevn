# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.7` (mã bản dựng `versionCode = 10207`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.7 / 10207)
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
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (Scene: NAVIGATION, oi_show_notify=false)
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Wearable (hiển thị "Đã ngắt" tối giản không khung xám)
│       │       │   ├── MediaCard.kt      # Nhạc sóng động & cơ chế nhường Đảo gốc Vivo động, trích xuất Album Art đa tầng
│       │       │   ├── OtpCard.kt        # Thẻ OTP tối ưu (trái "OTP", phải "$code · Chép", chạm chép clipboard)
│       │       │   ├── QuickShareCard.kt # Quick Share logo chính thức từ file SVG tải về
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chế độ Chuông / Rung / Im lặng / DND độc lập (RECEIVER_NOT_EXPORTED, Bitmap icon)
│       │       ├── download/         # Nhận diện tải xuống nhạy bén
│       │       ├── island/           # PlaygroundService (quản lý chiếm quyền scene & khôi phục thẻ nền), builder, constants
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP MessagingStyle (EXTRA_MESSAGES), Action Title, regex (OTP)
│       │       ├── quickshare/       # Nhận diện Quick Share & EasyShare & actions
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener, IconCache (100% Bitmap Icon), KeepAliveAccessibilityService
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
└── OriginIsle-VN-v1.2.7.apk          # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 7)
```

## 3. Bản Đồ Code (Code Map)
- `MediaCard.kt` (`app/src/main/java/com/originisle/android/cards/MediaCard.kt`) & `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`):
  - Xóa bỏ danh sách cố định `ORIGIN_WHITELISTED_MUSIC_APPS` (trước đây chặn nhầm Zing MP3, Spotify, YouTube Music).
  - `isOriginNativePlayerActive`: Chỉ trả về `true` khi `com.vivo.musicwidgetmix` (hoặc các gói widget nhạc hệ thống Vivo trong `ORIGIN_NATIVE_MUSIC_PACKAGES`) thực sự xuất hiện trong `activeNotifications`.
  - Trích xuất ảnh bìa đa tầng (từ `MediaMetadata.METADATA_KEY_ALBUM_ART`, `METADATA_KEY_ART`, `NotificationCompat.EXTRA_LARGE_ICON`, `n.getLargeIcon()`), vẽ ra Bitmap 100% an toàn và gắn `oi_force_show = true`, `oi_left_icon`, `oi_large_icon`.
- `RingerModeCard.kt` (`app/src/main/java/com/originisle/android/cards/RingerModeCard.kt`):
  - `register`: Đăng ký `RECEIVER_NOT_EXPORTED` trên Android 13+, kết hợp `ContentObserver` và `poll`.
- `PlaygroundService.kt` (`app/src/main/java/com/originisle/android/island/PlaygroundService.kt`):
  - Quản lý `sceneCurrentId[scene]` cho mọi ID thẻ và tự động khôi phục thẻ nền sau 150ms khi thẻ ngắn hạn đóng lại.

## 4. Lưu Ý Kỹ Thuật (Gotchas)
- **ApplicationId cố định**: Bắt buộc giữ `com.autonavi.minimap` trong `app/build.gradle.kts` để qua whitelist của Vivo Origin Island.
- **Tài nguyên Icon SuperX**: Mọi `Icon` đặt trong `Bundle` SuperX phải là `Icon.createWithBitmap` (`TYPE_BITMAP`), còn `NotificationCompat.Builder.setSmallIcon` dùng `R.mipmap.ic_launcher_round`.
- **Quy định xuất bản APK**: File APK thành phẩm lưu trực tiếp tại thư mục gốc dự án: `OriginIsle-VN-v1.2.7.apk`.

## 5. Việc Đang Mở (Lean Context)
- Không có lỗi tồn đọng. Bản dựng `v1.2.7` đã vượt qua 100% kiểm thử đơn vị và biên dịch thành công.
