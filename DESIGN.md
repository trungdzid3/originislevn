# HỢP ĐỒNG THIẾT KẾ ORIGINOS 6–7 (SSOT) — ORIGIN ISLE VIỆT NAM
Chuẩn thiết kế: **vivo OriginOS 6 (Space System 2.0)** & **OriginOS 7 (Liquid Glass / Atomic Motion 7.0)**

---

## 1. TRIẾT LÝ KHÔNG GIAN & THẨM MỸ (SPACE SYSTEM 2.0)

- **Tư tưởng cốt lõi**: Chuyển dịch từ giao diện phẳng phân tầng bằng màu sắc sang **giao diện mô phỏng vật lý thực**, lấy **ánh sáng, chiều sâu không gian (Z-axis) và động lực học chất lỏng** làm trục dẫn dắt.
- **Thanh trạng thái siêu không ranh giới**: Kích hoạt `WindowCompat.setDecorFitsSystemWindows(window, false)` với `statusBarColor = Color.Transparent` và `navigationBarColor = Color.Transparent`. Nền `OriginSpaceBackground` kéo dài quầng sáng đỉnh màn hình hòa liền mạch vào thanh trạng thái, loại bỏ hoàn toàn vạch chia thô.
- **Phân tầng Bề mặt 3 lớp (`--os-surface-tier`)**:
  1. **Tầng Nền (Base Space)**: `surface` kết hợp quầng `AmbientGlow` dịu ở đỉnh màn hình.
  2. **Tầng Thẻ Giữa (Mid-tier Liquid Glass Cards)**: `surfaceContainerLow` cho `GroupCard` và `surfaceContainer` cho `NestedSettingContainer`.
  3. **Tầng Nổi Đỉnh (Floating Overlay / Bar)**: `FloatingGlassBg` (`#E6141822` ở Dark / `#EBFFFFFF` ở Light) cho `OriginFloatingNavBar`, và `#F4141824` (Dark) / `#F8FFFFFF` (Light) với 4 ánh sáng vật lý (`Edge Light`, `Top Specular Highlight`, `Ambient Light`, `Touch Light`) cho `BluetoothPopupOverlay` (tuyệt đối không dùng `FLAG_BLUR_BEHIND`).

---

## 2. HỆ THỐNG VẬT LIỆU & ÁNH SÁNG (LIQUID GLASS & 4 LOẠI ÁNH SÁNG)

### A. Vật liệu Kính Lỏng (Liquid Glass) & Hạ cấp Phần cứng (Hardware Fallback)
- Hỗ trợ 3 mức sắc thái bề mặt (*Realistic / Natural / Soft*) dựa trên độ trong suốt và độ tương phản nền.
- **Hardware Fallback (Phân mảnh phần cứng)**: Giao diện không phụ thuộc sống còn vào hiệu ứng blur GPU nặng. Trên thiết bị tầm trung hoặc khi hệ thống giảm hiệu ứng mờ, các bề mặt kính tự động hạ cấp mạch lạc sang nền mờ đục / nền đặc (`FallbackDarkColorScheme`, `FallbackLightColorScheme`) mà vẫn giữ nguyên cấu trúc phân cấp bằng khoảng trắng, viền Edge Light và độ tương phản chữ.

### B. Hệ thống 4 Ánh sáng Vật lý
1. **Edge Light (`--os-light-edge`)**: Thay thế bóng đổ đen đặc bằng viền sáng mảnh theo hướng ánh sáng trên-xuống (`Brush.verticalGradient` từ `EdgeLightTop` -> `EdgeLightBottom`):
   - `EdgeLightTop`: `Color.White.copy(alpha = 0.22f)` (Dark) / `Color.White.copy(alpha = 0.85f)` (Light).
   - `EdgeLightBottom`: `Color.White.copy(alpha = 0.04f)` (Dark) / `Color.Black.copy(alpha = 0.06f)` (Light).
