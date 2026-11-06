# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.2` (mã bản dựng `versionCode = 10202`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.2 / 10202)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── res/
│       │   │   ├── drawable/         # ic_quick_share.xml, ic_smartband.xml, ic_headphones.xml...
│       │   │   └── xml/keepalive_accessibility_config.xml # Quét node UI pin Wearable
│       │   └── java/com/originisle/android/
│       │       ├── bank/             # Bóc tách biến động số dư Ngân hàng VN & SMS (lọc spam/promo 0đ)
│       │       ├── cards/            # 15 loại thẻ giao diện Đảo Origin
│       │       │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (Scene: NAVIGATION, oi_show_notify=false)
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Wearable (Hiện "Đã kết nối", bỏ khung xám ngắt kết nối)
│       │       │   ├── MediaCard.kt      # Nhạc sóng động & cơ chế nhường Đảo gốc OriginOS 6 (Zero-Overlap)
│       │       │   ├── OtpCard.kt        # Thẻ OTP ưu tiên cao (is_ongoing=true, nút sao chép mã)
│       │       │   ├── QuickShareCard.kt # Quick Share logo chính thức từ file SVG tải về
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chế độ Chuông / Rung / Im lặng / DND độc lập (showNotify=true)
│       │       ├── download/         # Nhận diện tải xuống nhạy bén
│       │       ├── island/           # Service SuperX, builder, constants, resolveAllowedScene
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP tiếng Việt có dấu & không dấu (SMS Brandname)
│       │       ├── quickshare/       # Nhận diện Quick Share & EasyShare & actions
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener & Receivers & KeepAliveAccessibilityService
│       │       ├── smart/            # Bóc tách thông tin màn hình
│       │       ├── sports/           # Tỷ số thể thao trực tiếp
│       │       └── ui/               # Giao diện Compose đại tu chuẩn Vivo OriginOS 6 (5 nhóm, lồng nhau)
│       └── test/                     # Unit Tests
├── backups/                          # Thư mục sao lưu an toàn khi đại tu
├── docs/
├── gradle/                           # Gradle wrapper & version catalogs
├── DESIGN.md                         # Nguồn chân lý thiết kế giao diện (SSOT)
├── PROJECT_CONTEXT.md                # Lưu trữ ngữ cảnh tinh gọn (Lean Context)
├── task.md                           # Phân quyền công việc & Locked Files
├── origin-isle-vn.code-workspace     # Cấu hình workspace
└── OriginIsle-VN-v1.2.2.apk          # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 2)
```

## 3. Bản Đồ Code (Code Map)
- `EarbudsCard.kt` (`app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`):
  - Khi kết nối chưa xác định % pin: Chuyển chữ hiển thị từ "Vòng đeo" sang "Đã kết nối" chuẩn chỉ.
  - Khi ngắt kết nối: Chuyển template từ `TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT` (Template 6) sang `TEMPLATE_RIGHT_ISLAND_TEXT_ICON` (Template 4), loại bỏ hoàn toàn khung bo tròn xám (capsule), hiển thị chữ "Đã ngắt" và icon đỏ thanh thoát.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`): Lọc từ khóa ngắt kết nối, chặn flap.
- `MediaCard.kt` (`app/src/main/java/com/originisle/android/cards/MediaCard.kt`): Cơ chế nhường Đảo gốc OriginOS 6.
- `RingerModeCard.kt` (`app/src/main/java/com/originisle/android/cards/RingerModeCard.kt`): Thẻ Chuông / Rung / Im lặng độc lập.
- `HomeScreen.kt`, `Theme.kt`, `Components.kt`: Giao diện Cài đặt Vivo OriginOS 6 lồng nhau 5 nhóm chuẩn mực.

## 4. Việc Đang Mở
- Không còn việc mở. Bản vá lỗi v1.2.2 đã hoàn thành và sẵn sàng sử dụng.
