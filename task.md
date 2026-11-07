# Danh sách Công việc (Task Matrix) — OriginIsle-VN v1.4.0

## Hợp đồng Dữ liệu & Giao diện Đã Chốt (Contract Freeze Gate)
1. **Nhận diện Thông báo Chuỗi TikTok (`TikTokStreakDetector.kt`)**:
   - Gói TikTok: `com.ss.android.ugc.trill`, `com.zhiliaoapp.musically`, `com.ss.android.ugc.aweme`.
   - Bắt kênh `dm_streaks_push`, `enable_streak_notification`, `streak_remind`, hoặc tiêu đề/nội dung chứa: `"chuỗi"`, `"streak"`, `"lửa"`, `"ngọn lửa"`, biểu tượng emoji lửa 🔥.
   - Trích xuất: Tên người gửi/bạn bè chuỗi, nội dung nhắc nhở, số ngày chuỗi (nếu có), `PendingIntent` mở cuộc trò chuyện.
2. **Thẻ Đảo Động Chuỗi TikTok (`TikTokStreakCard.kt`)**:
   - `ID = 35012`
   - Giao diện: Icon ngọn lửa 🔥 (`ic_tiktok_streak_fire.xml`), nền hiệu ứng gradient cam lửa `#FF5E00` -> `#FF9900`.
   - Nút hành động "Duy trì chuỗi": Bấm vào kích hoạt `TikTokStreakActionReceiver` copy tin nhắn tùy chỉnh vào Clipboard và kích hoạt mở thẳng cuộc trò chuyện TikTok.
3. **Cấu hình & Tùy biến (`HomeScreen.kt`, `ActivityScreen.kt`, `IslandSamples.kt`)**:
   - Khóa SharedPreferences: `cast_tiktok_streak` (mặc định `true`), `tiktok_streak_custom_message` (mặc định `"🔥"`).
   - Thêm ô nhập nội dung tin nhắn tùy chỉnh trong giao diện Cài đặt.
   - Thêm mẫu thử "Chuỗi TikTok" trên màn hình Thử nghiệm.

## Phân quyền Subagent & Khóa File (Locked Files)
- [x] Task 1: Xây dựng `TikTokStreakDetector.kt` và `TikTokStreakCard.kt` (`ID = 35012`), đăng ký `TikTokStreakActionReceiver` trong `AndroidManifest.xml` | [Agent: Subagent-TikTokStreak-Core] | [Skill: reverse-engineer-anything] | [Locked Files: app/src/main/java/com/originisle/android/tiktok/TikTokStreakDetector.kt, app/src/main/java/com/originisle/android/cards/TikTokStreakCard.kt, app/src/main/java/com/originisle/android/tiktok/TikTokStreakActionReceiver.kt, app/src/main/AndroidManifest.xml]
- [x] Task 2: Tích hợp `TikTokStreakDetector` vào `NotificationCastListener.kt` (ưu tiên cao, hiển thị thẻ Đảo `ID = 35012`) và bổ sung giao diện cấu hình/mẫu thử trong `HomeScreen.kt`, `ActivityScreen.kt`, `IslandSamples.kt` | [Agent: Subagent-TikTokStreak-UIIntegration] | [Skill: minimalist-ui] | [Locked Files: app/src/main/java/com/originisle/android/service/NotificationCastListener.kt, app/src/main/java/com/originisle/android/ui/HomeScreen.kt, app/src/main/java/com/originisle/android/ui/ActivityScreen.kt, app/src/main/java/com/originisle/android/ui/samples/IslandSamples.kt]
- [ ] Task 3: Nâng phiên bản `1.4.0` (`versionCode = 10400`), chạy Unit Tests, đóng gói `OriginIsle-VN-v1.4.0.apk`, cập nhật `PROJECT_CONTEXT.md` và đồng bộ Git | [Agent: Lead Orchestrator] | [Skill: pr-feedback-quality-gate] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md]