2. **Ambient Light (`AmbientGlow`)**: Quầng sáng môi trường tỏa dịu từ đỉnh `OriginSpaceBackground` (`accent.copy(alpha = 0.14f)` ở Dark / `accent.copy(alpha = 0.10f)` ở Light), đồng bộ với màu nhấn động (`AppThemeColor` / Monet).
3. **Touch Light & Đàn hồi Vật lý**: Mọi thành phần tương tác (`PillButton`, `SegmentedControl`, `ChoiceChip`, `TonalShortcutCard`) phản hồi chạm ngay ở khung hình đầu tiên (*First-frame Interruptible*) với độ co đàn hồi `0.97f` theo `spring`.
4. **Local Lift Under Text**: Chữ và icon đặt trên bề mặt kính lỏng luôn đảm bảo tương phản cao (`onSurface`, `onSurfaceVariant`) trên cả Dark Mode và Light Mode.

---

## 3. HỌ ĐƯỜNG CONG SQUIRCLE ĐỒNG TÂM (`IsleShapes`)

- Toàn bộ bo góc tuân thủ nguyên tắc **Squircle đồng tâm** (bo góc ngoài lớn hơn bo góc thành phần lồng bên trong):
  - `extraLarge`: `30.dp` (khung lớn, `OriginFloatingNavBar`, Bottom Sheet `32.dp`).
  - `large`: `26.dp` (`GroupCard`).
  - `medium`: `18.dp` (`NestedSettingContainer`, `TonalShortcutCard`).
  - `small`: `14.dp` (`IconTile`, `ChoiceChip`, thành phần lồng trong).
  - `pill`: `999.dp` / `50%` (`IsleSwitch`, `PillButton`, `SegmentedControl`, viên pin Bluetooth).

---

## 4. BẢNG MÀU DỰ ÁN & ĐỒNG BỘ POPUP (`Theme.kt` & `BluetoothPopupOverlay.kt`)

- **Dynamic Color & Preset `AppThemeColor`**:
  - Mặc định `AppThemeColor.DYNAMIC` (`#00BFA5`) trích xuất màu Monet trên Android 12+ (`SDK_INT >= S`).
  - Các preset chuẩn: `BLUE` (`#2563EB`), `TEAL` (`#00897B`), `VIOLET` (`#7C4DFF`), `ROSE` (`#D84374`), `AMBER` (`#D97706`).
- **Semantic Status**:
  - Thành công (`good*`): `#00D084` (Dark) / `#00A86B` (Light).
  - Cảnh báo (`warn*`): `#FF9F2E` (Dark) / `#F57C00` (Light).
  - Lỗi (`bad*` / `danger`): Ánh xạ trực tiếp `error`, `errorContainer`, `onErrorContainer`.
- **Popup Bluetooth (`BluetoothPopupOverlay.kt`)**:
  - **Bottom Sheet Bám Đáy (`32.dp`)**: Bám sát cạnh dưới (`MATCH_PARENT` trên điện thoại, giới hạn `min(screenWidth, 480.dp)` trên màn rộng), bo 2 góc trên `32.dp` và 2 góc dưới `0.dp`, padding đáy `navBarBottom + 18.dp`; chỉ sử dụng `FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS` (`PixelFormat.TRANSLUCENT`), **tuyệt đối không dùng `FLAG_BLUR_BEHIND` hay `blurBehindRadius`** (tránh bị OriginOS 6 ẩn/chặn cửa sổ overlay).
  - **Bề mặt Kính Lỏng & 4 Ánh sáng Vật lý**: Nền kính cân bằng tương phản cao `#F4141824` (Dark) / `#F8FFFFFF` (Light); **Edge Light** chuyển sắc trên-xuống `1.2.dp` kèm đường **Top Specular Highlight** `0.9.dp` mảnh mềm dọc cạnh trên giữa 2 góc bo; quầng **Ambient Light** (`RadialGradient`) tỏa mềm từ vùng giữa-trên thẻ phía sau khu vực tai nghe 3D theo `colorScheme.primary` (`0.14f` Dark / `0.08f` Light); **Touch Light** co đàn hồi `0.95f` + `alpha 0.88f` -> `OvershootInterpolator(1.05f)` trên nút tròn `"i"` (`46.dp x 46.dp`) và nút viên thuốc `"Hoàn tất"` (`height = 48.dp`).
  - **Header `vivo Sans` & Huy hiệu Trạng thái**: Thanh kéo mỏng `38.dp x 4.dp`, huy hiệu trạng thái kính mỏng nằm trên tiêu đề (`#00D084` `7.dp` + `"Đã kết nối"` `11.5.sp`), tiêu đề `23.sp` đậm `includeFontPadding = true` (`lineSpacing 3.dp / 1.05x`) bảo toàn dấu kép tiếng Việt (`ệ`, `ổ`, `ữ`), subtitle gọn gàng `13.sp` `onSurfaceVariant`.
  - **Huarong Atomic Battery Pods (`18.dp`, 2 dòng) & Atomic Motion 7.0**: Mỗi mục pin TWS là một Squircle đồng tâm bo 4 góc `18.dp` (`LiquidGlassSheetDrawable`) với nền kính nhẹ (`0x16FFFFFF` Dark / `0xC8F3F5FA` Light) và viền `Edge Light`, hiển thị 2 dòng (trên: icon `22.dp x 11.dp` + phần trăm `14.sp` đậm `tnum`; dưới: nhãn `11.sp` `"Tai trái"`, `"Hộp sạc"`, `"Tai phải"` hoặc `"Pin tai nghe"`), chia đều ngang (`weight = 1f` khi có 2–3 ô, tối đa `116.dp` khi chỉ có 1 ô). Hỗ trợ **First-frame Interruptible** (`ACTION_DOWN` hủy animation ngay khung đầu trên sheet và scrim, kéo lên trên có sức căng bề mặt `0.16f` rồi bật hồi êm), bảo toàn 3D `WebView`, hoạt ảnh Lenovo LE208 và giải mã ảnh/GIF.

