# Kế Hoạch Điều Phối Swarm — Origin Isle VN v2.1.0 (Mở Rộng Hỗ Trợ Samsung One UI 5.1+)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **Không sử dụng Quyền Trợ năng (`AccessibilityService`)**:
   - Đảo One UI chạy trực tiếp qua `WindowManager` (`TYPE_APPLICATION_OVERLAY`) + `ComposeView` trong `PlaygroundService` với `FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS or FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL or FLAG_HARDWARE_ACCELERATED` và `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`.
2. **Hợp đồng Độc Lập Giữa Các Worker (Decoupled Scope)**:
   - `PlaygroundService.kt` kiểm tra `DeviceInfoHelper.isSuperXSupported(this)` (đã có sẵn) để định tuyến giữa luồng Vivo SuperX gốc và `OneUiOverlayIsland`.
   - `OneUiOverlayIsland.kt` lắng nghe trực tiếp `SharedPreferences.OnSharedPreferenceChangeListener` trên `"experimental_prefs"` đối với các khóa:
     + `"oneui_island_offset_x_px"` (Int, mặc định `0`)
     + `"oneui_island_offset_y_px"` (Int, mặc định `0`)
     + `"oneui_island_pill_scale_pct"` (Int, mặc định `100`)
     + `"island_position"` (String: `"LEFT"`, `"CENTER"`, `"RIGHT"`)
     + `"cast_custom_light_color"` (String: `"NONE"`, `"AUTO"`, `#...`)
     Do đó `HomeScreen.kt` không import trực tiếp `OneUiOverlayIsland`.
3. **4 Lớp Chống Lệch Đảo Động trên One UI 5.1+**:
   - Đọc `windowManager.currentWindowMetrics` và `displayCutout?.boundingRectsTop?.firstOrNull { !it.isEmpty }` lấy tâm `cutoutCenterX`, `cutoutCenterY` và kích thước lỗ camera vật lý.
   - Kết hợp `offsetX`, `offsetY` từ `SharedPreferences`.
   - Tự động hiệu chuẩn qua `composeView.getLocationOnScreen(loc)`: nếu `deltaY = loc[1] - desiredScreenTopPx` nằm trong `1..200` (`abs(deltaY)`), tự động bù `layoutParams.y -= deltaY` qua `updateViewLayout`.
   - Khi ở `"CENTER"`, viên thuốc thu gọn chừa vùng rỗng giữa đúng bằng `cutoutWidthDp + 12.dp` để lỗ đục camera không che chữ/icon.
4. **Hạ SDK tối thiểu & Phiên bản**:
   - `app/build.gradle.kts`: `minSdk = 33`, `versionCode = 20100`, `versionName = "2.1.0"`.
   - `PlaygroundService.kt`: `ensureForeground()` chỉ truyền `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` khi `Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE`.

## Ma Trận Phân Quyền Swarm (Coordinator: Gemini 4 Argon | Workers: Gemini 4 Hydrogen)
- [x] Task 1: Hạ `minSdk = 33`, nâng `v2.1.0` (`20100`), thêm quyền `SYSTEM_ALERT_WINDOW`, xây dựng `OneUiOverlayIsland.kt` (chống lệch 4 lớp + Compose Pill & Expanded Card), cập nhật `PlaygroundService.kt` | [Agent: `oneui-core-overlay` (`gemini-4-hydrogen-medium`, `share`)] | [Locked Files: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/originisle/android/island/OneUiOverlayIsland.kt`, `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`]
- [x] Task 2: Thêm `Prefs.kt` keys tinh chỉnh vị trí One UI, nhận diện One UI >= 5.1 trong `DeviceInfoHelper.kt`, thêm helper Overlay trong `StatusHelpers.kt`, bỏ yêu cầu Trợ năng & thêm tinh chỉnh tâm Đảo X/Y trên `OnboardingScreen.kt`, `PermissionsSheet.kt`, `HomeScreen.kt` | [Agent: `oneui-device-ui` (`gemini-4-hydrogen-medium`, `share`)] | [Locked Files: `app/src/main/java/com/originisle/android/ui/Prefs.kt`, `app/src/main/java/com/originisle/android/ui/DeviceInfoHelper.kt`, `app/src/main/java/com/originisle/android/ui/StatusHelpers.kt`, `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`, `app/src/main/java/com/originisle/android/ui/OnboardingScreen.kt`, `app/src/main/java/com/originisle/android/ui/PermissionsSheet.kt`]
- [x] Task 3: Bổ sung đầy đủ các package Samsung Galaxy Buds (`com.samsung.accessory.*`, `com.samsung.android.hearablemgr`) trong `EarbudsCard.kt` và `NotificationCastListener.kt` | [Agent: `oneui-samsung-buds` (`gemini-4-hydrogen-medium`, `share`)] | [Locked Files: `app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`, `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`]
- [x] Task 4: Tích hợp 3 nhánh `share`, chạy `./gradlew.bat testDebugUnitTest assembleRelease`, xuất `release/OriginIsle-VN-v2.1.0.apk`, cập nhật `PROJECT_CONTEXT.md` & `task.md`, kiểm tra Secret Guard, commit & push GitHub + tạo Release `v2.1.0` | [Agent: `verifier-v210` (`gemini-4-hydrogen-medium`, `inherit`)] | [Locked Files: `PROJECT_CONTEXT.md`, `task.md`, `release/OriginIsle-VN-v2.1.0.apk`]
