# DANH SÁCH CÔNG VIỆC (TASK MATRIX)

## Đợt v2.10.21 — Sửa Kẹt Trạng Thái Khi Xóa Chữ "Playing" (Phân Biệt `status_chip_text` & `oi_right_content`)
- [x] Tách `status_chip_text = if (playing) " " else "  "` và `oi_right_content = if (playing) "" else " "` trong `MediaCard.kt` để vẫn ẩn hoàn toàn chữ `"Playing"` nhưng buộc vivo OriginOS 6 SystemUI re-render tức thì cả 5 cột sóng / 5 chấm tròn và nút Play/Pause khi đổi trạng thái | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt]
- [x] Build `v2.10.21`, cập nhật `PROJECT_CONTEXT.md`, commit/push và phát hành GitHub Release `v2.10.21` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]
