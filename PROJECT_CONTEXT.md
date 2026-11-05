# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.0.5` (mã bản dựng `versionCode = 10006`, chuẩn SemVer 2.0.0).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đồng bộ hoàn toàn lên `v2.2.3` / commit `73abb82`).
- **Kho lưu trữ GitHub**: `https://github.com/trungdzid3/originislevn.git` (`auto_github_sync: true`).

## 2. Cây Cấu Trúc Thư Mục Chuẩn
```text
origin-isle-vn/
├── .agent/
│   ├── GEMINI.md                     # Quy tắc toàn cục Antigravity (UTF-8 chuẩn)
│   └── rules.md                      # Rule cục bộ (auto_github_sync: true)
├── .github/
│   ├── FUNDING.yml
│   └── workflows/
│       └── verify-release.yml        # Workflow kiểm thử phát hành
├── app/
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/java/com/originisle/android/
│       │   ├── bank/                 # Bóc tách biến động số dư Ngân hàng VN & SMS
│       │   ├── cards/                # 14 loại thẻ giao diện Đảo Origin
│       │   ├── download/             # Nhận diện tiến trình tải xuống
│       │   ├── island/               # Service SuperX, builder, constants
│       │   ├── log/                  # Nhật ký hoạt động bền vững (CastLog)
│       │   ├── otp/                  # Nhận diện mã xác thực OTP tiếng Việt
│       │   ├── quickshare/           # Nhận diện Quick Share & EasyShare
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
└── OriginIsle-VN-v1.0.5-debug.apk    # File APK xuất bản duy nhất
```

## 3. Bản Đồ Code (Code Map)
- `PlaygroundService.kt` (`app/src/main/java/com/originisle/android/island/PlaygroundService.kt`):
  - `readAndPost` (khoảng L185-387): Nạp thông số Intent và gửi thẻ SuperX lên Đảo.
  - `onRejected` (khoảng L395-408): Tự động hạ cấp thẻ về dạng cơ bản không nút khi bị Đảo từ chối.
  - `Icon.ownedBy` (khoảng L415-419): Gán package sở hữu cho icon tránh lỗi trên một số ROM Vivo.
  - `postSuperX` (khoảng L426-458): Đóng gói notification gửi tới Vivo SystemUI.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`):
  - `onNotificationPosted` (khoảng L424-621): Cổng tiếp nhận thông báo theo thứ tự ưu tiên: Ngân hàng -> Quick Share -> Tải xuống -> OTP -> Nhạc -> Gọi xe -> Google Wallet -> Điều hướng -> Thông báo thường.
  - `cleanUpRejectedSuperX` (khoảng L760-774): Hủy bản fallback trên thanh thông báo và kích hoạt `ACTION_REJECTED`.
- `Cards` (`app/src/main/java/com/originisle/android/cards/`):
  - `GenericCard.kt` (L31-215): Thẻ chung & helper `shownActions`.
  - `ChargingCard.kt` (L80-132): Thẻ sạc pin với `%` bên phải camera và `oi_card_ring`.
  - `PaymentCard.kt` (L52-170): Apple Pay style cho Google Wallet & biến động số dư ngân hàng VN.
  - `QuickShareCard.kt`, `DownloadCard.kt`, `OtpCard.kt`, `EarbudsCard.kt`, `RideCard.kt`, `NavigationCard.kt`.
- `AppsScreen.kt` (`app/src/main/java/com/originisle/android/ui/AppsScreen.kt`):
  - `AppsScreen` (L52-175): Quản lý bật/tắt thông báo từng ứng dụng.
  - `AppIcon` (L213-223): Tải biểu tượng bất đồng bộ kèm bộ đệm cache.
  - `loadApps` (L225-241): Truy vấn ứng dụng cài đặt và ứng dụng hệ thống.

## 4. Lưu Ý Kỹ Thuật (Gotchas)
- **ApplicationId cố định**: Bắt buộc giữ `com.autonavi.minimap` trong `app/build.gradle.kts` để qua whitelist của Vivo Origin Island.
- **Tài nguyên Small Icon**: Mọi thông báo gửi tới Vivo SuperX phải sử dụng mipmap resource (`R.mipmap.ic_launcher_round`), không được dùng vector XML hoặc adaptive mipmap trong Compose painterResource.
- **Quy định xuất bản APK**: Luôn lưu APK thành phẩm trực tiếp tại thư mục gốc dự án: `OriginIsle-VN-v1.0.5-debug.apk`.

## 5. Việc Đang Mở (Lean Context)
- Hiện tại toàn bộ tác vụ dọn dẹp, chuẩn hóa cấu trúc và đồng bộ tính năng upstream v2.2.3 đã hoàn tất.
