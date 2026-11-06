# DANH SÁCH CÔNG VIỆC (TASK MATRIX) - BẢN NÂNG CẤP v1.2.6

[CONTRACT LOCKED]
- Phiên bản mục tiêu: `v1.2.6` (`versionCode = 10206`, `versionName = "1.2.6"`).
- Khóa phạm vi file độc lập giữa các Subagent.

## Ma trận Phân quyền Thực thi

- [x] **Task 1: Sửa triệt để RingerModeCard (RECEIVER_NOT_EXPORTED chuẩn Android 15/16, lắng nghe thêm VOLUME_CHANGED_ACTION, sửa bitmask readVibrateSetting, thêm click_resp, oi_right_icon Bitmap, bỏ nền xám capsule trong RingerModeCard & EarbudsCard)**
  - `[Agent: Subagent-Ringer-Card]`
  - `[Skill: android-cli]`
  - `[Locked Files: app/src/main/java/com/originisle/android/cards/RingerModeCard.kt, app/src/main/java/com/originisle/android/cards/EarbudsCard.kt]`

- [x] **Task 2: Nâng cấp PlaygroundService & IconCache (Chiếm quyền scene NAVIGATION cho mọi ID thẻ và tự động khôi phục thẻ nền ongoing sau khi thẻ chớp nhoáng đóng; rasterize 100% icon sang Bitmap)**
  - `[Agent: Subagent-Playground-Scene]`
  - `[Skill: android-cli]`
  - `[Locked Files: app/src/main/java/com/originisle/android/island/PlaygroundService.kt, app/src/main/java/com/originisle/android/service/IconCache.kt]`

- [x] **Task 3: Chấm dứt triệt để lỗi tách 2 Đảo Nhạc và chuẩn hóa giao diện Đảo Nhạc (Chặn NotificationCastListener nhân bản com.vivo.musicwidgetmix / VIVO_SUPERX_TAG sang GenericCard, tự động thu hồi MediaCard khi Đảo gốc xuất hiện, mở rộng whitelist app nhạc OriginOS 6, chuyển toàn bộ icon nút MediaCard sang pure Bitmap)**
  - `[Agent: Subagent-Media-ZeroOverlap]`
  - `[Skill: android-cli]`
  - `[Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]`

- [x] **Task 4: Nâng cấp build.gradle.kts lên v1.2.6 (10206), Unit Test QA, build APK & Đồng bộ Git**
  - `[Agent: Lead-Orchestrator]`
  - `[Skill: pr-feedback-quality-gate]`
  - `[Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]`
