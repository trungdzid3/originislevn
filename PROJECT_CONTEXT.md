# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.8` (mã bản dựng `versionCode = 10208`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.8 / 10208)
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
│       │       │   ├── MediaCard.kt      # Sóng âm HSV rực rỡ, khôi phục nền tối #FF262626 cho nút bấm, thời lượng bài hát
│       │       │   ├── OtpCard.kt        # Thẻ OTP chuẩn Ngân hàng (trái "OTP", phải mã OTP màu #00D084, chạm chép clipboard)
│       │       │   ├── QuickShareCard.kt # Quick Share logo chính thức từ file SVG tải về
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chế độ Chuông / Rung / Im lặng / DND tức thời (RECEIVER_EXPORTED, xóa oi_show_notify=false)
│       │       ├── download/         # Nhận diện tải xuống nhạy bén
│       │       ├── island/           # PlaygroundService (quản lý chiếm quyền scene & khôi phục thẻ nền), builder, constants
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP Bank-Style (STRONG_OTP_MARKER, quét MessagingStyle/publicVersion)
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
└── OriginIsle-VN-v1.2.8.apk          # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 8)
```

## 3. Bản Đồ Code (Code Map)
- `MediaCard.kt` (`app/src/main/java/com/originisle/android/cards/MediaCard.kt`):
  - `waveColorsFrom`: Chuyển đổi `HARDWARE` bitmap sang `ARGB_8888`, áp dụng hệ màu `HSV` tăng cường độ sáng (`V >= 0.85f`) để cột sóng âm bên phải viên nang luôn nhảy động rực rỡ, kèm fallback palette sáng.
  - `post`: Khôi phục `oi_bg_color = "#FF262626"` để 3 nút điều khiển ở `TEMPLATE_BUTTONS (8)` có nền tối nổi bật icon trắng; trích xuất thời lượng bài hát hiển thị trên `subtext`.
- `OriginIslandConstants.kt` & `OriginIslandBuilder.kt`:
  - Bổ sung `MUSIC` và `AUDIO` vào `SUPERX_SCENES`.
  - Nhánh `rightTemplate == 1` luôn ghi `waveColor` hợp lệ vào Bundle SuperX.
- `OtpDetector.kt` & `OtpCard.kt`:
  - `OtpDetector`: Bổ sung quy tắc nhận diện nhanh Bank-Style qua `STRONG_OTP_MARKER`, quét trọn vẹn cụm 4–8 chữ số độc lập (lọc số tiền, hotline, SĐT, năm sinh).
  - `NotificationCastListener.kt`: Gọi `OtpDetector.find(sbn, actionTitles)` quét cả `MessagingStyle` (`EXTRA_MESSAGES`) và `publicVersion`.
  - `OtpCard.kt`: Tái cấu trúc theo chuẩn `TEMPLATE_BASE (4)` y hệt `PaymentCard.postBankTransaction`: bên trái hiện icon + chữ `"OTP"`, bên phải hiện mã OTP màu `#00D084`, chạm vào chép ngay mã vào Clipboard.
- `RingerModeCard.kt` (`app/src/main/java/com/originisle/android/cards/RingerModeCard.kt`):
  - Đăng ký `RECEIVER_EXPORTED` trên Android 13+, ưu tiên đọc `EXTRA_RINGER_MODE` tức thời từ Intent.
  - Đọc thiết lập rung từ cả `Settings.System` và `Settings.Global`.
  - Xóa bỏ cờ `oi_show_notify = false` (gỡ bỏ chặn hiển thị trên Vivo), dọn `IconCache.activeSmallIcons` và đặt `is_ongoing = false`.

## 4. Lưu Ý Kỹ Thuật (Gotchas)
- **ApplicationId cố định**: Bắt buộc giữ `com.autonavi.minimap` trong `app/build.gradle.kts` để qua whitelist của Vivo Origin Island.
- **Tài nguyên Icon SuperX**: Mọi `Icon` đặt trong `Bundle` SuperX phải là `Icon.createWithBitmap` (`TYPE_BITMAP`), còn `NotificationCompat.Builder.setSmallIcon` dùng `R.mipmap.ic_launcher_round`.
- **Quy định xuất bản APK**: File APK thành phẩm lưu trực tiếp tại thư mục gốc dự án: `OriginIsle-VN-v1.2.8.apk`.

## 5. Việc Đang Mở (Lean Context)
- Không có lỗi tồn đọng. Bản dựng `v1.2.8` đã vượt qua 100% kiểm thử đơn vị và biên dịch thành công.
