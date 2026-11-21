# Kế Hoạch Điều Phối — Origin Isle VN v2.1.3 (Khắc Phục Báo % Tải Chrome/Edge & Chống Khuyết Icon Tải Xong)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **Khôi phục 100% báo `%` tiến độ khi đang tải trên Chrome & Edge (`DownloadDetector.kt`, `NotificationCastListener.kt`, `DownloadCard.kt`)**:
   - Trong `DownloadDetector.isMediaStyleNotification(n, extras)`:
     + Nếu `n.category == Notification.CATEGORY_PROGRESS` hoặc thông báo có `EXTRA_PROGRESS_MAX > 0` hoặc `EXTRA_PROGRESS_INDETERMINATE == true` mà **không có** `EXTRA_MEDIA_SESSION` (`MediaSession.Token`) và template không chứa `"MediaStyle"` -> trả về `false` ngay lập tức!
     + Xóa `"tiếp"` khỏi `MEDIA_ACTION_KEYWORDS` (vì `"Tiếp tục"` là nút Resume tải tệp của trình duyệt).
     + Nếu `n.actions` có nút `"hủy"` / `"cancel"` / `"tiếp tục"` / `"resume"` -> trả về `false` (đây là nút điều khiển tải tệp của Chrome/Edge, tuyệt đối không phải trình phát nhạc).
   - Trong `NotificationCastListener.isActualMediaPlaybackNotification(sbn, token)`:
     + Nếu `token == null` và `DownloadDetector.isDownloadPackage(sbn.packageName)` -> trả về `false` ngay để thông báo đang tải của Chrome/Edge không bao giờ bị nhận nhầm là Media và không bị gọi `cancelStrayDownloadCard`.
   - Trong `DownloadCard.post`:
     + Sửa `info.progress in 1..99` thành `info.progress in 0..99`.
     + Sửa `shouldPost`: luôn cho phép đăng ngay ở lần đầu (`val isFirstPost = lastTime == 0L || lastProg == -1`), và giảm ngưỡng cập nhật tiến độ xuống `now - lastTime >= 800L && (abs(info.progress - lastProg) >= 1 || info.isIndeterminate)`.
2. **Chống khuyết cạnh phải vòng tròn tích xanh & kéo dài nhẹ viên thuốc khi tải xong (`DownloadCard.kt`, `QuickShareCard.kt`, `NotificationCastListener.kt`)**:
   - Trong `BatteryCanvasRenderer.createGreenTickCircleIcon` (`DownloadCard.kt`):
     + Thu nhẹ bán kính vòng tròn `r = s * 0.35f` (thay vì `0.42f`) và dịch nhẹ tâm `cx = s * 0.44f` về bên trái để chừa khoảng đệm an toàn bên phải, không chạm vào góc bo của viên thuốc.
   - Đặt `oi_right_content = "\u2009"` (thin space) và `status_chip_text = "\u2009"` ở trạng thái hoàn tất trong `DownloadCard.kt`, `QuickShareCard.kt`, `NotificationCastListener.kt` để kéo dài nhẹ đuôi phải của viên thuốc Đảo động thêm một chút.

## Ma Trận Phân Quyền (Orchestrator: Gemini 4 Argon | Worker: Gemini 4 Hydrogen)
- [x] Task 1: Sửa `DownloadDetector.kt`, `DownloadCard.kt`, `QuickShareCard.kt`, `NotificationCastListener.kt`, nâng version `2.1.3` (`20103`), chạy test + build `release/OriginIsle-VN-v2.1.3.apk` và phát hành GitHub Release `v2.1.3` | [Agent: Download-And-Tick-Fix (`gemini-4-hydrogen-medium`)] | [Skill: impeccable] | [Locked Files: `app/src/main/java/com/originisle/android/download/DownloadDetector.kt`, `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`, `app/src/main/java/com/originisle/android/cards/QuickShareCard.kt`, `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`, `app/build.gradle.kts`, `PROJECT_CONTEXT.md`, `task.md`]
