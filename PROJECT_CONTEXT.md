# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: `1.5.0` (mã bản dựng `versionCode = 10500`, chuẩn `ThếHệ.NângCấp.VáLỗi` tức `MAJOR.MINOR.PATCH`).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đồng bộ hoàn toàn lên `v2.2.3` / commit `73abb82`).
- **Kho lưu trữ GitHub**: `https://github.com/trungdzid3/originislevn.git` (`auto_github_sync: true`).

## 2. Cây Cấu Trúc Thư Mục Chuẩn
```text
origin-isle-vn/
├── .agent/
│   ├── GEMINI.md                     # Quy tắc toàn cục Antigravity (UTF-8 chuẩn)
│   └── rules.md                      # Rule cục bộ (auto_github_sync: true)
├── app/
│   ├── build.gradle.kts              # Cấu hình bản dựng Android & dependencies (v1.5.0 / 10500)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── res/
│       │   │   ├── drawable/         # ic_success_tick_green.xml, ic_tiktok_streak_fire.xml, ic_media_heart.xml, ic_quick_share.xml...
│       │   │   └── xml/keepalive_accessibility_config.xml
│       │   └── java/com/originisle/android/
│       │       ├── bank/             # Bóc tách biến động số dư Ngân hàng VN & SMS
│       │       ├── call/             # MessengerCallDetector.kt, ZaloCallDetector.kt (bóc tách chuyên sâu cuộc gọi & CallStyle)
│       │       ├── cards/            # Các loại thẻ giao diện Đảo Origin
│       │       │   ├── BatteryCanvasRenderer.kt # Bộ vẽ Canvas tạo icon viên pin động & badge X đỏ
│       │       │   ├── DownloadCard.kt   # Tiến độ tải xuống (thay "100%" bằng icon tích xanh lá ic_success_tick_green khi hoàn tất)
│       │       │   ├── EarbudsCard.kt    # Tai nghe & Wearable (hiển thị "Đã ngắt" tối giản không khung xám)
│       │       │   ├── MediaCard.kt      # Cố định ID = 35010, oi_scene = "NAVIGATION", 3 nút chuẩn SuperX, hỗ trợ postFromController cho Zing MP3 / NCT
│       │       │   ├── MessengerCallCard.kt # Cố định ID = 35013, hiển thị cuộc gọi Messenger (avatar tròn kèm badge Messenger, nút Từ chối / Trả lời / Kết thúc)
│       │       │   ├── PaymentCard.kt    # Chuẩn hóa giao diện mở rộng: xóa hoàn toàn khối chữ xám to đùng "Tiền ra / Tiền vào", hiển thị tiền gọn gàng sắc nét
│       │       │   ├── QuickShareCard.kt # Cố định ID = 35009, chuyển trạng thái hoàn tất sang icon tích xanh lá ic_success_tick_green
│       │       │   ├── RideCard.kt       # Tiến trình Gọi xe & Giao đồ ăn VN (Template 2)
│       │       │   ├── RingerModeCard.kt # Chuyển đổi siêu mượt (debounce 80ms)
│       │       │   ├── TikTokStreakCard.kt # Cố định ID = 35012, hiển thị thông báo nhắc chuỗi TikTok ngọn lửa rực rỡ & nút 1 chạm duy trì chuỗi
│       │       │   └── ZaloCallCard.kt   # Cố định ID = 35011, hiển thị cuộc gọi Zalo (avatar bo tròn + nút Từ chối / Trả lời / Kết thúc)
│       │       ├── download/         # DownloadDetector chặn triệt để 100% Quick Share, Google Play Services & app nhạc
│       │       ├── island/           # PlaygroundService, OriginIslandBuilder (BASE_SUB_INFO = 0 khi subText rỗng loại bỏ khối chữ xám to đùng)
│       │       ├── log/              # Nhật ký hoạt động bền vững (CastLog)
│       │       ├── quickshare/       # Nhận diện Quick Share
│       │       ├── ride/             # Nhận diện chuyến đi gọi xe & giao đồ ăn VN
│       │       ├── service/          # NotificationCastListener (ưu tiên Zalo & Messenger Call -> Ngân hàng -> Quick Share -> Download)
│       │       ├── smart/            # Bóc tách thông tin màn hình
│       │       ├── sports/           # Tỷ số thể thao trực tiếp
│       │       ├── tiktok/           # TikTokStreakDetector.kt & TikTokStreakActionReceiver.kt
│       │       └── ui/               # Giao diện Compose (công tắc Messenger Call, Zalo Call, TikTok Streak, nút Thử Gọi Messenger)
│       └── test/                     # Unit Tests (VietnamBankDetectorTest, QuietHoursTest)
├── backups/                          # Thư mục sao lưu an toàn khi đại tu
├── docs/
├── gradle/                           # Gradle wrapper & version catalogs
├── DESIGN.md                         # Nguồn chân lý thiết kế giao diện (SSOT)
├── PROJECT_CONTEXT.md                # Lưu trữ ngữ cảnh tinh gọn (Lean Context)
├── task.md                           # Phân quyền công việc & Locked Files
├── origin-isle-vn.code-workspace     # Cấu hình workspace
└── OriginIsle-VN-v1.5.0.apk          # File APK xuất bản chuẩn chuyên nghiệp (Thế hệ 1, Nâng cấp 5, Bản vá 0)
```

