# DANH SÁCH CÔNG VIỆC (TASK MATRIX) - BẢN NÂNG CẤP v1.2.0

[CONTRACT LOCKED]
- Phiên bản mục tiêu: `v1.2.0` (`versionCode = 10200`, `versionName = "1.2.0"`).
- Không để 2 Subagent chỉnh sửa chung một file tại cùng một thời điểm.

## Ma trận Phân quyền Subagent

- [x] **Task 1: Chuẩn hóa Phát nhạc Đảo gốc OriginOS 6 (chống đè 2 thẻ nhạc), sửa dứt điểm lỗi thẻ Chuông/Rung/Im lặng và lỗi OTP SMS tiếng Việt không dấu**
  - `[Agent: Subagent-Media-Audio-OTP]`
  - `[Skill: android-system-integration]`
  - `[Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt, app/src/main/java/com/originisle/android/cards/RingerModeCard.kt, app/src/main/java/com/originisle/android/otp/OtpDetector.kt, app/src/main/java/com/originisle/android/cards/OtpCard.kt]`

- [x] **Task 2: Sửa dứt điểm lỗi bung thẻ Đồng hồ khi vuốt YouTube Music xuống đa nhiệm & Triển khai đa kênh đọc % pin Samsung Fit 3 trên Vivo**
  - `[Agent: Subagent-Wearable-Listener]`
  - `[Skill: reverse-engineer-anything]`
  - `[Locked Files: app/src/main/java/com/originisle/android/cards/EarbudsCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt, app/src/main/java/com/originisle/android/service/KeepAliveAccessibilityService.kt, app/src/main/res/xml/keepalive_accessibility_config.xml]`

- [x] **Task 3: Nâng cấp bộ nhận diện & thẻ tiến trình Gọi xe - Giao đồ ăn tại Việt Nam (Grab, Be, Xanh SM, ShopeeFood)**
  - `[Agent: Subagent-Vietnam-Ride-Food]`
  - `[Skill: android-live-updates]`
  - `[Locked Files: app/src/main/java/com/originisle/android/ride/VietnamRideDetector.kt, app/src/main/java/com/originisle/android/cards/RideCard.kt]`

- [x] **Task 4: Đại tu toàn diện giao diện ứng dụng chuẩn Cài đặt gốc Vivo OriginOS 6 (Font hệ thống, tỷ lệ hiển thị, khung cài đặt lồng nhau chuyên nghiệp)**
  - `[Agent: Subagent-Vivo-UI-Overhaul]`
  - `[Skill: minimalist-ui]`
  - `[Locked Files: app/src/main/java/com/originisle/android/ui/Theme.kt, app/src/main/java/com/originisle/android/ui/Components.kt, app/src/main/java/com/originisle/android/ui/HomeScreen.kt, DESIGN.md]`

- [x] **Task 5: Kiểm tra cú pháp, nâng cấp phiên bản v1.2.0, đóng gói APK và đồng bộ Git**
  - `[Agent: Lead-Orchestrator]`
  - `[Skill: pr-feedback-quality-gate]`
  - `[Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md]`
