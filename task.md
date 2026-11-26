# Kế Hoạch Đại Tu Giao Diện — OriginOS 6–7 (Space System 2.0 & Liquid Glass)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **`Theme.kt`**:
   - Giữ nguyên toàn bộ tên public API hiện có (`IsleColorPalette`, `LocalIsleColors`, `IsleColors`, `AppThemeColor`, `OriginIsleTheme`, `IsleShapes`, `resolveAppIsDarkTheme`, `resolveMaterialColorScheme`, `toIsleColorPalette`, `FallbackDarkColorScheme`, `FallbackLightColorScheme`) để không gãy bất kỳ màn hình nào.
   - Bổ sung token OriginOS 6–7 vào `IsleColors` (hoặc helper trong `Theme.kt`):
     - `val EdgeLightTop: Color` (ánh sáng viền cạnh trên sáng nhẹ: `Color.White.copy(alpha = 0.22f)` ở Dark / `Color.White.copy(alpha = 0.85f)` ở Light).
     - `val EdgeLightBottom: Color` (`Color.White.copy(alpha = 0.04f)` ở Dark / `Color.Black.copy(alpha = 0.06f)` ở Light).
     - `val FloatingGlassBg: Color` (`Color(0xE6141822)` ở Dark / `Color(0xEBFFFFFF)` ở Light).
     - `val AmbientGlow: Color` (`accent.copy(alpha = 0.14f)` ở Dark / `accent.copy(alpha = 0.10f)` ở Light).
   - Nâng cấp `IsleShapes` theo Squircle đồng tâm OriginOS 6–7 (`small = 14.dp`, `medium = 18.dp`, `large = 26.dp`, `extraLarge = 30.dp`).
   - Kích hoạt `WindowCompat.setDecorFitsSystemWindows(window, false)` và `statusBarColor = Color.Transparent` / `navigationBarColor = Color.Transparent` cho Thanh trạng thái siêu không ranh giới (超无界状态栏).
2. **`Components.kt`**:
   - Giữ nguyên signature tất cả component cũ (`GroupCard`, `IconTile`, `IsleSwitch`, `SegmentedControl`, `ChoiceChip`, `PillButton`, `TonalShortcutCard`, `MainScreenHeader`, `NestedSettingContainer`, `SettingRow`, `ToggleRow`, `NavRow`, `SectionLabel`, `HorizontalDivider`...).
   - Nâng cấp tạo hình sang **OriginOS 6–7 Liquid Glass & Edge Light**:
     - `GroupCard`: Bo góc squircle `26.dp`, nền kính phân tầng kèm viền sáng *Edge Light* (`Brush.verticalGradient` từ `IsleColors.EdgeLightTop` -> `IsleColors.EdgeLightBottom`).
     - `IsleSwitch`: Pill Switch đặc trưng OriginOS (`50.dp x 28.dp`), không dùng `material3.Switch` thô; núm tròn trắng mượt với `spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)`, *First-frame Interruptible* (không khóa cảm ứng).
     - `PillButton`, `SegmentedControl`, `ChoiceChip`, `TonalShortcutCard`: Áp dụng Edge Light, Squircle đồng tâm, hiệu ứng nhấn đàn hồi *Touch Light* (`animateFloatAsState` `0.97f` với `spring`).
   - Bổ sung 2 component mới trong `Components.kt`:
     - `OriginSpaceBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)`: Nền Space System 2.0 với quầng sáng Ambient Light dịu ở đỉnh màn hình tràn liền mạch lên Status Bar.
     - `OriginFloatingNavBar(tabs: List<OriginNavTab>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)` và `data class OriginNavTab(val label: String, val icon: ImageVector)`: Thanh điều hướng kính lỏng nổi lơ lửng ở đáy màn hình (tách khỏi đáy + chừa `navigationBarsPadding()`), viên thuốc chỉ báo trượt mượt theo `spring` (*Liquid Motion*).
3. **`MainActivity.kt` & Screens (`HomeScreen.kt`, `ModulesScreen.kt`, `SettingsScreen.kt`)**:
   - `MainActivity.kt`: Bỏ `NavigationBar` phẳng + `HorizontalDivider` của M3; đặt nội dung bên trong `OriginSpaceBackground`, hiển thị `OriginFloatingNavBar` nổi ở `Alignment.BottomCenter` khi `onboardingDone && !isSubPage`; hỗ trợ ngưỡng tỷ lệ `4:3` cho màn hình gập (`widthIn(max = 760.dp)` khi mở rộng).
   - `HomeScreen.kt`, `ModulesScreen.kt`, `SettingsScreen.kt`: Đảm bảo `contentPadding` đáy tối thiểu `112.dp` (hoặc `Spacer(Modifier.height(104.dp))` cuối danh sách cuộn) để nội dung cuối trang không bị `OriginFloatingNavBar` che khuất.
