# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.2.13` (mã bản dựng `versionCode = 10213`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
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
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.2.13 / 10213)
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
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, hiển thị Đang truyền cho indeterminate, hoàn tất chuẩn xác
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   └── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms, loại bỏ internal intent gây nhảy Im lặng)
│       │       ├── download/         # Nhận diện tải xuống nhạy bén (ưu tiên chạy trước OTP, có poll mỗi 1 giây)
│       │       ├── island/           # PlaygroundService (chặn phục hồi thẻ 35009), builder, constants
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── otp/              # Nhận diện OTP Bank-Style (STRONG_OTP_MARKER, quét MessagingStyle/publicVersion)
│       │       ├── quickshare/       # Nhận diện Quick Share (lọc idle/visibility, mở rộng isFinished)
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN (Grab, Be, Xanh SM, ShopeeFood)
│       │       ├── service/          # NotificationCastListener (loại bỏ poll rung 1s gây xung đột, bảo vệ Zing MP3)
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
└── OriginIsle-VN-v1.2.13.apk         # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 2, Bản vá 13)
```

## 3. Bản Đồ Code (Code Map)
- `RingerModeCard.kt` (`app/src/main/java/com/originisle/android/cards/RingerModeCard.kt`):
  - Xóa bỏ triệt để `"android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION"` (nguyên nhân cốt lõi khiến hệ thống gửi intent nội bộ ringer mode = 0 ngay sau khi bật Chuông hoặc Rung, làm thẻ bị nhảy ngay lập tức sang Im lặng).
  - Xóa bỏ ContentObserver trên `mode_ringer` và `zen_mode` gây xung đột race condition; loại bỏ việc poll rung định kỳ mỗi 1s từ `NotificationCastListener.kt`.
  - Giảm `DEBOUNCE_DELAY_MS` từ 200ms xuống **80ms**: phản hồi cực nhanh khi người dùng gạt/chuyển chế độ liên tiếp, đồng thời gom gọn các broadcast trùng lặp từ phần cứng mà không gây loạn trạng thái hay chớp giật giao diện.
  - Quản lý trạng thái thông minh qua `lastShownMode` và `isShowing`: nếu chế độ mục tiêu đang hiển thị thì chỉ làm mới bộ đếm tự đóng 2.8s, không gửi lại notification lặp.
  - Biểu tượng Rung: vẽ thân điện thoại đứng thẳng 100% ở trung tâm và 2 cặp sóng cánh cung cong mượt mà đối xứng trên nền trong suốt (không khung mờ).
- `MediaCard.kt` (`app/src/main/java/com/originisle/android/cards/MediaCard.kt`):
  - Khắc phục triệt để khoảng trống bên phải viên nang (Right Island): cung cấp `oi_right_template = TEMPLATE_RIGHT_ISLAND_WAVE` (1), `oi_wave_state = 1`, mảng màu `oi_wave_color_list` sáng rõ (`HSV` với `V >= 0.85f`), đính kèm `oi_right_icon` và `status_chip_text` làm fallback kép chống trống lỗ khuyết camera nếu OriginOS không kích hoạt PAG wave.
  - Phía bên trái viên nang (Left Island): bo góc squircle 0.22f cho ảnh bìa album, đặt `oi_left_content = ""` để ảnh bìa chiếm trọn vẹn vùng bên trái lỗ camera sắc nét như ảnh mẫu Vivo OriginOS 6.
  - Thẻ mở rộng (Expanded Card): tối ưu `subtext` hiển thị `Nghệ sĩ • mm:ss / mm:ss` khi có tiến trình phát hoặc `Nghệ sĩ • Đang phát`.
  - Làm mịn logic `isOriginNativePlayerActive`: chỉ nhường quyền khi widget gốc `com.vivo.musicwidgetmix` đang hoạt động thực sự, bảo vệ tuyệt đối các app Zing MP3, Nhaccuatui, Spotify, YouTube Music.
- `OriginIslandBuilder.kt` (`app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt`):
  - Bổ sung cờ `BUNDLE_KEY_ISLAND_RIGHT_PLAY_PAG = true` cho nhánh `TEMPLATE_RIGHT_ISLAND_WAVE` (1) để kích hoạt engine animation PAG trên OriginOS 6.
  - Tích hợp fallback nạp `rightIcon` và `rightContent` (colorSpan) đảm bảo viên nang bên phải luôn có hình/chữ hiển thị nếu không kích hoạt được sóng âm.
- `NotificationCastListener.kt` (`app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`):
  - Thêm nhận diện mở rộng các app nhạc VN/quốc tế (Zing MP3, Nhaccuatui, Spotify, YT Music, Soundcloud) và trích xuất an toàn token MediaSessionCompat.
  - Cải tiến `findMediaControllerFallback` hỗ trợ tìm kiếm theo gói nhạc và cơ chế retry bất đồng bộ 300ms chống rơi thông báo.
  - Bổ sung `isOriginNativePlayerActive` ưu tiên giữ thẻ nhạc bên thứ ba, chỉ nhường quyền khi gói nhạc hệ thống Vivo thực sự phát nhạc.
  - Xử lý `onNotificationRemoved` gọi `MediaCard.clear` an toàn khi Zing MP3/app nhạc dừng phát hoặc gỡ thông báo.
- `QuickShareDetector.kt`, `QuickShareCard.kt`, `NotificationCastListener.kt` & `PlaygroundService.kt`:
  - Lọc bỏ triệt để thông báo idle hiển thị nền của Google Quick Share / Nearby Share (`isIdleVisibilityNotification`).
  - Mở rộng nhận diện `isFinished = true` qua action (Mở/Xem/Chia sẻ/Xong/Đóng) và khi không còn progress/từ khóa đang truyền.
  - Cố định `ID = 35009` cho mọi giai đoạn Quick Share, hiển thị "Đang truyền" cho tiến trình indeterminate (không kẹt 0%), thêm `otherId != 35009` vào bộ lọc `resumeId` của `PlaygroundService`.
  - Quét định kỳ mỗi 1 giây trong `pollRunnable` và tự động phát `postFinished` khi hệ thống gỡ bỏ thông báo truyền tệp.

## 4. Lưu Ý Kỹ Thuật (Gotchas)
- **ApplicationId cố định**: Bắt buộc giữ `com.autonavi.minimap` trong `app/build.gradle.kts` để qua whitelist của Vivo Origin Island.
- **Tài nguyên Icon SuperX**: Mọi `Icon` đặt trong `Bundle` SuperX phải là `Icon.createWithBitmap` (`TYPE_BITMAP`), còn `NotificationCompat.Builder.setSmallIcon` dùng `R.mipmap.ic_launcher_round`.
- **Cờ `oi_show_notify`**: Tuyệt đối không đặt `oi_show_notify = false` cho các thẻ cần hiển thị viên nang trên OriginOS 6 vì gói `com.autonavi.minimap` sẽ bị Vivo SystemUI chặn hiển thị.
- **Quy định xuất bản APK**: File APK thành phẩm lưu trực tiếp tại thư mục gốc dự án: `OriginIsle-VN-v1.2.13.apk`.

## 5. Việc Đang Mở (Lean Context)
- Không có lỗi tồn đọng. Bản dựng `v1.2.13` đã vượt qua 100% kiểm thử đơn vị và biên dịch thành công.
