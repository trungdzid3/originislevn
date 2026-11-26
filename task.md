# DANH SÁCH CÔNG VIỆC (TASK MATRIX)

## Đợt v2.10.12 — Vá Nút Đảo Nhạc (`AudioManager.isMusicActive`), Bật Popup Tai Nghe Tức Thì & Dáng Bám Đáy
- [x] Gỡ so khớp chuỗi actionTitles trong `MediaCard.kt`, dùng `effectiveController` + `AudioManager.isMusicActive` (khắc phục kẹt tam giác khi đang phát) | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt]
- [x] Kích hoạt `BluetoothPopupOverlay.showOrUpdate(...)` tức thì khi tai nghe kết nối / Fast Pair `onBatteryUpdated` trong `EarbudsCard.kt` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/cards/EarbudsCard.kt]
- [x] Khôi phục dáng bottom sheet bám đáy (`roundBottomCorners = false`, bo 2 góc trên 32dp) trong `BluetoothPopupOverlay.kt` | [Agent: Lead-Orchestrator] | [Skill: originos-design] | [Locked Files: app/src/main/java/com/originisle/android/bluetooth/BluetoothPopupOverlay.kt]
- [x] Build `v2.10.12`, cập nhật `PROJECT_CONTEXT.md`, commit/push và phát hành GitHub Release `v2.10.12` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]
