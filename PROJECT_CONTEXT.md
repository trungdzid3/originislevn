# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.0.6` (mã bản dựng `versionCode = 10007`, chuẩn SemVer 2.0.0).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đồng bộ hoàn toàn lên `v2.2.3` / commit `73abb82`).
- **Kho lưu trữ GitHub**: `https://github.com/trungdzid3/originislevn.git` (`auto_github_sync: true`).

## 2. Cây Cấu Trúc Thư Mục Chuẩn
```text
origin-isle-vn/
├── .agent/
│   ├── GEMINI.md                     # Quy tắc toàn cục Antigravity (UTF-8 chuẩn)
│   └── rules.md                      # Rule cục bộ (auto_github_sync: true)
├── app/
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/java/com/originisle/android/
│       │   ├── bank/                 # Bóc tách biến động số dư Ngân hàng VN & SMS (lọc spam/promo 0đ)
│       │   ├── cards/                # 15 loại thẻ giao diện Đảo Origin
│       │   │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │   │   ├── DownloadCard.kt   # Tiến độ tải xuống (Scene: DELIEVERY)
│       │   │   ├── EarbudsCard.kt    # Tai nghe & Galaxy Fit 3 (chặn pop-up ảo, báo % pin, sạc đầy 100%, chip "Ngắt")
│       │   │   ├── MediaCard.kt      # Nhạc sóng động (Scene: MOVIE, tự thu gọn sau 3s)
│       │   │   ├── OtpCard.kt        # Thẻ OTP ưu tiên cao (is_ongoing=true, nút sao chép)
│       │   │   ├── QuickShareCard.kt # Quick Share logo chính thức Canvas 192x192
│       │   │   └── RingerModeCard.kt # 4 chế độ Chuông / Rung / Im lặng / DND đa tầng
│       │   ├── download/             # Nhận diện tải xuống (loại trừ Maps/Navigation/Ride)
│       │   ├── island/               # Service SuperX, builder, constants, multi-scene management
│       │   ├── log/                  # Nhật ký hoạt động bền vững (CastLog)
│       │   ├── otp/                  # Nhận diện mã xác thực OTP tiếng Việt (quét toàn bộ match, phân tách \n)
│       │   ├── quickshare/           # Nhận diện Quick Share & EasyShare & actions
│       │   ├── ride/                 # Nhận diện chuyến đi gọi xe & giao đồ ăn VN
│       │   ├── service/              # NotificationCastListener & Receivers
│       │   ├── smart/                # Bóc tách thông tin màn hình
│       │   ├── sports/               # Tỷ số thể thao trực tiếp
│       │   └── ui/                   # Giao diện Compose chuẩn OriginOS 6
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
└── OriginIsle-VN-v1.0.6-debug.apk    # File APK xuất bản duy nhất
```

## 3. Bản Đồ Code (Code Map)
- `PlaygroundService.kt` (`app/src/main/java/com/originisle/android/island/PlaygroundService.kt`):
  - `readAndPost` (L185-387): Quản lý `sceneCurrentId` độc lập theo scene (`MOVIE`, `DELIEVERY`, `TAXI`, `NAVIGATION`), cho phép xếp chồng và lướt đa nhiệm native trên Vivo SystemUI.
  - `cancelNotification` (L444-462): Dọn dẹp thẻ và scene tương ứng sạch sẽ.
  - `postSuperX` (L400-424): Đóng gói notification gửi tới Vivo SystemUI.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`):
  - `onNotificationPosted` (L424-650): Thứ tự ưu tiên nghiêm ngặt: Ngân hàng -> Quick Share -> Mã OTP -> Tải xuống -> Galaxy Fit 3 / Wearable -> Media -> Ride -> Dẫn đường.
  - `notificationText` (L740-787): Phân tách bằng `\n` tránh dính số điện thoại/ngày tháng vào mã OTP; bóc tách `android.messages`.
  - `onInterruptionFilterChanged`: Cầu nối trực tiếp chuyển trạng thái DND sang `RingerModeCard`.
- `Cards` (`app/src/main/java/com/originisle/android/cards/`):
  - `EarbudsCard.kt`: Quản lý `lastConnectionState`, chặn triệt để pop-up ảo từ notification nền của Samsung; khi kết nối ưu tiên hiển thị thẳng `% pin` lên viên con nhộng và vòng tròn camera; khi mất kết nối hiển thị viên chữ `"Ngắt"` đồng bộ; bổ sung `onDeviceFullyCharged` thông báo khi thiết bị sạc đầy pin 100%.
  - `VietnamBankDetector.kt`: `extractNumericAmount` loại bỏ số tiền `<= 0đ`; lọc triệt để tin nhắn rác / khuyến mãi / gói cước viễn thông nhận nhầm; siết chặt điều kiện SMS biến động số dư thực tế.
  - `RingerModeCard.kt`: Bắt đủ 4 chế độ Chuông (Vàng `#FCD535`), Rung (Lam `#4A90E2`), Im lặng (Đỏ `#FF5247`), Không làm phiền DND (Tím `#7B61FF`, icon trăng khuyết), lắng nghe đa tầng BroadcastReceiver + ContentObserver + InterruptionFilter.
  - `QuickShareCard.kt` & `IconCache.kt`: `createQuickShareOfficialIcon` vẽ logo Quick Share chính thức Canvas 192x192; chặn icon mảnh ghép của Play Services trong `IconCache.safeAppIcon`.
  - `DownloadDetector.kt`: Loại trừ triệt để Google Maps, Waze, Navigation, Ride và thắt chặt điều kiện progress bar.
  - `MediaCard.kt`: Độc lập scene `MOVIE`, tự đóng sau 3s khi dừng.

## 4. Lưu Ý Kỹ Thuật (Gotchas)
- **ApplicationId cố định**: Bắt buộc giữ `com.autonavi.minimap` trong `app/build.gradle.kts` để qua whitelist của Vivo Origin Island.
- **Tài nguyên Small Icon**: Mọi thông báo gửi tới Vivo SuperX phải sử dụng mipmap resource (`R.mipmap.ic_launcher_round`), không được dùng vector XML hoặc adaptive mipmap trong Compose painterResource.
- **Quy định xuất bản APK**: Luôn lưu APK thành phẩm trực tiếp tại thư mục gốc dự án: `OriginIsle-VN-v1.0.6-debug.apk`.

## 5. Việc Đang Mở (Lean Context)
- Toàn bộ 6 vấn đề kỹ thuật và yêu cầu tính năng mở rộng đều đã được giải quyết, kiểm thử đơn vị và biên dịch thành công 100%.
