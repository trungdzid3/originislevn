# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.1.0` (mã bản dựng `versionCode = 10100`, chuẩn SemVer 2.0.0).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đồng bộ hoàn toàn lên `v2.2.3` / commit `73abb82`).
- **Kho lưu trữ GitHub**: `https://github.com/trungdzid3/originislevn.git` (`auto_github_sync: true`).

## 2. Cây Cấu Trúc Thư Mục Chuẩn
```text
origin-isle-vn/
├── .agent/
│   ├── GEMINI.md                     # Quy tắc toàn cục Antigravity (UTF-8 chuẩn)
│   └── rules.md                      # Rule cục bộ (auto_github_sync: true)
├── app/
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.1.0 / 10100)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── res/drawable/ic_quick_share.xml # Vector drawable Quick Share chuẩn chính thức 100%
│       │   └── java/com/originisle/android/
│       │       ├── bank/             # Bóc tách biến động số dư Ngân hàng VN & SMS (lọc spam/promo 0đ)
│       │       ├── cards/            # 15 loại thẻ giao diện Đảo Origin
│       │       │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (Scene: NAVIGATION, oi_show_notify=false)
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Galaxy Fit 3 (GATT 0x180F/0x2A19, wearable_last_known_battery, fallback % pin)
│       │       │   ├── MediaCard.kt      # Nhạc sóng động (Scene: NAVIGATION, tự thu gọn sau 3s)
│       │       │   ├── OtpCard.kt        # Thẻ OTP ưu tiên cao (is_ongoing=true, nút sao chép)
│       │       │   ├── QuickShareCard.kt # Quick Share logo chính thức từ file SVG tải về (PathParser Canvas)
│       │       │   └── RingerModeCard.kt # 4 chế độ Chuông / Rung / Im lặng / DND độc lập + polling 1s
│       │       ├── download/         # Nhận diện tải xuống nhạy bén (loại trừ Maps/Media/Wearable/System)
│       │       ├── island/           # Service SuperX, builder, constants, resolveAllowedScene
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện mã xác thực OTP tiếng Việt (quét toàn bộ match, phân tách \n)
│       │       ├── quickshare/       # Nhận diện Quick Share & EasyShare & actions
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN
│       │       ├── service/          # NotificationCastListener & Receivers
│       │       ├── smart/            # Bóc tách thông tin màn hình
│       │       ├── sports/           # Tỷ số thể thao trực tiếp
│       │       └── ui/               # Giao diện Compose chuẩn OriginOS 6
│       └── test/                     # Unit Tests
├── backups/                          # Thư mục sao lưu an toàn khi đại tu
├── docs/
│   ├── DEV.md                        # Hướng dẫn build, ký APK và AMap spoof
│   ├── PROTOCOL.md                   # Đặc tả kỹ thuật SuperX của OriginOS
│   ├── ECOSYSTEM_REFERENCE.md        # Tổng hợp hệ sinh thái Đảo Origin & HyperIsland
│   ├── architecture/
│   │   └── ARCHITECTURE.md           # Sơ đồ kiến trúc & luồng dữ liệu (Mermaid)
│   └── screenshots/
├── gradle/                           # Gradle wrapper & version catalogs
├── scripts/
│   ├── README.md
│   ├── generate_origin_icons.py
│   └── release.sh
├── DESIGN.md                         # Nguồn chân lý thiết kế giao diện (SSOT)
├── PROJECT_CONTEXT.md                # Lưu trữ ngữ cảnh tinh gọn (Lean Context)
├── task.md                           # Phân quyền công việc & Locked Files
├── origin-isle-vn.code-workspace     # Cấu hình workspace
└── OriginIsle-VN-v1.1.0.apk          # File APK xuất bản chuẩn chuyên nghiệp
```

## 3. Bản Đồ Code (Code Map)
- `QuickShareDetector.kt` (`app/src/main/java/com/originisle/android/quickshare/QuickShareDetector.kt`):
  - `parse` (L45-125): Kiểm tra cờ `FLAG_ONGOING_EVENT`. Khi thông báo không còn ongoing và `max <= 0`, xác định là hoàn tất (`isFinished = true`), mở rộng từ khóa hoàn tất ("đã lưu", "lưu vào", "sẵn sàng", "chạm để mở", "saved", "ready"), xóa bỏ hoàn toàn việc gán ép `progress = 10` & `isIndeterminate = true`.
- `QuickShareCard.kt` (`app/src/main/java/com/originisle/android/cards/QuickShareCard.kt`):
  - `createQuickShareOfficialIcon` (L38-115): Sử dụng `androidx.core.graphics.PathParser` vẽ chính xác 100% hai nhánh mũi tên xoay tròn và hình tròn xanh `#4285F4` từ tệp SVG `Quickshare.svg` người dùng cung cấp; liên kết `R.drawable.ic_quick_share`.
  - `show` (L120-210): Đồng bộ cờ `is_ongoing` với `sbn.isOngoing`. Nếu không ongoing hoặc `info.isFinished`, chuyển ngay sang thẻ Hoàn tất (tích xanh, dismiss sau 3.5s). Tích hợp cơ chế Watchdog timeout 45 giây chống kẹt thẻ.
- `EarbudsCard.kt` (`app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`):
  - `saveCachedBattery` / `getCachedBattery` (L164-210): Tự động khử tên đuôi `(xxxx)`, so khớp mờ và lưu trữ pin vào key toàn cầu `wearable_last_known_battery`.
  - `readGattBatteryAsync` (L215-280): Đọc pin phần cứng trực tiếp từ thiết bị BLE qua GATT Service `0x180F` (Battery Service) / Characteristic `0x2A19`.
  - `show` (L1100-1200): Cơ chế fallback pin thông minh, nếu pin tức thời chưa về kịp thì nạp ngay mức pin đã biết gần nhất để luôn hiển thị `% pin` trên viên con nhộng và vòng tròn tiến độ camera selfie.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`):
  - `onNotificationPosted` (L550-685): Quét sâu `extras.keySet()` của ứng dụng Samsung Wearable để trích xuất pin ẩn; nạp cache pin ngay khi nhận sự kiện kết nối.
  - `onNotificationRemoved` (L750-800): Ngăn chặn `postCanceled` sai khi notification hoàn tất được gỡ bỏ.

## 4. Lưu Ý Kỹ Thuật (Gotchas)
- **ApplicationId & Scene**: Bắt buộc giữ `applicationId = "com.autonavi.minimap"` và ưu tiên `oi_scene = "NAVIGATION"`.
- **Tài nguyên Small Icon**: Mọi thông báo gửi tới Vivo SuperX phải sử dụng mipmap resource (`R.mipmap.ic_launcher_round`).
- **Quy định xuất bản APK**: Luôn lưu APK thành phẩm trực tiếp tại thư mục gốc dự án theo chuẩn SemVer: `OriginIsle-VN-v1.1.0.apk`.

## 5. Việc Đang Mở (Lean Context)
- Không có việc tồn đọng. Bản dựng v1.1.0 hoạt động ổn định và sẵn sàng sử dụng.
