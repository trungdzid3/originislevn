# Danh sách Công việc (Task Matrix) — OriginIsle-VN v1.3.1

## Hợp đồng Dữ liệu & Giao diện Đã Chốt (Contract Freeze Gate)
1. **Khôi phục Đảo Động Nhạc cho Zing MP3 & Các Ứng dụng Nhạc (`MediaCard.kt`, `OriginIslandBuilder.kt`, `PlaygroundService.kt`)**:
   - Đổi `scene` và `oi_scene` trong `MediaCard.kt` về `"NAVIGATION"` (bắt buộc cho package giả lập `com.autonavi.minimap` để không bị Vivo SystemUI chặn thẻ nhạc).
   - Chuẩn hóa `OriginIslandBuilder.kt` theo đúng schema SuperX của Vivo OriginOS 6 cho `TEMPLATE_BUTTONS (8)`:
     - Trong `baseInfos`: luôn đặt `base.putInt(C.BUNDLE_KEY_BASE_SUB_INFO, 0)` cho `template == 8` (không truyền `BASE_SUB_INFO_IMAGE = 3` để tránh bị SuperX Validator đánh dấu `SUPERX_VALUE = "false"`).
     - Trong `infos` nhánh `8`: giới hạn chuẩn `.take(3)` nút điều khiển, dùng cặp màu chuẩn Vivo `textColor = Color.BLACK` (`#FF000000`) và `btnColor = Color.WHITE` (`#FFFFFFFF`), xóa `BUNDLE_KEY_INFO_PROGRESS` khỏi nhánh `8`.
     - Trong `right` nhánh `1` (`TEMPLATE_RIGHT_ISLAND_WAVE`): luôn truyền `BUNDLE_KEY_ISLAND_RIGHT_WAVE_COLOR` (fallback `listOf("#FF5376", "#00E5FF", "#7C4DFF")` nếu rỗng), `BUNDLE_KEY_ISLAND_RIGHT_WAVE_STATE` và `right.putBoolean(C.BUNDLE_KEY_ISLAND_RIGHT_PLAY_PAG, waveState == 1)`.
   - Đặt `putExtra("oi_force_show", true)` trong `MediaCard.kt` và giữ duy nhất 1 thẻ nhạc `MediaCard.ID = 35010`.
2. **Nhận diện Chính xác Thông báo Phát nhạc Zing MP3 (`NotificationCastListener.kt`)**:
   - Viết hàm `isAuxiliaryMusicDownloadNotif(sbn, token)` chỉ chặn thông báo tải nhạc ngoại tuyến thực sự của ứng dụng nhạc (`token == null && !isMediaStyleOrTransport(n)` kèm progress bar hoặc từ khóa `"đang tải"`, `"tải nhạc"`, `"tải xuống"`, `\d+/\d+\s*bài`).
   - Cho phép mọi thông báo phát nhạc của `isKnownMusicApp(sbn.packageName)` / `DownloadDetector.isMusicOrMediaApp(sbn.packageName)` (bao gồm khi tìm thấy phiên phát qua `findMediaControllerFallback(sbn.packageName)`) đi vào `MediaCard.postFromNotification`.
   - Không gọi `isOriginNativePlayerActive()` để xóa thẻ đối với các ứng dụng trong `KNOWN_MUSIC_APPS` / `MediaCard.THIRD_PARTY_MUSIC_PACKAGES`.
   - Trong `pollRunnable`, nếu `castMedia == true` mà chưa có thông báo nhạc trên thanh trạng thái nhưng có `MediaController` đang phát (`STATE_PLAYING`) thuộc `KNOWN_MUSIC_APPS`, tự động cập nhật thẻ nhạc từ `MediaController`.

## Phân quyền Subagent & Khóa File (Locked Files)
- [x] Task 1: Chuẩn hóa `OriginIslandBuilder.kt`, `PlaygroundService.kt` và `MediaCard.kt` (`oi_scene = "NAVIGATION"`, schema `TEMPLATE_BUTTONS (8)` hợp lệ SuperX, sóng nhạc bên phải Đảo, `oi_force_show = true`) | [Agent: Subagent-Media-RestoreIsland] | [Skill: frontend-design] | [Locked Files: app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt, app/src/main/java/com/originisle/android/island/PlaygroundService.kt, app/src/main/java/com/originisle/android/cards/MediaCard.kt]
- [x] Task 2: Khắc phục bộ lọc thông báo Zing MP3 trong `NotificationCastListener.kt` (`isAuxiliaryMusicDownloadNotif`, `isActualMediaPlaybackNotification`, hỗ trợ `findMediaControllerFallback` và `pollRunnable`) | [Agent: Subagent-Listener-ZingMp3Fix] | [Skill: reverse-engineer-anything] | [Locked Files: app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]
- [x] Task 3: Nâng phiên bản `1.3.1` (`versionCode = 10301`), kiểm thử Unit Test, đóng gói `OriginIsle-VN-v1.3.1.apk`, cập nhật `PROJECT_CONTEXT.md` và đồng bộ Git | [Agent: Lead Orchestrator] | [Skill: pr-feedback-quality-gate] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md]
