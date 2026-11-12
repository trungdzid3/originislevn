# Kế Hoạch Điều Phối Swarm — Origin Isle VN v2.0.1 (Chặn Triệt Để Pin Ảo Đồng Hồ 3% & Chỉ Báo Khi Kết Nối)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **Nguyên nhân gây lỗi báo pin ảo đồng hồ `3%`**:
   - `BatteryManager.BATTERY_STATUS_DISCHARGING = 3` bị đọc nhầm từ key `"battery_status"` / `"value"` trong `NotificationCastListener.extractDeepWearableBattery` và `EarbudsCard.extractSamsungBattery`.
   - Regex `(?:...|galaxy\s*fit|...)\s*[:：·\-]?\s*(\d{1,3})\s*%?` có `%?` không bắt buộc nên bóc luôn số `3` trong tên `"Galaxy Fit 3"` thành `3%`.
   - `EarbudsCard.queryProviderForBattery` có vòng lặp mù `for (i in 0 until c.columnCount)` đọc nhầm cột `_id = 3` hoặc `status = 3` trong bảng SQLite.
   - `onBatteryUpdated`, `tryParseBatteryFromNotification` và `NotificationCastListener` tự động bật Đảo khi thấy `isNewLowWarning` (`3% <= 15%`) hoặc thông báo nền dù đồng hồ không hề có sự kiện kết nối mới.
2. **Quy tắc Hiển thị Bắt buộc**:
   - Chỉ hiển thị Đảo cho tai nghe / đồng hồ **KHI VÀ CHỈ KHI vừa kết nối (hoặc vừa ngắt kết nối) phần cứng Bluetooth thực sự**. Không bao giờ tự bật Đảo từ thông báo nền, `ContentObserver`, hay cảnh báo pin yếu ngầm khi thiết bị không vừa kết nối.

## Ma Trận Phân Quyền Swarm (Coordinator: Argon | Workers: Gemini 3.8 Flash)
- [x] Task 1: Xóa bỏ việc đọc key `"battery_status"`/`"value"`, xóa vòng lặp mù cột SQLite trong `queryProviderForBattery`, dọn cache `wearable_battery_cache` bị dính số `3%` cũ khi `start()`, và khóa chặt `EarbudsCard` chỉ hiện thẻ khi có sự kiện kết nối/ngắt kết nối thật (`isDeviceHardwareConnected`) | [Agent: earbuds-guard (`gemini-3.8-flash-medium`)] | [Skill: impeccable] | [Locked Files: `app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`]
- [x] Task 2: Sửa `extractDeepWearableBattery` trong `NotificationCastListener.kt` và `KeepAliveAccessibilityService.kt` (bỏ `"battery_status"`/`"value"`, bắt buộc có `%`, loại trừ tên `"Galaxy Fit 3"` khỏi regex số, bỏ nhánh tự gọi `onDeviceConnected` khi không có sự kiện `isConnect` + kết nối phần cứng thật) | [Agent: notif-wearable-guard (`gemini-3.8-flash-medium`)] | [Skill: impeccable] | [Locked Files: `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`, `app/src/main/java/com/originisle/android/service/KeepAliveAccessibilityService.kt`]
- [x] Task 3: Tích hợp 2 nhánh share, thêm unit test kiểm chứng `"Galaxy Fit 3"` và `battery_status = 3` không bao giờ sinh ra `3%`, chạy `./gradlew.bat testDebugUnitTest assembleRelease`, cập nhật `PROJECT_CONTEXT.md`, push GitHub & cập nhật APK trên Release `v2.0.0` | [Agent: verifier-guard (`gemini-3.8-flash-medium`)] | [Skill: pr-feedback-quality-gate] | [Locked Files: `app/src/test/java/com/originisle/android/cards/EarbudsBatteryParserTest.kt`, `PROJECT_CONTEXT.md`, `task.md`, `release/OriginIsle-VN-v2.0.0.apk`]
