# KẾ HOẠCH KHẮC PHỤC TRIỆT ĐỂ LỖI KẸT THÔNG BÁO QUICK SHARE 10%

[CONTRACT LOCKED]
- Sửa lỗi kẹt thông báo Quick Share 10% khi đã xong:
  1. `QuickShareDetector.kt`:
     - Kiểm tra cờ `FLAG_ONGOING_EVENT`. Khi thông báo không còn ongoing và `max <= 0`, coi là hoàn tất/đã nhận tệp (`isFinished = true`), loại bỏ hoàn toàn việc gán nhầm `progress = 10` & `isIndeterminate = true`.
     - Bổ sung bộ từ khóa hoàn tất đầy đủ: "đã lưu", "lưu vào", "sẵn sàng", "chạm để mở", "chạm để xem", "saved", "ready", "tap to view".
  2. `QuickShareCard.kt`:
     - Đồng bộ cờ `is_ongoing` với `sbn.isOngoing`. Nếu không phải ongoing hoặc `info.isFinished`, tự động chuyển sang trạng thái Hoàn tất (tích xanh, dismiss sau 3.5s).
     - Thêm cơ chế Watchdog timeout chống treo thẻ vĩnh viễn trên Đảo.
  3. `NotificationCastListener.kt`:
     - Khi notification bị gỡ bỏ, nếu không có cờ ongoing hoặc `isFinished` hoặc tệp đã nhận xong, phát hoạt ảnh Hoàn tất, không bao giờ rơi vào `postCanceled` tạo báo hủy ảo.

[SUBAGENT TASKS]
- [x] Task 1: Khắc phục logic nhận diện hoàn tất, loại bỏ kẹt 10% và đồng bộ cờ ongoing trong QuickShareDetector, QuickShareCard, NotificationCastListener | [Agent: Subagent-QuickShareStuckFix] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/quickshare/QuickShareDetector.kt, app/src/main/java/com/originisle/android/cards/QuickShareCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]
- [x] Task 2: Kiểm tra biên dịch, cập nhật PROJECT_CONTEXT.md, đóng gói lại APK v1.1.0 và đồng bộ Git | [Agent: Lead-Orchestrator] | [Skill: android-cli] | [Locked Files: PROJECT_CONTEXT.md, task.md]