---

## 5. HỆ THỐNG KIỂU CHỮ (`vivo Sans`)

- **Họ phông chữ**: Ưu tiên `vivo Sans` / `FontFamily.SansSerif`, tôn trọng `fontScale` hệ thống. Kiểm thử kỹ dấu thanh kép tiếng Việt (`ệ`, `ổ`, `ữ`) ở cỡ chữ nhỏ và weight đậm để tránh chạm dấu hoặc cắt ngọn.
- **Phân cấp hiển thị**:
  - **Screen Title (`headlineLarge`)**: `28sp`, `FontWeight.Bold`, letter spacing `-0.5sp`.
  - **SectionLabel**: `13sp`, `FontWeight.SemiBold`, màu `TextSecondary`.
  - **SettingRow Title**: `16sp`, `FontWeight.SemiBold`, `Modifier.weight(1f)`.
  - **NavRow Value**: `maxLines = 1`, `TextOverflow.Ellipsis`, `widthIn(max = 136.dp)`.
  - **Subtitle**: `13sp`, `lineHeight = 18sp`, màu `TextSecondary`.

---

## 6. LINH KIỆN CHUẨN ORIGINOS (`Components.kt`)

- **`OriginSpaceBackground`**: Khung nền Space System 2.0 với quầng `AmbientGlow` ở đỉnh màn hình tràn liền mạch lên Status Bar.
- **`OriginFloatingNavBar` (Liquid Glass Dock + Liquid Lens)**: Thanh điều hướng kính lỏng nổi lơ lửng ở đáy màn hình (`FloatingGlassBg` + `navigationBarsPadding()`), sử dụng **viền Edge Light 2 lớp kèm đường specular highlight `0.5.dp` ở cạnh trên**. Viên thuốc chỉ báo **Liquid Lens** trượt mượt giữa các tab theo `spring(dampingRatio = Spring.DampingRatioLowBouncy / 0.72f, stiffness = Spring.StiffnessMediumLow)` (*First-frame Interruptible*) kết hợp phản hồi co đàn hồi *Touch Light* (`0.96f`).
- **`CompactStatusCard` (Huarong Grid Atomic Status Widget)**: Thẻ trạng thái nguyên tử không tầng (*zero-tier*) chuẩn Lưới Hoa Dung trong khung squircle `26.dp` có viền `Edge Light`, gom gọn: huy hiệu trạng thái kết nối dạng pill, tên thiết bị + phiên bản, dải thông số lượt thẻ Đảo trong ngày (`CastLog.entries.size` với chữ số `tnum`), và công tắc tổng `IsleSwitch` thao tác trực tiếp tại chỗ.
- **`AtomicShortcutTile` (Lưới Atomic 1x2)**: Cặp ô lối tắt nhanh (`Ứng dụng` & `Nhật ký`) cân đối theo lưới `1x2`, tuân thủ **Squircle đồng tâm** (`26.dp` khung ngoài, `14.dp` ô icon bên trong) kèm viền `Edge Light` và phản hồi chạm *Touch Light* (`0.96f` spring + ánh sáng chạm nhẹ).
- **`MediaIslandOverlay` (Liquid Glass Media Overlay)**: Thẻ phát nhạc nổi (`WindowManager`) bo góc `30.dp` sử dụng **viền Edge Light chuyển sắc từ trên xuống dưới** (`LayerDrawable` `TOP_BOTTOM`) và phản hồi *Touch Light* co nảy (`0.90f` -> `1f` với `OvershootInterpolator`) trên 3 nút điều khiển `Prev` / `Play-Pause` / `Next`.
- **`GroupCard`**: Thẻ nhóm bo góc squircle `26.dp`, nền kính phân tầng kèm viền sáng `Edge Light` (`EdgeLightTop` -> `EdgeLightBottom`).
- **`IsleSwitch`**: Pill Switch đặc trưng OriginOS (`50.dp x 28.dp`), núm tròn trắng chuyển động `spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)`, *First-frame Interruptible*.
- **`IconTile`**: Huy hiệu squircle `38.dp`, bo góc `12.dp`–`14.dp`, nền `color.copy(alpha = 0.14f)`, icon `20.dp` đồng bộ nét.
- **`PillButton`**: Nút viên thuốc bo tròn hoàn toàn, hiệu ứng nhấn đàn hồi *Touch Light* (`0.97f`), hỗ trợ `container` / `content` tùy biến.
- **`SegmentedControl` & `ChoiceChip`**: Bo góc viên thuốc / squircle, viền `Edge Light`, phản hồi chạm đàn hồi.
- **`NestedSettingContainer`**: Thẻ lồng nhau `18.dp` kèm `AnimatedVisibility`.

