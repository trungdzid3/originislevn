# DANH SÁCH CÔNG VIỆC (TASK MATRIX)

## Đợt hiện tại — Tự Động Nhận Diện Tai Nghe Popup & Gỡ Tùy Biến Thủ Công
- [x] Nâng cấp `BluetoothDevicePreset.inferFromName` nhận diện AirPods, Samsung Buds, Vivo Earbuds/TWS, Oppo Enco, Xiaomi, Redmi (giữ 3D cho Redmi Buds 6 Play & Lenovo LE208, còn lại dùng ảnh 2D mặc định) và bỏ áp dụng cấu hình tùy chỉnh trong `getDeviceConfig` | [Agent: Subagent-1] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/bluetooth/BluetoothPopupStore.kt, app/src/test/java/com/originisle/android/bluetooth/BluetoothPopupStoreTest.kt]
- [x] Gỡ bỏ đọc `customImagePath` trong `BluetoothPopupOverlay.kt` (chỉ dùng 3D hiện tại và ảnh 2D preset) và gỡ mục `Tùy chỉnh tai nghe Bluetooth` / `BluetoothPopupSheet` khỏi `ModulesScreen.kt` | [Agent: Subagent-2] | [Skill: N/A] | [Locked Files: app/src/main/java/com/originisle/android/bluetooth/BluetoothPopupOverlay.kt, app/src/main/java/com/originisle/android/ui/ModulesScreen.kt]
- [x] Chạy `.\gradlew.bat testDebugUnitTest --console=plain` kiểm tra toàn bộ unit test và biên dịch | [Agent: Lead-Orchestrator] | [Skill: N/A] | [Locked Files: N/A]
