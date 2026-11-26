# DANH SÁCH CÔNG VIỆC (TASK MATRIX)

## Đợt v2.10.11 — Vá Popup Tai Nghe (`FLAG_BLUR_BEHIND`) & Vá Nút Tạm Dừng Kẹt `||` Trên Zing MP3
- [x] Gỡ `FLAG_BLUR_BEHIND` / `blurBehindRadius` trên `BluetoothPopupOverlay.kt` (giữ nguyên giao diện Floating Liquid Glass OriginOS 6–7) | [Agent: Lead-Orchestrator] | [Skill: originos-design] | [Locked Files: app/src/main/java/com/originisle/android/bluetooth/BluetoothPopupOverlay.kt]
- [x] Sửa `MediaCard.post(...)` dùng `effectiveController` + `inferPlayingFromNotificationOrAudio` (tránh Zing MP3 giữ `isOngoing=true` khi đang tạm dừng) | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt]
- [x] Build `v2.10.11` (`testDebugUnitTest assembleRelease`), cập nhật `PROJECT_CONTEXT.md`, commit/push và phát hành GitHub Release `v2.10.11` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]
