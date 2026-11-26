# DANH SÁCH CÔNG VIỆC (TASK MATRIX)

## Đợt v2.10.19 — Đảo Nhạc Upstream Gốc + Sóng Nhạc 5 Cột (`createMusicWaveIcon`)
- [x] Giữ nguyên 100% logic & nút chuẩn gốc `upstream/main` (`v2.10.17`, bỏ `oi_btn_transparent`), chỉ thay phần hiển thị sóng nhạc khi đang phát bằng `createMusicWaveIcon(waveColors)` trên `TEMPLATE_RIGHT_ISLAND_TEXT_ICON` (`oi_right_content = if (playing) "" else chip`) trong `MediaCard.kt` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt]
- [x] Build `v2.10.19`, cập nhật `PROJECT_CONTEXT.md`, commit/push và phát hành GitHub Release `v2.10.19` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]
