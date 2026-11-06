# KẾ HOẠCH BỔ SUNG PIN CHÍNH XÁC CHO VÒNG ĐEO GALAXY FIT 3 & ĐÓNG GÓI BẢN V1.1.0

[CONTRACT LOCKED]
- Báo pin thiết bị đeo Galaxy Fit 3 khi kết nối:
  - Nâng cấp `EarbudsCard.kt`:
    - Chuẩn hóa key và bổ sung bộ đệm pin toàn cầu `wearable_last_known_battery` cùng thuật toán so khớp mờ (Fuzzy matching) bỏ đuôi `(xxxx)`.
    - Tích hợp Bluetooth GATT Battery Service Reader (Service 0x180F / Characteristic 0x2A19) tự động đọc pin trực tiếp từ phần cứng vòng đeo tay khi kết nối ACL.
    - Trong `show()`, với thiết bị đeo, nếu pin chưa về kịp, luôn ưu tiên hiển thị pin đã biết gần nhất thay vì hiện chữ "Đã kết nối".
  - Nâng cấp `NotificationCastListener.kt`:
    - Quét sâu toàn bộ `extras.keySet()` của notification Samsung để bóc tách các trường pin ẩn (`battery_level`, `level`, `percentage`, `capacity`).
    - Khi nhận sự kiện kết nối mà notification chưa kịp mang % pin, tự động tra cứu bộ đệm để nạp mức pin ngay từ mili-giây đầu tiên.
- Biểu tượng Quick Share chính thức:
  - Tích hợp tệp vector `app/src/main/res/drawable/ic_quick_share.xml` và `PathParser` trong `QuickShareCard.kt` từ tệp SVG trong Downloads.

[SUBAGENT TASKS]
- [x] Task 1: Nâng cấp GATT Battery Service Reader và bộ đệm pin toàn cầu cho Galaxy Fit 3 trong EarbudsCard và NotificationCastListener | [Agent: Subagent-WearableBatteryHardware] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/EarbudsCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]
- [x] Task 2: Cập nhật icon Quick Share chính thức chuẩn Google từ SVG vào ic_quick_share.xml và QuickShareCard.kt | [Agent: Subagent-QuickShareVisual] | [Skill: android-cli] | [Locked Files: app/src/main/res/drawable/ic_quick_share.xml, app/src/main/java/com/originisle/android/cards/QuickShareCard.kt]
- [x] Task 3: Kiểm tra biên dịch, đóng gói lại APK v1.1.0 và đồng bộ Git | [Agent: Lead-Orchestrator] | [Skill: android-cli] | [Locked Files: PROJECT_CONTEXT.md, task.md]
