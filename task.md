# DANH SÁCH CÔNG VIỆC (TASK MATRIX) - BẢN NÂNG CẤP v1.2.7

[CONTRACT LOCKED]
- Phiên bản mục tiêu: `v1.2.7` (`versionCode = 10207`, `versionName = "1.2.7"`).
- Khóa phạm vi file độc lập giữa các Subagent.

## Ma trận Phân quyền Thực thi

- [x] **Task 1: Khắc phục triệt để hiển thị nhạc Zing MP3 & các app bên thứ 3 (Gỡ bỏ chặn nhầm ORIGIN_WHITELISTED_MUSIC_APPS, chỉ nhường khi com.vivo.musicwidgetmix thực sự active, trích xuất Album Art đa tầng & an toàn 100% Bitmap icon)**
  - `[Agent: Subagent-Zing-Media]`
  - `[Skill: android-cli]`
  - `[Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]`

- [x] **Task 2: Nâng cấp build.gradle.kts lên v1.2.7 (10207), Unit Test QA, build APK & Đồng bộ Git**
  - `[Agent: Lead-Orchestrator]`
  - `[Skill: pr-feedback-quality-gate]`
  - `[Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]`
