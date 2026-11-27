# DANH SÁCH CÔNG VIỆC (TASK MATRIX)

## Đợt v2.10.26 — Bảng Nhạc Đen Tuyền OLED, Media Output Switcher, Ẩn Đảo Nhỏ Khi Mở & Phóng To Sóng Nhạc
- [x] Đổi `CARD_BG_COLOR = 0xFF000000` (đen tuyền OLED), nâng cấp `openAudioOutputPicker` ưu tiên hộp thoại Media Output Switcher, và gọi `MediaCard.setOverlaySuppression` để ẩn Đảo nhỏ khi mở `MediaIslandOverlay` / hiện lại khi đóng | [Agent: Lead-Orchestrator] | [Skill: originos-design] | [Locked Files: app/src/main/java/com/originisle/android/service/MediaIslandOverlay.kt, app/src/main/java/com/originisle/android/cards/MediaCard.kt]
- [x] Phóng to icon 5 cột sóng nhạc (`createMusicWaveIcon`) & 5 chấm tròn sóng dừng (`createPausedWaveIcon`) trong `MediaCard.kt` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt]
- [x] Build `v2.10.26`, cập nhật `PROJECT_CONTEXT.md`, commit/push và phát hành GitHub Release `v2.10.26` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]
