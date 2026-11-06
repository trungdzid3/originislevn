# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.14` (mã bản dựng `versionCode = 10214`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.14 / 10214)
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
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (TEMPLATE_BASE + vòng tiến độ #2B66FF, 100% Bitmap Icon)
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Wearable (hiển thị "Đã ngắt" tối giản không khung xám)
│       │       │   ├── MediaCard.kt      # Sóng âm HSV rực rỡ, PAG wave cờ, fallback icon nốt nhạc chống trống, bo góc squircle bìa album
│       │       │   ├── OtpCard.kt        # Thẻ OTP chuẩn Ngân hàng (trái "OTP", phải mã OTP màu #00D084, chạm chép clipboard)
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, hiển thị Đang nhận tệp/kết nối mượt mà, chuyển tiếp Request -> Transfer -> Finished chuẩn xác
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms, loại bỏ internal intent gây nhảy Im lặng)
│       │       ├── download/         # Nhận diện tải xuống nhạy bén (chặn bắt nhầm Gmail/Email/Chat, chặn nhân bản Group Summary)
│       │       ├── island/           # PlaygroundService (chặn phục hồi thẻ 35009), builder, constants
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP Bank-Style (STRONG_OTP_MARKER, quét MessagingStyle/publicVersion)
│       │       ├── quickshare/       # Nhận diện Quick Share (loại bỏ phán đoán 100% khi max<=0, mở rộng từ khóa nhận tệp)
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener (xử lý onNotificationRemoved chuẩn, không gọi 100% khi remove request)
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
└── OriginIsle-VN-v1.2.14.apk         # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 14)
```

## 3. Bản Đồ Code (Code Map)
- `QuickShareDetector.kt` (`app/src/main/java/com/originisle/android/quickshare/QuickShareDetector.kt`): Hàm `parse` (khoảng dòng 173-380) xử lý bóc tách thông báo Quick Share, loại bỏ điều kiện tự nhận định 100% khi max<=0, mở rộng từ khóa nhận tệp và ràng buộc `isFinished = false` khi là request.
- `QuickShareCard.kt` (`app/src/main/java/com/originisle/android/cards/QuickShareCard.kt`): Hàm `post` (khoảng dòng 102-320) và `postFinished` (khoảng dòng 325-375) quản lý hiển thị thẻ Quick Share trên Đảo Origin, kiểm soát `isFinishedEffective` và nhãn "Đang nhận tệp" / "Đang kết nối".
- `DownloadDetector.kt` (`app/src/main/java/com/originisle/android/download/DownloadDetector.kt`): Hàm `isDownload` (khoảng dòng 120-250) loại trừ `NON_DOWNLOAD_APPS` (Gmail, Outlook, Messenger, Zalo,...) và chặn thông báo nhóm `FLAG_GROUP_SUMMARY` tránh nhân bản 2 thẻ tải xuống.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`): `onNotificationRemoved` (khoảng dòng 1510-1550) chỉ gọi `postFinished` khi thực sự hoàn tất (không phải request, progress >= 80% hoặc isFinished).
- `RingerModeCard.kt` (`app/src/main/java/com/originisle/android/cards/RingerModeCard.kt`): Xử lý bộ lọc chuông/rung (lines 60-170), debounce 80ms, loại bỏ `INTERNAL_RINGER_MODE_CHANGED_ACTION`.
- `MediaCard.kt` (`app/src/main/java/com/originisle/android/cards/MediaCard.kt`): Hàm `postMedia` (lines 80-260) cấu hình SuperX Media.

## 4. Những Điểm Cần Chú Ý Kỹ Thuật (Gotchas)
- `applicationId = "com.autonavi.minimap"`: Tuyệt đối giữ nguyên để vượt whitelist SuperX trên Vivo OriginOS 6.
- `QuickShare ID = 35009`: Định danh duy nhất của Quick Share, chỉ hoàn tất khi tiến trình thực sự đạt 100% hoặc có từ khóa hoàn tất xác thực.
- `FLAG_GROUP_SUMMARY`: Phải luôn bỏ qua ở các bộ lọc Live card (như DownloadCard) để tránh hiện tượng nhân đôi 2 thẻ cùng lúc.
