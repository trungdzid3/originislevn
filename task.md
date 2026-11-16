# Kế Hoạch Điều Phối Swarm — Origin Isle VN v2.0.3 (Sửa Lỗi Báo Pin Cũ Lần Trước & Hỗ Trợ Xiaomi Earbuds)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **Chấm dứt hoàn toàn việc báo pin của lần mở trước (`EarbudsCard.kt`)**:
   - Tạo `clearCachedTelemetry(context: Context, nameOrAddress: String?)` xóa cả RAM (`deviceTelemetry`) lẫn `SharedPreferences("wearable_battery_cache")` (`telemetry_${key}_*` và key trực tiếp) khi thiết bị ngắt kết nối (`ACTION_ACL_DISCONNECTED`, `isProfileDisconnected`, `onDeviceDisconnected`, `onFastPairNotificationRemoved`).
   - Khi tai nghe mới kết nối (`ACTION_ACL_CONNECTED` / `isProfileConnected`, khi `!wasAlreadyConnected`):
     + **TUYỆT ĐỐI KHÔNG** lấy `getStoredTelemetry` / `getCachedBattery` từ lần mở trước để `show(...)` ngay!
     + Chỉ đọc nguồn dữ liệu **hiện tại trực tiếp** (`readTelemetry(device, intent)` + `scanActiveNotificationsForEarbuds(context, devName)`).
     + Nếu chưa có pin hiện tại thật, chờ trong `BATTERY_WAIT_MS = 2500L` (`pending[address]`) để đón gói tin Fast Pair / HFP / Metadata mới nhất thay vì hiện pin cũ.
   - Trong `tryParseBatteryFromNotification` và `handle(BATTERY_LEVEL_CHANGED)`: nếu trong vòng 15 giây đầu vừa kết nối (`isWithinAntiFlapCooldown`) mà nhận được thông số pin thật khác với thông số đang có (`changed == true`), luôn cho phép `show(...)` cập nhật lên Đảo ngay lập tức.
2. **Hỗ trợ thêm `com.mi.earphone` (Xiaomi Earbuds)**:
   - Bổ sung `"com.mi.earphone"` vào `TWS_NOTIFICATION_PACKAGES` (`EarbudsCard.kt`) và `pollRunnable` (`NotificationCastListener.kt`).

## Ma Trận Phân Quyền Swarm (Coordinator: Gemini 4 Argon | Workers: Gemini 4 Hydrogen)
- [x] Task 1: Sửa `EarbudsCard.kt` (thêm `clearCachedTelemetry`, không đọc cache pin cũ khi kết nối mới, đợi 2.5s nhận pin thật, cập nhật ngay khi Fast Pair báo pin mới trong 15s đầu, thêm `"com.mi.earphone"`) | [Agent: earbuds-fresh-battery (`gemini-4-hydrogen-medium`)] | [Skill: impeccable] | [Locked Files: `app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`]
- [x] Task 2: Bổ sung `"com.mi.earphone"` vào `pollRunnable` của `NotificationCastListener.kt` để quét chủ động thông báo pin Xiaomi Earbuds | [Agent: notif-xiaomi-tws (`gemini-4-hydrogen-medium`)] | [Skill: impeccable] | [Locked Files: `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`]
- [x] Task 3: Tích hợp 2 nhánh share, nâng version `2.0.3` (`20003`) trong `app/build.gradle.kts`, chạy `./gradlew.bat testDebugUnitTest assembleRelease`, xuất `release/OriginIsle-VN-v2.0.3.apk`, cập nhật `PROJECT_CONTEXT.md` & `task.md`, push GitHub & phát hành Release `v2.0.3` | [Agent: verifier-v203 (`gemini-4-hydrogen-medium`)] | [Skill: pr-feedback-quality-gate] | [Locked Files: `app/build.gradle.kts`, `PROJECT_CONTEXT.md`, `task.md`, `release/OriginIsle-VN-v2.0.3.apk`]
