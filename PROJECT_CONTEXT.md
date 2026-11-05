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
│       │   ├── bank/                 # Bóc tách biến động số dư Ngân hàng VN & SMS
│       │   ├── cards/                # 15 loại thẻ giao diện Đảo Origin
│       │   │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │   │   ├── DownloadCard.kt   # Tiến độ tải xuống có icon bắt đầu & action buttons
│       │   │   ├── EarbudsCard.kt    # Tai nghe & vòng đeo Galaxy Fit 3 (X đỏ khi ngắt)
│       │   │   ├── QuickShareCard.kt # Quick Share có action buttons & tích xanh
│       │   │   └── RingerModeCard.kt # Chế độ Chuông / Rung / Im lặng OriginOS 6
│       │   ├── download/             # Nhận diện tiến trình tải xuống & actions
│       │   ├── island/               # Service SuperX, builder, constants
│       │   ├── log/                  # Nhật ký hoạt động bền vững (CastLog)
│       │   ├── otp/                  # Nhận diện mã xác thực OTP tiếng Việt
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
  - `readAndPost` (L185-387): Nạp thông số Intent và gửi thẻ SuperX lên Đảo.
  - `onRejected` (L395-408): Tự động hạ cấp thẻ về dạng cơ bản không nút khi bị Đảo từ chối.
  - `Icon.ownedBy` (L415-419): Gán package sở hữu cho icon tránh lỗi trên một số ROM Vivo.
  - `postSuperX` (L426-458): Đóng gói notification gửi tới Vivo SystemUI.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`):
  - `onNotificationPosted` (L424-650): Cổng tiếp nhận thông báo: Ngân hàng -> Quick Share -> Tải xuống -> Samsung Wearable / Fit 3 -> OTP -> Nhạc -> Gọi xe -> Google Wallet -> Điều hướng -> Thông báo thường.
  - `onNotificationRemoved` (L900-1080): Bắt sự kiện gỡ thông báo: với Tải xuống và Quick Share, luôn duy trì `activeDownloadIds` để khi gỡ mà chưa xong thì chuyển sang hoạt ảnh Đã hủy (X đỏ 2.5s) thay vì ngắt phụt làm đảo tắt ngấm.
- `Cards` (`app/src/main/java/com/originisle/android/cards/`):
  - `EarbudsCard.kt`: Hỗ trợ cả tai nghe TWS & thiết bị đeo thông minh (Galaxy Fit 3, Galaxy Watch,...), lắng nghe 8 Broadcasts Samsung, quét BLE GATT profile 7, máy trạng thái Kết nối (xanh lá 5s) / Mất kết nối (vòng tròn dấu X đỏ bên trong `createRedCrossCircleIcon`, template 4 icon-only, giữ 3s), bố cục pin co giãn tự động 1-2-3 thành phần với `TEMPLATE_BUTTONS`.
  - `BatteryCanvasRenderer.kt`: Bộ vẽ Canvas tạo icon viên pin động (xanh/vàng/đỏ theo %) kèm tia sét sạc; tạo vòng tròn đỏ dấu X trắng (`createRedCrossCircleIcon`) và vòng tròn xanh dấu tích (`createGreenCheckCircleIcon`).
  - `DownloadCard.kt` & `QuickShareCard.kt`: Biểu tượng tải xuống ban đầu `ic_download` khi bắt đầu, logo app nguồn bên trái và vòng tròn tiến độ % camera bên phải khi tải; mở rộng có nút Tạm dừng / Hủy / Tiếp tục (`TEMPLATE_BUTTONS`); xong hiện viền sáng xanh lá + tích xanh; hủy hiện viền đỏ + vòng tròn dấu X đỏ tối giản chữ.
  - `RingerModeCard.kt`: Đón bắt `AudioManager.RINGER_MODE_CHANGED_ACTION`, hiển thị đảo thông báo khi đổi chế độ Chuông (vàng `#FCD535`), Rung (lam `#4A90E2`), Im lặng (đỏ `#FF5247`), tự đóng sau 2.5s.
  - `GenericCard.kt`, `ChargingCard.kt`, `PaymentCard.kt`, `OtpCard.kt`, `RideCard.kt`, `NavigationCard.kt`.
- `AppsScreen.kt` (`app/src/main/java/com/originisle/android/ui/AppsScreen.kt`):
  - `AppsScreen` (L52-175): Quản lý bật/tắt thông báo từng ứng dụng.
  - `AppIcon` (L213-223): Tải biểu tượng bất đồng bộ kèm bộ đệm cache.
  - `loadApps` (L225-241): Truy vấn ứng dụng cài đặt và ứng dụng hệ thống.

## 4. Lưu Ý Kỹ Thuật (Gotchas)
- **ApplicationId cố định**: Bắt buộc giữ `com.autonavi.minimap` trong `app/build.gradle.kts` để qua whitelist của Vivo Origin Island.
- **Tài nguyên Small Icon**: Mọi thông báo gửi tới Vivo SuperX phải sử dụng mipmap resource (`R.mipmap.ic_launcher_round`), không được dùng vector XML hoặc adaptive mipmap trong Compose painterResource.
- **Quy định xuất bản APK**: Luôn lưu APK thành phẩm trực tiếp tại thư mục gốc dự án: `OriginIsle-VN-v1.0.6-debug.apk`.

## 5. Việc Đang Mở (Lean Context)
- Toàn bộ tính năng đã được triển khai, kiểm thử đơn vị và biên dịch thành công 100%.
