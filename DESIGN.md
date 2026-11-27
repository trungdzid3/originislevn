# Hợp Đồng Thiết Kế Giao Diện - OriginIsle VN (DESIGN.md SSOT)

> **Chuẩn tham chiếu**: OriginOS 6 / OriginUI ROM 16.0 (`V2318` Android 16) kết hợp hệ thống token & thành phần thực tế từ `extracted-rom16-specs.md` và `originui-components.md`.

---

## 1. Atmosphere & Aesthetic (Không khí & Thẩm mỹ chủ đạo)
- **Phong cách**: **OriginUI ROM 16.0 (Clean Atmospheric Glass & G2 Squircle)** — tối giản, khoảng thở lớn, phân lớp bề mặt rõ ràng bằng độ tương phản nền-thẻ (`#F2F2F5` vs `#FFFFFF` ở Light Mode; `#0D0F14` vs `#171A21` ở Dark Mode), đường phân cách siêu mảnh (`0.5.dp`), và hiệu ứng bo góc liên tục G2 (`k = radius * 0.22`).
- **Nguyên tắc thị giác**:
  - Loại bỏ cảm giác "Material 3 mặc định" (tím/xanh xám đục). Ưu tiên nền sáng tinh khiết `#F2F2F5` + thẻ trắng `#FFFFFF` nổi bật rõ rệt, và nền tối sâu `#0D0F14` + thẻ `#171A21` có viền ánh sáng cạnh (`Edge Light`) tinh tế.
  - Thao tác mượt mà với spring physics (`emil-design-eng`), không giật cục.

---

## 2. Project Palette (Bảng màu Dự án - ROM 16.0)

### 2.1 Light Mode (OriginUI ROM 16.0 Chuẩn)
| Vai trò | Giá trị Hex | Ghi chú |
|---|---|---|
| Page Background (`bg`) | `#F2F2F5` | Nền trang tổng thể |
| Card / Surface (`card`) | `#FFFFFF` | Thẻ nhóm, Bottom Navigation, Sheet |
| Surface Variant (`cardAlt`) | `#F7F8FA` | Thẻ phụ, vùng lồng |
| Input / Chip Fill (`chipBg`) | `#0D000000` | Ô tìm kiếm `VSearchView2`, nút AlphaGray, Chip chưa chọn |
| Primary Accent (`accent`) | `#0F77FF` | Điểm nhấn mặc định OriginUI (hỗ trợ Dynamic Color / Preset) |
| Primary Alpha Fill (`accentSoft`) | `#1A0F77FF` | Nền nút AlphaBlue, pill đang chọn |
| Action Link (`link`) | `#456FFF` | Liên kết trong `VTipCard` |
| Destructive (`error`) | `#FF3636` | Cảnh báo, xóa, trạng thái lỗi |
| Success (`ok`) | `#23B26D` | Trạng thái hoạt động tốt |
| Warning (`warn`) | `#FF9500` | Cảnh báo nhẹ |
| Text Primary (`textPrimary`) | `#E6000000` | Tiêu đề, nội dung chính (90% đen) |
| Text Secondary (`textSecondary`) | `#CC000000` | Mô tả phụ `g2` (80% đen) |
| Text Sub-item (`textSub`) | `#78000000` | Phụ đề hàng `VListItem` (47% đen) |
| Text Muted (`textMuted`) | `#59000000` | Nhãn tab không chọn, gợi ý `g5` (35% đen) |
| Hairline Divider (`divider`) | `#14000000` | Đường kẻ phân cách `0.5.dp` (8% đen) |

### 2.2 Dark Mode (OriginUI Dark & Edge Light)
| Vai trò | Giá trị Hex | Ghi chú |
|---|---|---|
| Page Background (`bg`) | `#0D0F14` | Nền tối sâu OriginOS |
| Card / Surface (`card`) | `#171A21` | Thẻ nhóm, Sheet tối |
| Surface Variant (`cardAlt`) | `#1E222B` | Thẻ phụ, vùng lồng |
| Input / Chip Fill (`chipBg`) | `#14FFFFFF` | Ô tìm kiếm, nút phụ |
| Primary Accent (`accent`) | `#2E8BFF` | Xanh sáng tối ưu độ tương phản nền tối |
| Primary Alpha Fill (`accentSoft`) | `#260F77FF` | Nền pill nhấn nhẹ |
| Text Primary (`textPrimary`) | `#F2F4F8` | Chữ chính |
| Text Secondary (`textSecondary`) | `#B3F2F4F8` | Chữ phụ |
| Text Muted (`textMuted`) | `#73F2F4F8` | Nhãn mờ |
| Hairline Divider (`divider`) | `#1FFFFFFF` | Viền mảnh `0.5.dp` |
| Nav Bar Floating (`navBg`) | `#EB141822` | Thanh điều hướng nổi bán trong suốt + Edge Light |

---

## 3. Fillet / Border Radius & Layout Tokens (ROM 16.0)
- **Horizontal Screen / Card Margin**: `14.dp` (`ScreenPadding`) cho khoảng cách lề tiêu chuẩn của thẻ nhóm `VListItem` / `GroupCard`.
- **Corner Radii (Fillet System + G2 Squircle)**:
  - Áp dụng **`G2RoundedCornerShape`** (đường cong liên tục G2 bậc 4 / superellipse `|x|^4 + |y|^4 = r^4`) cho mọi thẻ, nút, thanh tìm kiếm và hộp thoại.
  - `S = 12.dp`: `VTipCard` (`WarningBanner`), thumbnail, tile nhỏ.
  - `M = 18.dp`–`20.dp`: Thẻ phụ lồng trong, `VSearchView2` (`20.dp`).
  - `L = 24.dp`: `GroupCard` chính (`GroupRadius`), `Button` chuẩn (`24.dp`), `BentoTile`.
  - `XL = 30.dp`–`32.dp`: `VDialog`, `ModalBottomSheet` (`30.dp`), `HeroCard` (`30.dp`), `Button` lớn (`32.dp` / `CircleShape`).