---

## 7. KIẾN TRÚC 3 TAB & THÍCH ỨNG MÀN HÌNH GẬP (`4:3`)

- **Điều hướng chính**: `OriginFloatingNavBar` nổi ở `Alignment.BottomCenter` khi `onboardingDone && !isSubPage`; tự động ẩn khi mở `AppsScreen` hoặc `ActivityScreen`. Các màn hình cuộn (`HomeScreen`, `ModulesScreen`, `SettingsScreen`) luôn chừa `contentPadding` đáy tối thiểu `112.dp` để nội dung cuối không bị thanh nổi che khuất.
- **3 Đích cấp cao nhất**:
  1. **Tab 1 — Trang chủ (`HomeScreen.kt`)**: `MainScreenHeader`, Banner cảnh báo quyền (tự ẩn khi đủ quyền), `CompactStatusCard` (Huarong Grid Atomic Status Widget), Nhóm Giao diện & Tương tác, cặp `AtomicShortcutTile` 1x2 mở `AppsScreen` và `ActivityScreen`.
  2. **Tab 2 — Tính năng (`ModulesScreen.kt`)**: Nhóm Dịch vụ & Ứng dụng và Nhóm Hệ thống & Phần cứng.
  3. **Tab 3 — Cài đặt (`SettingsScreen.kt`)**: Nhóm Quy tắc & Riêng tư, Nhóm Giữ nền (`ShizukuKeepAliveHelper`), Nhóm Hệ thống & Cập nhật (`PermissionsSheet`, `UpdateDialog`).
- **Thích ứng Màn hình Gập & Tỷ lệ `4:3`**:
  - Hỗ trợ co giãn động mượt mà, giữ nguyên trạng thái trang và dữ liệu đang nhập, không tạo viền đen 2 bên.
  - Khi màn hình mở rộng vượt ngưỡng tỷ lệ `4:3` (hoặc tablet), giới hạn chiều rộng nội dung tối đa `widthIn(max = 760.dp)` căn giữa cân đối và không đặt popup/dialog cắt ngang trục bản lề.

---

## 8. QUY TẮC CHỐNG LỖI THIẾT KẾ (APP ANTI-PATTERNS)

- Cấm sử dụng `NavigationBar` phẳng mặc định của M3 kèm vạch chia `HorizontalDivider` ở đáy màn hình.
- Cấm dùng `androidx.compose.material3.Switch` thô thay cho `IsleSwitch` đàn hồi OriginOS.
- Cấm khóa cảm ứng chờ animation kết thúc; mọi chuyển động phải ngắt được ngay ở khung hình đầu tiên.
- Cấm dùng emoji hoặc icon sticker màu sắc trong giao diện và tài liệu.
