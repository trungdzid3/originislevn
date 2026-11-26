# DANH SÁCH CÔNG VIỆC (TASK MATRIX)

## Đợt v2.10.14 — Popup Tai Nghe Chuẩn OriginOS 6–7 (Bottom Sheet) & Khôi Phục `STATE_NONE` Đảo Nhạc (`v2.10.5`)
- [x] Nâng cấp `BluetoothPopupOverlay.kt` theo chuẩn OriginOS 6–7 (giữ bottom sheet bám đáy `32dp` & `WindowManager.LayoutParams` an toàn không `FLAG_BLUR_BEHIND`, 4 ánh sáng vật lý, huy hiệu `"Đã kết nối"`, Huarong Atomic Battery Pods `18dp` 2 dòng `tnum`, nút đáy `46dp` + Pill `48dp` Touch Light, kéo thả `First-frame Interruptible`) và cập nhật `DESIGN.md` | [Agent: Subagent-OriginOS-Popup-Redesign] | [Skill: originos-design] | [Locked Files: app/src/main/java/com/originisle/android/bluetooth/BluetoothPopupOverlay.kt, DESIGN.md]
- [x] Khôi phục `null, PlaybackState.STATE_NONE -> sbn.isOngoing || ...` của `v2.10.5` trong `MediaCard.kt` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt]
- [x] Build `v2.10.14`, cập nhật `PROJECT_CONTEXT.md`, commit/push và phát hành GitHub Release `v2.10.14` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]
