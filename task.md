# Kế Hoạch Điều Phối Swarm — Tách 2 Dự Án Độc Lập (OneUI Isle VN v1.0.0 & Origin Isle VN v2.2.0)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **Dự án 1 — `oneui-isle-vn` (`c:\Users\Lenovo\.gemini\antigravity\scratch\oneui-isle-vn`, `v1.0.0` / `10000`)**:
   - `applicationId = "com.oneuiisle.android"`, `minSdk = 33` (Android 13 / One UI 5.1+), `targetSdk = 36`, tên app `"OneUI Isle VN"`.
   - **KHÔNG có `AccessibilityService`**: Đã gỡ `KeepAliveAccessibilityService.kt` và khai báo trong `AndroidManifest.xml`.
   - **Khóa SharedPreferences (`"experimental_prefs"`) cho Bộ Thanh Trượt Toàn Dải**:
     + `PREF_ONEUI_OFFSET_X_PX = "oneui_island_offset_x_px"` (`Int`, `-250..250`, mặc định `0`)
     + `PREF_ONEUI_OFFSET_Y_PX = "oneui_island_offset_y_px"` (`Int`, `-150..200`, mặc định `0`)
     + `PREF_ONEUI_WIDTH_DP = "oneui_island_width_dp"` (`Int`, `120..340`, mặc định `196`)
     + `PREF_ONEUI_HEIGHT_DP = "oneui_island_height_dp"` (`Int`, `24..56`, mặc định `32`)
     + `PREF_ONEUI_CAMERA_GAP_DP = "oneui_island_camera_gap_dp"` (`Int`, `0..80`, mặc định `28`)
     + `PREF_ONEUI_HIDE_LANDSCAPE = "oneui_island_hide_landscape"` (`Boolean`, mặc định `true`)
   - **Hợp đồng Xem Thử Màu Đỏ (Calibration Mode)**:
     + Gửi `Intent(context, PlaygroundService::class.java)` với `action = PlaygroundService.ACTION_START`, `id = 35019`, `oi_auto_dismiss_ms = 8000L`, `oi_calibration_red = true`, `title = "Căn chỉnh"`, `oi_left_content = "Camera"`, `oi_right_content = "Khớp tâm"`.
     + Khi `id == 35019` hoặc `oi_calibration_red == true`, `OneUiOverlayIsland` vẽ viên thuốc màu đỏ nổi bật `#E53935` viền trắng `#FFFFFFFF` để người dùng căn khít lỗ camera thật.
   - **Phong cách Samsung (theo ảnh mẫu)**:
     + Viên thuốc đen OLED `#000000`, viền mảnh `0.8dp` `#33FFFFFF`, bo tròn hoàn toàn.
     + Khoang trái là **Huy hiệu Tròn Màu Đặc (Colored Circle Badge)** kích thước khớp chiều cao viên thuốc với icon trắng ở giữa (Tím `#5E5CE6` cho Timer/Lịch, Xanh dương `#3B82F6` cho Đèn pin/Tải xuống/QuickShare, Xanh lá `#34C759` cho Cuộc gọi/Sạc/Tai nghe, Xanh ngọc `#00BFA5` cho Ngân hàng/Thanh toán, Cam `#FF5E00` cho TikTok, hoặc icon trắng nét mảnh trực tiếp trên nền đen cho thông báo hệ thống).
     + Khoang giữa chừa đúng `cameraGapDp` cố định tuyệt đối quanh tâm `cutoutCenterX + offsetX`.
     + **Kiến trúc 2 Cửa Sổ (`Display Window MATCH_PARENT FLAG_NOT_TOUCHABLE` + `Touch Window`)**: triệt tiêu 100% lỗi nhảy lệch mỗi thẻ 1 bên và giật khung hình.
     + Dùng `ContextCompat.startForegroundService` an toàn cho Android 13 (One UI 5.1).

2. **Dự án 2 — `origin-isle-vn` (`c:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn`, `v2.2.0` / `20200`)**:
   - Trả về bản thuần Vivo OriginOS 6 (`minSdk = 34`, `applicationId = "com.autonavi.minimap"`).
   - Gỡ bỏ `OneUiOverlayIsland.kt`, quyền `SYSTEM_ALERT_WINDOW`, và toàn bộ cấu hình/UI One UI Overlay.

