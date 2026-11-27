# DANH SÁCH CÔNG VIỆC (TASK MATRIX)

## Đợt v2.10.25 — Chạm Đảo Mở Thẳng Bảng Điều Khiển Kính Mờ `MediaIslandOverlay` (Không `FLAG_BLUR_BEHIND`)
- [x] Kết nối `MediaIslandLaunchActivity` (`source_content_intent`) với `oi_island_click_expand = !useHybridOverlay` trong `MediaCard.kt` (giữ nguyên `oi_show_notify = false`, cơ chế chống kẹt `" "` vs `"  "` và 5 cột sóng / 5 chấm tròn) | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt]
- [x] Gỡ `FLAG_BLUR_BEHIND` / `blurBehindRadius` khỏi `MediaIslandOverlay.kt` để tránh OriginOS 6 ẩn overlay | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/service/MediaIslandOverlay.kt]
- [x] Build `v2.10.25`, cập nhật `PROJECT_CONTEXT.md`, commit/push và phát hành GitHub Release `v2.10.25` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]