- **Collapsing Toolbar (OriginOS Settings Style)**:
  - Tiêu đề trang mở rộng `28.sp` (SemiBold/Bold, tracking `-0.5.sp`), tự động thu gọn mượt thành `18.sp` trên thanh tiêu đề ghim đỉnh khi người dùng cuộn danh sách (`OriginCollapsingScaffold`).
- **Hairline Divider**: `0.5.dp` (`divider`), thụt lề trái `16.dp` (hoặc thẳng hàng với text khi có icon) bên trong `GroupCard`.

---

## 4. Typography Scale (Phân cấp chữ ROM 16.0)
- **Font Family**: `VivoSans` (`vivo-sans` system với fallback `FontFamily.SansSerif`) cho an toàn dấu tiếng Việt kép (`ệ, ổ, ữ, đ`), kết hợp `JakartaSans` nếu cần.
- **Kích thước chuẩn**:
  - Toolbar Expanded Title: `28.sp` (SemiBold/Bold, tracking `-0.5.sp`)
  - Toolbar Collapsed Title: `18.sp` (SemiBold)
  - Section Header / Group Label: `13.sp` (Medium, `textMuted`)
  - ListItem Title / Input (`VListItem`): `16.sp` (Medium, `textPrimary`)
  - Tab / Toolbar Button: `15.sp` (Medium)
  - `VTipCard` Title: `14.sp` (SemiBold)
  - `VTipCard` Body / ListItem Subtitle: `12.sp`–`13.sp` (`textSub` / `textMuted`)
  - Search Tag / Caption: `12.sp`

---

## 5. OriginUI Component Contracts (Bắt buộc đồng bộ)

1. **`MoveBoolButton` (`IsleSwitch`)**:
   - Kích thước track: `44.dp x 28.dp` (`vos7_0`), bo tròn hoàn toàn.
   - Trạng thái **OFF**: Track `#59CCCCCC` (Light) / `#75464646` (Dark), thumb là **vòng tròn rỗng (hollow ring)** viền `2.5.dp`, đường kính ~`18.dp`, màu trắng (Light) hoặc `#E0E0E0` (Dark).
   - Trạng thái **ON**: Track `accent.copy(alpha = 0.22f)` (Light `#330F77FF`) hoặc `accent.copy(alpha = 0.35f)` (Dark), thumb là **hình tròn đặc** màu trắng/sáng có viền halo/diffuse glow màu `accent`.
2. **`VListItem` & `GroupCard`**:
   - Bo góc `24.dp`, nền `#FFFFFF` (Light) / `#171A21` (Dark), lề ngang `14.dp`.
   - Ngăn cách bằng đường `0.5.dp` (`divider`).
3. **`VTipCard` (`WarningBanner`)**:
   - Bo góc `12.dp`, nền `#FFFFFF` hoặc `#08000000` (Light) / `#1E222B` (Dark), chữ tiêu đề `14.sp`, nội dung `12.sp`, nút hành động màu `#456FFF` (hoặc semantic accent khi cảnh báo lỗi).
4. **`VSearchView2`**:
   - Chiều cao `44.dp`–`48.dp`, bo góc `20.dp`, nền `#0D000000` (Light) / `#14FFFFFF` (Dark), không viền thô.
5. **`Button` (`PillButton`)**:
   - Chiều cao tối thiểu `50.dp` (Large), bo góc `24.dp`–`32.dp`.
   - Biến thể: Primary (`#0F77FF`, chữ trắng), AlphaBlue (`#1A0F77FF`, chữ `#0F77FF`), AlphaGray (`#0D000000`, chữ `#E6000000`).
6. **`VDialog` & Bottom Sheet**:
   - Bo góc `30.dp`, nền `#FFFFFF` (Light) / `#171A21` (Dark), nút hành động chuẩn OriginUI `50.dp`.
7. **`BottomNavigationView` (`OriginFloatingNavBar`)**:
   - Light Mode: Nền `#FFFFFF` / `#F2F2F5` kết hợp viền mảnh `#14000000`, mục chọn `#0F77FF`, mục chưa chọn `#59000000`.
   - Dark Mode: `#EB141822` + Edge Light `#29FFFFFF`.

---

## 6. Role-Based UI Constraints & App Anti-Patterns
- **Tuyệt đối không**: Dùng Emoji làm icon UI (chỉ dùng `Icons.Rounded.*` hoặc vector custom).
- **Tuyệt đối không**: Thay đổi signature public của các Composable dùng chung (`IsleSwitch`, `GroupCard`, `PillButton`, `WarningBanner`, `SettingRow`, `ToggleRow`, `NavRow`, `OriginFloatingNavBar`, `MainScreenHeader`, `ScreenHeader`, `CompactStatusCard`, `AtomicShortcutTile`) để không làm gãy các màn hình đang hoạt động.
- **Tuyệt đối không**: Sửa `applicationId = "com.autonavi.minimap"`.