## 3. Bản Đồ Code (Code Map)
- `MessengerCallDetector.kt` (`app/src/main/java/com/originisle/android/call/MessengerCallDetector.kt`): Nhận diện cuộc gọi Messenger (`com.facebook.orca`), bắt `CallStyle` (`CATEGORY_CALL`), trích xuất tên người gọi, avatar (`largeIcon`), phân biệt cuộc gọi đến (`incoming`) và đang đàm thoại (`ongoing`), lấy `actionIntent` của Trả lời (`answer`) và Từ chối / Kết thúc (`decline` / `end`).
- `MessengerCallCard.kt` (`app/src/main/java/com/originisle/android/cards/MessengerCallCard.kt`): Cố định `ID = 35013`, vẽ avatar tròn kèm huy hiệu Messenger ở góc dưới phải, hiển thị nút Trả lời/Từ chối hoặc Kết thúc/Mở app, hỗ trợ `postSample(context)`.
- `OriginIslandBuilder.kt` (`app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt`): Sửa nhánh `template == 4` chỉ đặt `BASE_SUB_INFO = 1` khi `subText` thực sự có nội dung; nếu rỗng thì đặt `BASE_SUB_INFO = 0` (loại bỏ hoàn toàn khối chữ xám to thô kệch bên phải thẻ mở rộng).
- `PaymentCard.kt` (`app/src/main/java/com/originisle/android/cards/PaymentCard.kt`): Bỏ chuỗi `subtext = "Tiền vào · ..."` / `"Tiền ra · ..."`, hiển thị số tiền to rõ nét, bố cục thẻ mở rộng chuẩn phong cách OriginOS 6.
- `QuickShareCard.kt` & `DownloadCard.kt`: Khi tải/truyền xong, hiển thị icon dấu tích tròn xanh lá `ic_success_tick_green.xml` thay cho chuỗi ký tự thô `"100%"` / `"Xong"`.
- `NotificationCastListener.kt` & `MediaCard.kt`: Sửa triệt để luồng Zing MP3 và NhacCuaTui: không drop notification khi thiếu token, truy vấn `findMediaControllerFallback` khi đang phát hoặc ongoing, bypass kiểm tra `isOriginNativePlayerActive` cho toàn bộ ứng dụng trong `THIRD_PARTY_MUSIC_PACKAGES`.
- `HomeScreen.kt` & `ActivityScreen.kt`: Thêm công tắc `cast_messenger_call` và nút thử nghiệm `Thử Gọi Messenger`.

## 4. Những Điểm Cần Chú Ý Kỹ Thuật (Gotchas)
- Khối chữ xám to trong thẻ mở rộng: Bắt nguồn từ `BUNDLE_KEY_BASE_SUB_INFO = 1` của SuperX Bundle khiến hệ thống OriginOS render một sub-widget chữ xám lớn chiếm nửa phải thẻ mở rộng. Chỉ cần đặt `BASE_SUB_INFO = 0` khi `subText.isNullOrBlank()` là giao diện thẻ mở rộng trở nên sạch sẽ, cân đối 100%.
- Zing MP3 Notification vs Service: Zing MP3 không luôn gắn `EXTRA_MEDIA_SESSION` vào notification `extras`. Phải luôn kích hoạt fallback qua `MediaSessionManager` / `findMediaControllerFallback` trước khi kết luận không có playback.
