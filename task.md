# KẾ HOẠCH NÂNG CẤP PHIÊN BẢN CHUYÊN NGHIỆP V1.1.0 & KHẮC PHỤC TRIỆT ĐỂ TIẾN ĐỘ TẢI XUỐNG + CHẾ ĐỘ CHUÔNG/RUNG/DND

[CONTRACT LOCKED]
1. Sửa lỗi tiến độ tải xuống & Đa nhiệm an toàn:
   - Khôi phục `oi_scene = "NAVIGATION"` trong `DownloadCard.kt` và `MediaCard.kt` (đồng bộ với `NotificationCastListener.kt`), thêm cơ chế `resolveAllowedScene` trong `PlaygroundService.kt` kiểm tra `NotificationManager.getSceneStatus` trước khi dùng scene khác.
   - Trong `DownloadDetector.kt`: Giữ bộ lọc loại trừ nghiêm ngặt cho Media, Bản đồ (`NAV_PACKAGES`, `CATEGORY_NAVIGATION`), Gọi xe, Thiết bị đeo/Hệ thống; sau bước loại trừ, khôi phục nhận diện nhạy bén với mọi thông báo có `max > 0 || indeterminate` hoặc `CATEGORY_PROGRESS`.
2. Sửa dứt điểm nhận diện đổi chế độ Chuông / Rung / Im lặng / Không làm phiền (DND):
   - Tách riêng 3 biến trạng thái trong `RingerModeCard.kt`: `lastRingerMode`, `lastVibrateSetting`, `lastDndState`. Không dùng `isDndActive` để chặn sự kiện đổi `ringerMode`.
   - Bổ sung `RingerModeCard.poll(context)` và gọi mỗi giây trong `NotificationCastListener.pollRunnable`.
   - Chuẩn hóa payload `showRingerCard` đồng bộ với `ChargingCard.kt` (`oi_left_content = ""`, `oi_show_right_icon = false`, nền `#FF1C1C1E`, chữ `#FFFFFFFF`, bỏ `oi_keep_duration`, ghi log vào `CastLog`).
3. Báo pin thiết bị đeo Galaxy Fit 3:
   - Lưu đệm pin bền vững `wearable_battery_cache` và quét `activeNotifications` khi thiết bị kết nối.
4. Chuẩn hóa phiên bản chuyên nghiệp:
   - Nâng `versionName = "1.1.0"`, `versionCode = 10100` trong `app/build.gradle.kts`.
   - Xuất bản tệp APK chuẩn: `OriginIsle-VN-v1.1.0.apk` (loại bỏ hậu tố debug và dọn file debug cũ).

[SUBAGENT TASKS]
- [x] Task 1: Nâng cấp bộ nhớ đệm và cơ chế quét pin thông minh cho Galaxy Fit 3 / Wearable trong EarbudsCard và NotificationCastListener | [Agent: Subagent-WearableBatteryExpert] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/EarbudsCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]
- [x] Task 2: Sửa lỗi Tiến độ tải xuống và bảo vệ scene đa nhiệm trong DownloadDetector, DownloadCard, MediaCard, PlaygroundService | [Agent: Subagent-DownloadFix] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/download/DownloadDetector.kt, app/src/main/java/com/originisle/android/cards/DownloadCard.kt, app/src/main/java/com/originisle/android/cards/MediaCard.kt, app/src/main/java/com/originisle/android/island/PlaygroundService.kt]
- [x] Task 3: Sửa dứt điểm nhận diện Chuông / Rung / Im lặng / DND trong RingerModeCard và tích hợp polling trong NotificationCastListener | [Agent: Subagent-RingerFix] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/RingerModeCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]
- [x] Task 4: Nâng phiên bản v1.1.0, biên dịch đóng gói APK chuyên nghiệp OriginIsle-VN-v1.1.0.apk, cập nhật PROJECT_CONTEXT.md và đồng bộ Git | [Agent: Lead-Orchestrator] | [Skill: android-cli] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]
