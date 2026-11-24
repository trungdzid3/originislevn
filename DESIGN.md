# HỢP ĐỒNG THIẾT KẾ MATERIAL DESIGN 3 (SSOT) - ORIGIN ISLE VIỆT NAM
Chuẩn thiết kế: Google Material Design 3 (Material You Dynamic Color + Tonal Surface Containers)

---

## 1. TRIẾT LÝ THẨM MỸ & KHÔNG GIAN (MATERIAL YOU TONAL SYSTEM)
- **Tư tưởng cốt lõi**: Giao diện thuần Material Design 3 (M3), phân tầng chiều sâu bằng Tonal Surface Containers thay cho bóng đổ nặng nề, kết hợp Dynamic Color (Android 12+ / `Build.VERSION_CODES.S`) để đồng bộ hài hòa với hình nền hệ thống.
- **Bề mặt & Chiều sâu (Surface Roles)**:
  - **Background (`surface`)**: Nền tổng thể của màn hình (`colorScheme.surface`).
  - **Surface (`surfaceContainerLow`)**: Bề mặt thẻ nhóm `GroupCard`.
  - **SurfaceHigh (`surfaceContainer`)**: Bề mặt thẻ lồng nhau `NestedSettingContainer` và thanh công cụ phụ.
  - **SurfaceHigher (`surfaceContainerHigh`)**: Bề mặt chip chưa chọn, menu thả xuống và hộp thoại `UpdateDialog`.
  - **SwitchOff (`surfaceContainerHighest`)**: Rãnh công tắc khi tắt, nền preview.
  - **Viền Hairline (`0.5dp`)**: `outlineVariant.copy(alpha = 0.45f)` giúp tách bạch nhẹ nhàng các khối thẻ trên cả Dark Mode và Light Mode.

---

## 2. BẢNG MÀU DỰ ÁN (PROJECT PALETTE & SEMANTIC TOKENS)

### A. Dynamic Color & Tonal Fallback
- **Android 12+ (`SDK_INT >= S`)**: Tự động nạp `dynamicDarkColorScheme(context)` và `dynamicLightColorScheme(context)`.
- **Fallback Tonal Palette**: Cung cấp bộ `FallbackDarkColorScheme` và `FallbackLightColorScheme` sắc nét đầy đủ các vai trò `surface`, `surfaceContainerLowest` -> `surfaceContainerHighest`, `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `secondaryContainer`, `onSecondaryContainer`, `tertiaryContainer`, `error`, `errorContainer`, `onErrorContainer`, `outline`, `outlineVariant`.

### B. Ánh Xạ `IsleColorPalette` từ M3 `ColorScheme`
- **Accent & Interactive**: `accent = primary`, `glowAccent = primaryContainer`, `accentText = primary`, `borderActive = primary.copy(alpha = 0.35f)`.
- **Typography & Icons**: `text = onSurface`, `textSecondary = onSurfaceVariant`, `textTertiary = onSurfaceVariant.copy(alpha = 0.75f)`, `textChip = onSecondaryContainer`, `chevron = onSurfaceVariant`.
- **Dividers & Hairlines**: `borderHairline = outlineVariant.copy(alpha = 0.45f)`, `divider = outlineVariant.copy(alpha = 0.4f)`.
- **Trạng thái (Semantic Status)**:
  - **Thành công (`good*`)**: `#00D084` (Dark) / `#00A86B` (Light) cùng các tonal container tương phản rõ ràng.
  - **Cảnh báo (`warn*`)**: `#FF9F2E` (Dark) / `#F57C00` (Light).
  - **Lỗi / Nguy hiểm (`bad*` / `danger`)**: Ánh xạ trực tiếp `error`, `errorContainer`, `onErrorContainer`.

---

## 3. HỆ THỐNG KIỂU CHỮ (TYPOGRAPHY)
- **Họ phông chữ**: Tôn trọng `VivoSans` / `FontFamily.SansSerif` và `fontScale` hệ thống.
- **Phân cấp**:
  - **Screen Title (`headlineLarge`)**: `28sp`, `FontWeight.Bold`, letter spacing `-0.5sp`.
  - **SectionLabel**: `13sp`, `FontWeight.SemiBold`, màu `TextSecondary`.
  - **SettingRow Title**: `16sp`, `FontWeight.SemiBold`, `Modifier.weight(1f)`.
  - **NavRow Value**: Giới hạn `maxLines = 1`, `TextOverflow.Ellipsis`, `widthIn(max = 136.dp)` tránh kéo giãn bố cục khi kiểm tra cập nhật.
  - **Subtitle**: `13sp`, `lineHeight = 18sp`, màu `TextSecondary`.

---

