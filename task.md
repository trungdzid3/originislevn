# KẾ HOẠCH TRIỂN KHAI HỖ TRỢ GALAXY FIT 3 & THẺ PIN ĐA THÀNH PHẦN CO GIÃN THÍCH ỨNG

[CONTRACT LOCKED]
- Phân loại thiết bị (`DeviceCategory`):
  - `EARBUDS_TWS`: Tai nghe không dây 2 bên + Hộp sạc.
  - `HEADPHONES`: Tai nghe chụp tai / 1 thành phần âm thanh.
  - `SMARTBAND_WATCH`: Vòng đeo tay / Đồng hồ thông minh (Galaxy Fit 3 `SM-R390`, Galaxy Watch, Mi Band,...).
- Cấu trúc dữ liệu thành phần pin (`BatterySlot`):
  - `label: String` ("L", "R", "Hộp sạc", "Galaxy Fit 3")
  - `level: Int` (0..100)
  - `isCharging: Boolean`
- Thuật toán Co giãn Thích ứng (Adaptive Layout Algorithm) khi chạm mở rộng:
  - `slots.size == 3` (Đủ L + R + Hộp sạc): Dùng `TEMPLATE_BUTTONS (8)` chia 3 cột đều nhau (33.3% x 3), mỗi cột gồm `DynamicBatteryIcon` + `"L · 95%"`, `"R · 100%"`, `"Hộp · 80%"`.
  - `slots.size == 2` (Ví dụ chỉ có `R` và `Hộp sạc` như ảnh mẫu `heobeos plus`, hoặc `L` + `R`): Tự động loại bỏ thành phần trống, co giãn 2 thành phần còn lại sang 2 bên cân xứng 50% - 50% qua `TEMPLATE_BUTTONS (8)` với icon viên pin riêng biệt.
  - `slots.size == 1` (Chỉ có Galaxy Fit 3 hoặc 1 tai nghe đơn): Co giãn về 1 ô trung tâm 100% kèm vòng tròn tiến độ `oi_card_ring = level`.
- Kịch bản Kết nối & Mất kết nối:
  - Khi kết nối (`CONNECTED`): Viên thuốc hiện icon thiết bị + `%` pin + vòng tiến độ xanh lá `#00D084`, giữ 5s.
  - Khi mất kết nối (`DISCONNECTED` sau debounce 1.5s): Viên thuốc phát sáng viền đỏ `#FF5247`, icon gạch chéo/dấu `✕` (`ICON_STATUS_FAIL = 1`), tiêu đề `"$name · Mất kết nối"`, giữ 3s rồi tự đóng.

[SUBAGENT TASKS]
- [x] Task 1: Xây dựng bộ vẽ `BatteryCanvasRenderer.kt` (vẽ icon viên pin ngang màu xanh/vàng/đỏ theo % + tia sét sạc và icon đồng hồ/vòng đeo tay Galaxy Fit 3) | [Agent: Subagent-BatteryRenderer] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/BatteryCanvasRenderer.kt, app/src/main/res/drawable/ic_smartband.xml]
- [x] Task 2: Nâng cấp `EarbudsCard.kt` (mở khóa lọc `WEARABLE`/BLE GATT cho Galaxy Fit 3, lắng nghe Samsung Broadcasts, máy trạng thái Kết nối/Mất kết nối, và thuật toán co giãn bố cục 1-2-3 thành phần pin) | [Agent: Subagent-EarbudsWearable] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/EarbudsCard.kt]
- [x] Task 3: Cập nhật `NotificationCastListener.kt` (bắt thông báo trạng thái kết nối & pin từ `com.samsung.wearable.fit3plugin` / `com.samsung.android.app.watchmanager` chuyển tiếp sang `EarbudsCard`) | [Agent: Subagent-CastListenerWearable] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/service/NotificationCastListener.kt]
- [x] Task 4: Kiểm tra QA, biên dịch APK `1.0.5`, cập nhật `PROJECT_CONTEXT.md` và đồng bộ Git | [Agent: Lead-Orchestrator] | [Skill: android-cli] | [Locked Files: PROJECT_CONTEXT.md, task.md]
