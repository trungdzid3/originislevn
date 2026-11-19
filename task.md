# Kế Hoạch Điều Phối Swarm — Origin Isle VN v2.1.1 (Hoàn Thiện Đảo One UI 5.1+ & Đổi Điều Phối Sang Gemini 4 Neon)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT v2.1.1)
1. **Khóa SharedPreferences Mới (`experimental_prefs`)**:
   - `"oneui_island_hide_landscape"` (`Boolean`, mặc định `true`) — khai báo `PREF_ONEUI_HIDE_LANDSCAPE` / `DEFAULT_ONEUI_HIDE_LANDSCAPE = true` trong `Prefs.kt`.
2. **Hợp đồng Xem Thử Căn Chỉnh Camera Không Phụ Thuộc Import**:
   - `HomeScreen.kt` gửi `Intent(context, PlaygroundService::class.java)` với:
     + `action = PlaygroundService.ACTION_START`
     + `id = 35019`, `is_ongoing = false`, `oi_auto_dismiss_ms = 6000L`, `oi_scene = "NAVIGATION"`
     + `oi_template = 4`, `oi_right_template = 4`
     + `title = "Căn chỉnh Đảo"`, `text = "Kiểm tra tâm lỗ camera trước"`
     + `oi_left_content = "Origin"`, `oi_right_content = "Khớp tâm"`, `oi_island_click_expand = true`
   - Không import `OneUiOverlayIsland` trong `HomeScreen.kt`.
3. **4 Nâng Cấp Cốt Lõi `OneUiOverlayIsland.kt` & `PlaygroundService.kt`**:
   - Khóa tâm thực của vùng `Spacer` (`cutoutSpacerCenterOffsetPx` qua `onGloballyPositioned`) vào `cutoutCenterX + offsetX`.
   - `sendPendingIntentSafe`: từ API 34+ truyền `ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED`.
   - Ẩn tự động khi xoay ngang (`Configuration.ORIENTATION_LANDSCAPE`) nếu `oneui_island_hide_landscape == true`.
   - Cử chỉ tự nhiên: `FLAG_WATCH_OUTSIDE_TOUCH` (chạm ngoài thu gọn), vuốt lên thu gọn, vuốt ngang chuyển/đóng thẻ, hoãn tự ẩn 2s khi thẻ đang mở rộng.
4. **Đổi Vai Trò Coordinator `/swarm`**:
   - Chuyển quy chuẩn từ **Gemini 4 Argon** (`gemini-4-argon-medium`) sang **Gemini 4 Neon** (`gemini-4-neon-medium`) trong `C:\Users\Lenovo\.gemini\GEMINI.md`, `.agent/GEMINI.md` và `.agent/rules.md`.

## Ma Trận Phân Quyền Swarm (Coordinator: Gemini 4 Neon | Workers: Gemini 4 Hydrogen)
- [x] Task 1: Nâng `v2.1.1` (`20101`), hoàn thiện `OneUiOverlayIsland.kt` (khóa tâm Spacer, BAL API 34+, ẩn xoay ngang, cử chỉ chạm ngoài/vuốt lên/vuốt ngang, `isCardExpanded`) và `PlaygroundService.kt` (`onConfigurationChanged`, hoãn auto-dismiss khi đang mở) | [Agent: `oneui-overlay-polish` (`gemini-4-hydrogen-medium`, `share`)] | [Locked Files: `app/build.gradle.kts`, `app/src/main/java/com/originisle/android/island/OneUiOverlayIsland.kt`, `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`]
- [x] Task 2: Thêm `PREF_ONEUI_HIDE_LANDSCAPE` trong `Prefs.kt`, thêm nút & auto-preview căn chỉnh camera (`id = 35019`) + công tắc tự ẩn xoay ngang trong `HomeScreen.kt` | [Agent: `oneui-ui-preview` (`gemini-4-hydrogen-medium`, `share`)] | [Locked Files: `app/src/main/java/com/originisle/android/ui/Prefs.kt`, `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`]
- [x] Task 3: Cập nhật rule đổi Coordinator `/swarm` từ Argon sang Gemini 4 Neon (`gemini-4-neon-medium`) | [Agent: `rules-neon-sync` (`gemini-4-hydrogen-medium`, `inherit`)] | [Locked Files: `C:\Users\Lenovo\.gemini\GEMINI.md`, `.agent/GEMINI.md`, `.agent/rules.md`]
- [x] Task 4: Tích hợp 2 nhánh `share`, giữ rule & `task.md`, chạy `./gradlew.bat testDebugUnitTest assembleRelease`, xuất `release/OriginIsle-VN-v2.1.1.apk`, cập nhật `PROJECT_CONTEXT.md` & `task.md`, kiểm tra Secret Guard, commit & push lên `origin main` | [Agent: `verifier-v211` (`gemini-4-hydrogen-medium`, `inherit`)] | [Locked Files: `PROJECT_CONTEXT.md`, `task.md`, `release/OriginIsle-VN-v2.1.1.apk`]
