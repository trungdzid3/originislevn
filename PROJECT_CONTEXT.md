# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.5` (mã bản dựng `versionCode = 10205`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.5 / 10205)
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
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Wearable (hiển thị "Đã ngắt" tối giản không icon X đỏ)
│       │       │   ├── MediaCard.kt      # Nhạc sóng động & cơ chế nhường Đảo gốc OriginOS 6 (Zero-Overlap)
│       │       │   ├── OtpCard.kt        # Thẻ OTP tối ưu (trái "OTP", phải "$code · Chép", chạm chép clipboard)
│       │       │   ├── QuickShareCard.kt # Quick Share logo chính thức từ file SVG tải về
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chế độ Chuông / Rung / Im lặng / DND độc lập (showNotify=true)
│       │       ├── download/         # Nhận diện tải xuống nhạy bén
│       │       ├── island/           # Service SuperX, builder, constants, resolveAllowedScene
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP MessagingStyle (EXTRA_MESSAGES), Action Title, regex (OTP)
│       │       ├── quickshare/       # Nhận diện Quick Share & EasyShare & actions
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener & Receivers & KeepAliveAccessibilityService
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
└── OriginIsle-VN-v1.2.5.apk          # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 5)
```

## 3. Bản Đồ Code (Code Map)
- `VietnamBankDetector.kt` (`app/src/main/java/com/originisle/android/bank/VietnamBankDetector.kt`):
  - `hasPromoLimitPrefix`: Nhận diện các tiền tố hạn mức tiếp thị (`lên tới`, `lên đến`, `tối đa`, `hạn mức`, `tới`, `đến`...) ngay trước số tiền để loại bỏ hoàn toàn các thông báo quảng cáo/hạn mức vay (ZaloPay, MBBank...).
  - Mở rộng kiểm tra `hasSpamPromoKeywords` cho cả ứng dụng ngân hàng gốc (Native Banking Apps) và tin nhắn SMS.
  - `hasClearBalanceFluctuationSigns`: Bắt buộc kiểm tra chỉ dấu biến động số dư thực tế trước khi xác nhận giao dịch.
  - Loại bỏ fallback tự gán dấu trừ `-` khi thiếu từ khóa giao dịch.
- `VietnamBankDetectorTest.kt` (`app/src/test/java/com/originisle/android/bank/VietnamBankDetectorTest.kt`):
  - 16 trường hợp kiểm thử bao phủ toàn diện: quảng cáo ZaloPay/MBBank/VPBank (trả về null), giao dịch chuyển tiền/thanh toán thật (nhận diện chính xác).
- `EarbudsCard.kt` (`app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`):
  - `showDisconnected`: Hiển thị chữ `Đã ngắt` tối giản, loại bỏ hoàn toàn `redCrossIcon` và `oi_icon_status_type`.
- `VietnamRideDetector.kt` (`app/src/main/java/com/originisle/android/ride/VietnamRideDetector.kt`):
  - Dữ liệu reverse engineering thực tế từ 4 file XAPK (Grab, Be, Xanh SM, ShopeeFood) với từ vựng tiếng Việt gốc và trích xuất nhà hàng/tài xế.

## 4. Lưu Ý Kỹ Thuật (Gotchas)
- **ApplicationId cố định**: Bắt buộc giữ `com.autonavi.minimap` trong `app/build.gradle.kts` để qua whitelist của Vivo Origin Island.
- **Tài nguyên Small Icon**: Mọi thông báo gửi tới Vivo SuperX phải sử dụng mipmap resource (`R.mipmap.ic_launcher_round`), không được dùng vector XML.
- **Quy định xuất bản APK**: File APK thành phẩm lưu trực tiếp tại thư mục gốc dự án: `OriginIsle-VN-v1.2.5.apk`.

## 5. Việc Đang Mở (Lean Context)
- Không có lỗi tồn đọng. Bản dựng `v1.2.5` đã vượt qua 100% kiểm thử đơn vị và biên dịch thành công.
