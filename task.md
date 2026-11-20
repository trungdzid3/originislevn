# Kế Hoạch Điều Phối Swarm — Origin Isle VN v2.1.2 (Xóa Màu Viền & Diệt Triệt Để Giật Lag One UI 5.1)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT v2.1.2)
1. **Xóa hoàn toàn tính năng Màu viền phát sáng Đảo**:
   - `HomeScreen.kt`: Xóa bỏ hoàn toàn mục chọn "Màu viền phát sáng Đảo" (`ColorSwatchPill`, `customLightColor`, `resolveGlowColor`, `lightColorLabel`, dòng "Viền sáng: ...", hiệu ứng `drawBehind` hào quang trong `DeviceHeroCard`).
   - `OneUiOverlayIsland.kt`: Xóa bỏ `KEY_CUSTOM_LIGHT`, `customLightColor`, `resolveGlowColor`, dùng cố định viền hairline mảnh `#2EFFFFFF` / `#2E2E32`.
   - `PlaygroundService.kt`: Gán cố định `lightColor = 0` (bỏ đọc `cast_custom_light_color`).
   - `Prefs.kt`: Gỡ bỏ các hằng số `PREF_CAST_CUSTOM_LIGHT_COLOR`, `LIGHT_COLOR_*` (không còn file nào dùng).
2. **Diệt triệt để lỗi giật lag trên One UI 5.1**:
   - `OneUiOverlayIsland.kt`:
     + Xóa bỏ vòng lặp vô tận `addOnLayoutChangeListener -> calibratePositionLocked -> wm.updateViewLayout` (nguyên nhân chính gây giật 60-120 IPC/giây trên One UI 5.1 khi hệ thống kẹp tọa độ `loc[1]`).
     + Hiệu chuẩn trục `Y` tối đa **1 lần duy nhất** cho mỗi cấu hình `(offsetY, orientation, pillScalePct)` (ngưỡng `abs(deltaY) in 2..160`); nếu One UI kẹp sàn thì dừng ngay, không trừ `lp.y` liên tục.
     + Chỉ gọi `wm.updateViewLayout` khi `abs(lp.x - desiredX) >= 2` hoặc `lp.y` thực sự thay đổi.
     + Chống spam cập nhật trùng lặp (`cardSignature`): nếu `postOrUpdate` nhận cùng thẻ với nội dung y hệt đang hiển thị thì bỏ qua hoàn toàn.
     + Khi `hideOverlayLocked()`, đặt `currentCardState = null` để dừng triệt để mọi animation Compose (`MusicWaveBars`) khi Đảo ẩn.
   - `HomeScreen.kt`:
     + Thu gọn **Phòng thử nghiệm mẫu Đảo (`SampleTester`)** mặc định **đóng** (chỉ mở khi người dùng bấm vào để thử demo, không chạy `IslandPreviewView` animation liên tục ngay khi mở app).
     + Tối ưu không gọi `DeviceInfoHelper.collect(context)` 3 lần lúc khởi động và giãn vòng lặp kiểm tra `connected` lên `2500ms` (chỉ cập nhật khi giá trị đổi).
   - `NotificationCastListener.kt`:
     + Trong `onListenerConnected()`: trên máy dùng Overlay (`!DeviceInfoHelper.isSuperXSupported(this)`), **KHÔNG** gọi `reload()` dội hàng loạt thông báo cũ đang tồn đọng trong khay lên Đảo cùng lúc lúc mới mở app; chỉ xử lý thông báo mới đến (hoặc khi người dùng chủ động bấm "Hiện lại tất cả").
     + Giãn chu kỳ `pollRunnable` trên thiết bị không có SuperX từ `1000L` lên `2500L` để giảm tải Main Thread.
3. **Phiên bản**: `versionCode = 20102`, `versionName = "2.1.2"`.

## Ma Trận Phân Quyền Swarm (Coordinator: Gemini 4 Neon | Workers: Gemini 4 Hydrogen)
- [x] Task 1: Nâng `v2.1.2` (`20102`), sửa triệt để vòng lặp `updateViewLayout` + chống spam trùng thẻ + xóa màu viền trong `OneUiOverlayIsland.kt` và `PlaygroundService.kt` | [Agent: `oneui-perf-fix` (`gemini-4-hydrogen-medium`, `share`)] | [Locked Files: `app/build.gradle.kts`, `app/src/main/java/com/originisle/android/island/OneUiOverlayIsland.kt`, `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`]
- [x] Task 2: Xóa phần chọn màu viền trong `Prefs.kt` & `HomeScreen.kt`, thu gọn `SampleTester` mặc định đóng, tối ưu hiệu năng `HomeScreen.kt`, và tối ưu `onListenerConnected` + `pollRunnable` cho One UI trong `NotificationCastListener.kt` | [Agent: `ui-clean-v212` (`gemini-4-hydrogen-medium`, `share`)] | [Locked Files: `app/src/main/java/com/originisle/android/ui/Prefs.kt`, `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`, `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`]
- [x] Task 3: Tích hợp 2 nhánh `share`, chạy `./gradlew.bat testDebugUnitTest assembleRelease`, xuất `release/OriginIsle-VN-v2.1.2.apk`, cập nhật `PROJECT_CONTEXT.md` & `task.md`, kiểm tra Secret Guard, commit & push lên `origin main` | [Agent: `verifier-v212` (`gemini-4-hydrogen-medium`, `inherit`)] | [Locked Files: `PROJECT_CONTEXT.md`, `task.md`, `release/OriginIsle-VN-v2.1.2.apk`]