## Ma Trận Phân Quyền Swarm (Coordinator: Gemini 4 Neon | Workers: Gemini 4 Hydrogen)
- [x] Task 1 (`oneui-isle-vn` Core Engine): Cấu hình Gradle/Manifest/strings `com.oneuiisle.android` (`minSdk = 33`, `v1.0.0`), đại tu `OneUiOverlayIsland.kt` (Kiến trúc 2 Cửa Sổ + Phong cách Samsung Huy hiệu Tròn Màu + 5 thông số tự do + Chế độ đỏ `id=35019`), thuần hóa `PlaygroundService.kt` (`ContextCompat.startForegroundService`, bỏ SuperX) | [Agent: `oneui-engine-builder` (`gemini-4-hydrogen-medium`, `inherit`)] | [Locked Files: `oneui-isle-vn/app/build.gradle.kts`, `oneui-isle-vn/app/src/main/AndroidManifest.xml`, `oneui-isle-vn/app/src/main/res/values/strings.xml`, `oneui-isle-vn/app/src/main/java/com/originisle/android/island/OneUiOverlayIsland.kt`, `oneui-isle-vn/app/src/main/java/com/originisle/android/island/PlaygroundService.kt`]
- [x] Task 2 (`oneui-isle-vn` UI & Sliders): Thêm 5 Prefs toàn dải, thay UI căn chỉnh bằng 5 `Slider` + nút `-`/`+` + Xem thử màu đỏ trực tiếp trong `HomeScreen.kt`, thuần hóa `OnboardingScreen.kt`, `PermissionsSheet.kt`, `StatusHelpers.kt`, `MainActivity.kt`, `OriginIsleApp.kt` (bỏ Trợ năng & bỏ Vivo SuperX) | [Agent: `oneui-settings-ui` (`gemini-4-hydrogen-medium`, `inherit`)] | [Locked Files: `oneui-isle-vn/app/src/main/java/com/originisle/android/ui/Prefs.kt`, `oneui-isle-vn/app/src/main/java/com/originisle/android/ui/HomeScreen.kt`, `oneui-isle-vn/app/src/main/java/com/originisle/android/ui/OnboardingScreen.kt`, `oneui-isle-vn/app/src/main/java/com/originisle/android/ui/PermissionsSheet.kt`, `oneui-isle-vn/app/src/main/java/com/originisle/android/ui/StatusHelpers.kt`, `oneui-isle-vn/app/src/main/java/com/originisle/android/MainActivity.kt`, `oneui-isle-vn/app/src/main/java/com/originisle/android/OriginIsleApp.kt`]
- [x] Task 3 (`origin-isle-vn` Vivo Cleanup): Gỡ bỏ `OneUiOverlayIsland.kt` và toàn bộ code/UI One UI Overlay khỏi `origin-isle-vn`, nâng `v2.2.0` (`20200`), trả `minSdk = 34` | [Agent: `vivo-cleanup-v220` (`gemini-4-hydrogen-medium`, `share`)] | [Locked Files: `origin-isle-vn` (`OneUiOverlayIsland.kt`, `PlaygroundService.kt`, `app/build.gradle.kts`, `AndroidManifest.xml`, `Prefs.kt`, `StatusHelpers.kt`, `HomeScreen.kt`, `OnboardingScreen.kt`, `PermissionsSheet.kt`, `NotificationCastListener.kt`)]
- [x] Task 4 (Dual Build & Verify): Tích hợp nhánh `vivo-cleanup-v220`, build & test CẢ 2 dự án, xuất `origin-isle-vn/release/OriginIsle-VN-v2.2.0.apk` và `oneui-isle-vn/release/OneUIIsle-VN-v1.0.0.apk`, cập nhật `PROJECT_CONTEXT.md` & `task.md`, push `origin-isle-vn` lên `origin main` | [Agent: `verifier-dual-build` (`gemini-4-hydrogen-medium`, `inherit`)]
