# KẾ HOẠCH NÂNG CẤP HOẠT ẢNH, CHẾ ĐỘ RUNG/CHUÔNG, CƠ CHẾ PIN XIAOMI & LOGO ORIGINOS 6

[CONTRACT LOCKED]
- Vòng tròn dấu X đỏ (`RedCrossCircleIcon`):
  - Kích thước: 96x96 px.
  - Vòng tròn nền đỏ đặc `#FF5247` với viền bo tròn mượt.
  - Dấu `✕` trắng dày 4.5dp bên trong với đầu nét bo tròn `Paint.Cap.ROUND`.
  - Được dùng làm `oi_right_icon` với `TEMPLATE_RIGHT_ISLAND_TEXT_ICON (4)` và `oi_right_content = ""` (chế độ icon-only 103 của vivo).
- Hoạt ảnh Tải xuống & Hủy:
  - Khi bắt đầu tải (`is_ongoing`): Hiển thị biểu tượng tải xuống ban đầu `ic_download` trên đảo bên cạnh logo app và vòng tiến độ xanh `#00D084`.
  - Khi hủy tải (`isCanceled` hoặc khi thông báo tải dở bị gỡ bỏ): Thẻ lập tức chuyển sang trạng thái Hủy với dấu `✕` đỏ trong vòng tròn đỏ, viền phát sáng đỏ `#FF5247`, tự đóng sau 2.5s.
- Cơ chế Báo Pin Xiaomi Earbuds (Reverse Engineering Findings):
  - Giao thức BLE Xiaomi / RCSP: `mElectricList` gồm 3 phần tử: `INDEX_LEFT = 0`, `INDEX_RIGHT = 1`, `INDEX_BOX = 2` (Hộp sạc).
  - Định dạng byte pin: Bit 7 (`& 0x80` = `IS_CHARGE_TAG`) biểu thị đang sạc, Bits 0-6 (`& 0x7F` = `REAL_BETTERY_VALUE`) biểu thị mức % pin (0..100).
  - Kiểm tra tính hợp lệ: `isInvalidValue(val)` trả về true nếu `val < 0 || val > 100`. Nếu không cắm tai nghe hoặc thành phần không truyền, giá trị là không hợp lệ -> View của thành phần đó tự động chuyển sang `GONE`, và Layout co giãn thích ứng giữa 1, 2, hoặc 3 thành phần.
- Thẻ Thông báo Chế độ Chuông / Rung / Im lặng (`RingerModeCard.kt`):
  - Lắng nghe `AudioManager.RINGER_MODE_CHANGED_ACTION`.
  - `RINGER_MODE_NORMAL` (2): Chuông (Icon Chuông vàng `#FCD535`, nhãn "Chuông").
  - `RINGER_MODE_VIBRATE` (1): Rung (Icon Điện thoại Rung lam `#4A90E2`, nhãn "Rung").
  - `RINGER_MODE_SILENT` (0): Im lặng (Icon Chuông gạch chéo đỏ `#FF5247`, nhãn "Im lặng").
  - Thời gian hiển thị: 2500ms tự đóng mượt mà.
- Thiết kế Logo OriginOS 6:
  - Dựa trên triết lý "Origin Design": Đường cong G2 liên tục (G2-continuous squircle), Liquid Glass (kính chất lỏng trong suốt), Dynamic Glow (hào quang xanh lam #2555FF & ngọc lam #00D084), viên nang Đảo Nguyên Tử (Atomic Island Capsule).

[SUBAGENT TASKS]
- [x] Task 1: Sửa UI ngắt kết nối tai nghe thành vòng tròn đỏ có dấu X trắng bên trong | [Agent: Subagent-DisconnectBadge] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/BatteryCanvasRenderer.kt, app/src/main/java/com/originisle/android/cards/EarbudsCard.kt]
- [x] Task 2: Khôi phục biểu tượng tải xuống ban đầu và sửa cơ chế chuyển sang dấu X đỏ khi hủy tải | [Agent: Subagent-DownloadCancel] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/DownloadCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]
- [x] Task 3: Xây dựng `RingerModeCard.kt` hiển thị đảo thông báo khi đổi chế độ Chuông / Rung / Im lặng theo video demo | [Agent: Subagent-RingerMode] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/RingerModeCard.kt, app/src/main/java/com/originisle/android/service/OriginApp.kt]
- [x] Task 4: Thiết kế lại Logo chuẩn OriginOS 6 (G2 squircle, Liquid Glass, Dynamic Glow) | [Agent: Subagent-LogoDesign] | [Skill: logo-design] | [Locked Files: app/src/main/res/drawable/ic_launcher_foreground.xml, app/src/main/res/drawable/ic_launcher_background.xml]
- [x] Task 5: Biên dịch QA, cập nhật `PROJECT_CONTEXT.md` và đồng bộ Git | [Agent: Lead-Orchestrator] | [Skill: android-cli] | [Locked Files: PROJECT_CONTEXT.md, task.md]
