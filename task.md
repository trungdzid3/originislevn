# Danh sách Công việc (Task Matrix) — OriginIsle-VN v1.3.2

## Hợp đồng Dữ liệu & Giao diện Đã Chốt (Contract Freeze Gate)
1. **Loại bỏ Hoàn toàn Cơ chế Suy đoán Ảo Quick Share (`QuickShareCard.kt`, `QuickShareDetector.kt`, `NotificationCastListener.kt`)**:
   - Thêm trạng thái `isFailed` trong `QuickShareInfo` nhận diện chính xác các từ khóa lỗi: `"lỗi"`, `"thất bại"`, `"không thể"`, `"failed"`, `"error"`, `"không thành công"`.
   - Bỏ điều kiện đoán mò `(info.progress >= 99)` hoặc `(info.progress >= 80)` trong `isFinishedEffective`. Chỉ công nhận hoàn tất khi:
     (1) `info.isFinished == true` (thông báo ghi rõ từ khóa hoàn tất/thành công hoặc có action mở/xem tệp không kèm cờ ongoing), HOẶC
     (2) `progress == 100`.
   - Bỏ watchdog 2.5s tự động ép sang 100% khi ở 90..99%. Nếu sau 15s không có thông báo mới thì tự đóng đảo mà KHÔNG báo 100%.
   - Trong `onNotificationRemoved`: Nếu thông báo bị xóa lúc chưa hoàn tất hoặc mang dấu hiệu lỗi/hủy, phát hoạt ảnh `postCanceled` (Đã hủy / Lỗi) hoặc đóng thẻ ngay, tuyệt đối không gọi `postFinished` khi chỉ mới ở 80..99%.

## Phân quyền Subagent & Khóa File (Locked Files)
- [x] Task 1: Bổ sung `isFailed` và hoàn thiện bóc tách trạng thái lỗi/hủy/hoàn tất thực sự trong `QuickShareDetector.kt` | [Agent: Subagent-QuickShare-ExactState] | [Skill: reverse-engineer-anything] | [Locked Files: app/src/main/java/com/originisle/android/quickshare/QuickShareDetector.kt]
- [x] Task 2: Loại bỏ đoán mò 99% / 80%, gỡ bỏ watchdog tự bung 100%, hiển thị thẻ Thất bại/Lỗi màu đỏ trong `QuickShareCard.kt` và xử lý `onNotificationRemoved` trong `NotificationCastListener.kt` | [Agent: Subagent-QuickShare-NoGuessing] | [Skill: frontend-design] | [Locked Files: app/src/main/java/com/originisle/android/cards/QuickShareCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]
- [ ] Task 3: Nâng phiên bản `1.3.2` (`versionCode = 10302`), kiểm thử Unit Test, đóng gói `OriginIsle-VN-v1.3.2.apk`, cập nhật `PROJECT_CONTEXT.md` và đồng bộ Git | [Agent: Lead Orchestrator] | [Skill: pr-feedback-quality-gate] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md]