4. **`BluetoothPopupOverlay.kt` & `DESIGN.md`**:
   - `BluetoothPopupOverlay.kt`: Áp dụng bề mặt kính tối/sáng OriginOS (`#F2121620` / `#F5FFFFFF`), viền Edge Light, bo góc squircle `32.dp`, nút pill `"Hoàn tất"` chuẩn OriginOS.
   - `DESIGN.md`: Cập nhật toàn diện thành **Hợp Đồng Thiết Kế OriginOS 6–7 (Space System 2.0 & Liquid Glass)** (SSOT).

## Ma Trận Phân Quyền Subagent (`Mode: SUBAGENT`)
- [x] Task 1: Đại tu `Theme.kt` & `Components.kt` sang OriginOS 6–7 Space System 2.0 / Liquid Glass | [Agent: `OriginOS-Theme-And-Components`] | [Skill: `originos-design`] | [Locked Files: `app/src/main/java/com/originisle/android/ui/Theme.kt`, `app/src/main/java/com/originisle/android/ui/Components.kt`]
- [x] Task 2: Thay `NavigationBar` M3 bằng `OriginFloatingNavBar` & `OriginSpaceBackground` trong `MainActivity.kt`, cập nhật padding đáy & thích ứng trên `HomeScreen.kt`, `ModulesScreen.kt`, `SettingsScreen.kt` | [Agent: `OriginOS-Shell-And-Screens`] | [Skill: `originos-design`] | [Locked Files: `app/src/main/java/com/originisle/android/MainActivity.kt`, `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`, `app/src/main/java/com/originisle/android/ui/ModulesScreen.kt`, `app/src/main/java/com/originisle/android/ui/SettingsScreen.kt`]
- [x] Task 3: Đồng bộ `BluetoothPopupOverlay.kt` sang OriginOS Liquid Glass & viết lại `DESIGN.md` SSOT | [Agent: `OriginOS-Popup-And-SSOT`] | [Skill: `originos-design`] | [Locked Files: `app/src/main/java/com/originisle/android/bluetooth/BluetoothPopupOverlay.kt`, `DESIGN.md`]
- [x] Task 4: Nâng cấp 6 màn hình/dialog phụ (`AppsScreen.kt`, `ActivityScreen.kt`, `UpdateDialog.kt`, `PermissionsSheet.kt`, `OnboardingScreen.kt`, `BluetoothPopupSheet.kt`) sang OriginOS 6–7 Space System 2.0 & Liquid Glass | [Agent: `ui-secondary`] | [Skill: `originos-design`]
- [x] Task 5: Sửa nhận diện & fallback phát nhạc Zing MP3 lên Đảo Origin (`NotificationCastListener.kt`, `DownloadDetector.kt`, `MediaCard.kt`) | [Agent: `zing-fix`]
- [x] Task 6: Nâng `versionCode = 20900` / `versionName = "2.9.0"`, chạy `testDebugUnitTest` & `assembleRelease`, xuất bản `release/OriginIsle-VN-v2.9.0.apk` và cập nhật `PROJECT_CONTEXT.md` | [Agent: `verifier`]

## Đợt Nâng Cấp v2.10.0 — OriginOS 7 Liquid Glass & Huarong Grid + Media Island Music Fix
- [x] Task 7: Nâng cấp `Theme.kt` (lineHeight tiếng Việt) & `Components.kt` (Liquid Lens `OriginFloatingNavBar`, `OriginSpaceBackground` Ambient Light, Touch Light) | [Agent: `ui-liquid-glass`] | [Locked Files: `app/src/main/java/com/originisle/android/ui/Theme.kt`, `app/src/main/java/com/originisle/android/ui/Components.kt`]
- [x] Task 8: Nâng cấp Huarong Grid Atomic Status Widget + 1x2 `AtomicShortcutTile` trong `HomeScreen.kt`, Liquid Glass Edge Light & Touch Light trong `MediaIslandOverlay.kt`, cập nhật `DESIGN.md` | [Agent: `ui-huarong-home`] | [Locked Files: `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`, `app/src/main/java/com/originisle/android/ui/MediaIslandOverlay.kt`, `DESIGN.md`]
- [x] Task 9: Sửa lỗi Media Island không hiển thị khi phát nhạc (`NotificationCastListener.kt`, `MediaCard.kt`, `PlaygroundService.kt`) | [Agent: `media-debug-fix`] | [Locked Files: `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`, `app/src/main/java/com/originisle/android/cards/MediaCard.kt`, `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`]
- [x] Task 10: Nâng `versionCode = 21000` / `versionName = "2.10.0"`, chạy `testDebugUnitTest` & `assembleRelease`, xuất bản `release/OriginIsle-VN-v2.10.0.apk` và cập nhật `PROJECT_CONTEXT.md` & `task.md` | [Agent: `verifier-2`] | [Locked Files: `app/build.gradle.kts`, `PROJECT_CONTEXT.md`, `task.md`, `release/OriginIsle-VN-v2.10.0.apk`]
