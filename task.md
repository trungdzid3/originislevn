# DANH SÁCH CÔNG VIỆC (TASK MATRIX)

## Đợt v2.10.20 — Xóa Chữ "Playing" & Đổi Icon Tạm Dừng Pill Thành 5 Chấm Tròn Sóng Dừng (`.....`)
- [x] Đặt `status_chip_text = " "` và `oi_right_content = ""` (xóa hoàn toàn chữ `"Playing"`), bổ sung `createPausedWaveIcon(waveColors)` vẽ 5 chấm tròn đồng bộ vị trí/màu sóng nhạc khi tạm dừng trong `MediaCard.kt`, giữ nguyên 100% logic & nút chuẩn gốc `upstream/main` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt]
- [x] Build `v2.10.20`, cập nhật `PROJECT_CONTEXT.md`, commit/push và phát hành GitHub Release `v2.10.20` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]