## 4. LINH KIỆN CHUẨN MATERIAL DESIGN 3 (COMPONENTS)
- **GroupCard**: Container `RoundedCornerShape(24.dp)`, nền `IsleColors.Surface` (`surfaceContainerLow`), viền `0.5.dp` `IsleColors.BorderHairline`.
- **IconTile**: Huy hiệu tonal container `38.dp`, bo góc `12.dp`, nền `color.copy(alpha = 0.14f)`, chứa `Icon` kích thước `20.dp` màu `color`.
- **IsleSwitch**: Sử dụng `androidx.compose.material3.Switch` chính thức với `SwitchDefaults.colors(...)` ánh xạ `primary`, `onPrimary`, `surfaceContainerHighest`, `outline`.
- **SegmentedControl**: Dạng M3 Segmented Pill (`RoundedCornerShape(50)` cho track `SurfaceHigh` và mục đang chọn nền `secondaryContainer`, chữ `onSecondaryContainer`).
- **ChoiceChip**: Phong cách M3 FilterChip / Tonal Chip (`primaryContainer` / `onPrimaryContainer` khi chọn; `SurfaceHigher` / `TextSecondary` khi chưa chọn).
- **PillButton**: Mặc định dùng `primary` / `onPrimary` chuẩn M3 (vẫn hỗ trợ truyền `container`, `content` tùy biến).
- **UpdateDialog**: Hộp thoại M3 `RoundedCornerShape(28.dp)`, nền `surfaceContainerHigh`, thẻ changelog `surfaceContainer`, nút chính `primary` / `onPrimary`, nút phụ `secondaryContainer` / `onSecondaryContainer`.
- **NestedSettingContainer**: Card con `SurfaceHigh` bo góc `16.dp` kèm `AnimatedVisibility` mượt mà.

---

## 5. KIẾN TRÚC 3 TAB CHUẨN MATERIAL DESIGN 3 (`NavigationBar`)
- **Điều hướng chính**: `NavigationBar` cố định ở đáy `Scaffold` (`containerColor = IsleColors.Surface`, indicator `secondaryContainer` / `onSecondaryContainer`, hairline trên `0.5dp`) cho 3 đích cấp cao nhất; tự động ẩn khi mở màn hình con `AppsScreen` hoặc `ActivityScreen`.
1. **Tab 1 — Trang chủ (`HomeScreen.kt`)**:
   - Thiết kế tối giản vừa gọn ~1.5 màn hình: `MainScreenHeader` (kèm menu 3 chấm mở `ModalBottomSheet` Phòng thử nghiệm mẫu Đảo, Đóng tất cả thẻ, Chạy lại hướng dẫn), Banner cảnh báo quyền (tự ẩn hoàn toàn khi đủ quyền), `CompactStatusCard` (chấm trạng thái + tên thiết bị + công tắc tổng `cast_notifications`).
   - **Nhóm 1 — Giao diện & Tương tác**: Giao diện (`Sáng / Hệ thống / Tối`), Chặn Pop-up nổi (kèm `NestedSettingContainer` chọn chế độ + nút mở cài đặt thông báo hệ thống), Chạm viên thuốc mở rộng, Trả lời nhanh nổi.
   - **Nhóm 2 — Popup Bluetooth**: Công tắc Popup kết nối Bluetooth & `NavRow` tùy chỉnh tai nghe Bluetooth.
   - **Lối tắt nhanh**: 2 `TonalShortcutCard` ngang hàng mở `AppsScreen` và `ActivityScreen`.
2. **Tab 2 — Tính năng (`ModulesScreen.kt`)**:
   - **Nhóm 1 — Dịch vụ & Ứng dụng**: Gọi xe & Giao đồ ăn, Biến động số dư Ngân hàng, Cuộc gọi Zalo, Chuỗi TikTok (kèm hộp tin nhắn lồng nhau), Tải xuống & Quick Share, Nhắc lịch & Sự kiện, Cuộc gọi & Đàm thoại.
   - **Nhóm 2 — Hệ thống & Phần cứng**: Trình phát nhạc, Tai nghe & Thiết bị đeo, Pin & Sạc (kèm chọn thời gian hiển thị lồng nhau), Chế độ Âm thanh, Đèn pin, Chỉ báo Camera & Micro.
3. **Tab 3 — Cài đặt (`SettingsScreen.kt`)**:
   - **Nhóm 1 — Quy tắc & Riêng tư**: Mức lọc thông báo (`Quan trọng / Tất cả` + tự ẩn tin nhắn thường), Thời lượng viên thuốc (`3s / 5s / 7s / 10s`), Bỏ qua im lặng, Màn hình khóa, Ẩn số dư & OTP khi khóa, Theo Không làm phiền, Lịch giờ yên tĩnh (kèm 2 nút chọn giờ), Nút thao tác trên thẻ.
   - **Nhóm 2 — Giữ nền (Shizuku)**: Trạng thái & nút `Áp dụng` (`ShizukuKeepAliveHelper`) + Tự áp dụng sau khi khởi động lại.
   - **Nhóm 3 — Hệ thống & Cập nhật**: `NavRow` Quyền hệ thống (`PermissionsSheet`) & Kiểm tra cập nhật (`UpdateDialog`).
