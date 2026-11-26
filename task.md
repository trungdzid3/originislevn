# DANH SÁCH CÔNG VIỆC (TASK MATRIX)

## Đợt v2.10.13 — Khôi Phục Nền Ổn Định `v2.10.9` + Chuẩn `upstream/main` Cho Đảo Nhạc & Popup Tai Nghe
- [x] Khôi phục `MediaCard.kt`, `MediaControlReceiver.kt` và `BluetoothPopupOverlay.kt` về nền ổn định `v2.10.9` (`5470dd8`), đối chiếu `upstream/main` (`null -> sbn.isOngoing`, `STATE_NONE -> false`), giữ 26 app whitelist + kích hoạt popup tức thì trong `EarbudsCard.kt` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/cards/MediaCard.kt, app/src/main/java/com/originisle/android/service/MediaControlReceiver.kt, app/src/main/java/com/originisle/android/bluetooth/BluetoothPopupOverlay.kt]
- [x] Build `v2.10.13`, cập nhật `PROJECT_CONTEXT.md`, commit/push và phát hành GitHub Release `v2.10.13` | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: app/build.gradle.kts, PROJECT_CONTEXT.md, task.md]
