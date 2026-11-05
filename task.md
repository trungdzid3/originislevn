# DANH SÁCH CÔNG VIỆC NÂNG CẤP HOẠT ẢNH TẢI XUỐNG & QUICK SHARE (CHÍNH XÁC THEO VIDEO DEMO)

[CONTRACT LOCKED]
- DownloadInfo:
  - `val isCanceled: Boolean = false`
  - `val actions: List<Notification.Action> = emptyList()`
- QuickShareInfo:
  - `val isCanceled: Boolean = false`
  - `val actions: List<Notification.Action> = emptyList()`
- Quy ước Animation Thành công:
  - `oi_light_color = "#00D084"`
  - `oi_icon_status_type = OriginIslandConstants.ICON_STATUS_SUCCESS` (0)
  - `oi_right_template = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT` (6) với text `✓`
  - Tự động hủy sau 3.5 giây (3500ms).
- Quy ước Animation Thất bại / Hủy:
  - `oi_light_color = "#FF5247"`
  - `oi_icon_status_type = OriginIslandConstants.ICON_STATUS_FAIL` (1)
  - `oi_right_template = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT` (6) với text `✕`
  - Tiêu đề ngắn gọn: "Đã hủy" / "✕" (tối giản chữ)
  - Tự động hủy sau 2.5 giây (2500ms).
- Quy ước Đang tải / Đang truyền:
  - Icon bên trái: Logo app thực tế của nguồn đang tải (`oi_left_icon` = app icon, `oi_large_icon` = app icon).
  - Bên phải: Vòng tròn tiến độ camera (`TEMPLATE_RIGHT_ISLAND_PROGRESS = 2`, `oi_progress = pct`, `show_progress = true`).
  - Mở rộng: Nếu có action buttons (`actions.isNotEmpty()`), dùng `TEMPLATE_BUTTONS` (8) cho phép Hủy / Tạm dừng / Tiếp tục; nếu không có actions thì dùng `TEMPLATE_BASE` (4) với `oi_card_ring = pct`.

[SUBAGENT TASKS]
- [x] Task 1: Nâng cấp `DownloadDetector.kt` và `DownloadInfo.kt` (bóc tách action buttons, phát hiện isCanceled/isFailed) | [Agent: Subagent-DownloadDetector] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/download/DownloadInfo.kt, app/src/main/java/com/originisle/android/download/DownloadDetector.kt]
- [x] Task 2: Nâng cấp `DownloadCard.kt` (logo app nguồn bên trái, vòng tròn tiến độ bên phải, mở rộng có nút Tạm dừng/Hủy, tích xanh khi xong, X đỏ khi hủy/lỗi) | [Agent: Subagent-DownloadCard] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/DownloadCard.kt]
- [x] Task 3: Nâng cấp `QuickShareDetector.kt`, `QuickShareCard.kt` và `NotificationCastListener.kt` (tương tự Download: tích xanh lá khi xong, X đỏ khi hủy/lỗi, nút mở rộng, chống tắt ngấm khi notification bị gỡ) | [Agent: Subagent-QuickShare] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/quickshare/QuickShareDetector.kt, app/src/main/java/com/originisle/android/cards/QuickShareCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]
- [x] Task 4: Kiểm tra QA, biên dịch APK `1.0.5`, cập nhật `PROJECT_CONTEXT.md` và đồng bộ Git | [Agent: Lead-Orchestrator] | [Skill: android-cli] | [Locked Files: PROJECT_CONTEXT.md, task.md]
