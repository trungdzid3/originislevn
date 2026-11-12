# Kế Hoạch Điều Phối Swarm — Origin Isle VN v2.0.2 (Đại Tu Đảo Tai Nghe Bằng `oi_sub_image` Full-Width & Loại Bỏ Màu Viền)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **Loại bỏ hoàn toàn màu viền phát sáng mặc định**:
   - `Prefs.kt`: `DEFAULT_CUSTOM_LIGHT_COLOR = "NONE"`.
   - `PlaygroundService.kt`: `prefs.getString("cast_custom_light_color", "NONE")`, khi `"NONE"` thì `lightColor = 0` (tắt hoàn toàn `BUNDLE_KEY_EFFECT_IS_LIGHT`).
   - `EarbudsCard.kt`: Xóa bỏ `putExtra("oi_light_color", ...)` ở mọi trạng thái tai nghe/wearable.
2. **Đảo Mở Rộng Tai Nghe Full-Width Banner (`oi_sub_image` + `TEMPLATE_BASE = 4`)**:
   - Thay thế `TEMPLATE_TEXT_SYMMETRY = 3` (vốn bị Vivo ép kích thước theo mẫu tỷ số bóng đá nên icon bé tí) bằng `TEMPLATE_BASE = 4` kèm `oi_sub_image` (`BASE_SUB_INFO_IMAGE = 3`).
   - `BatteryCanvasRenderer.createEarbudsExpandedBanner(context: Context, leftPct: Int, rightPct: Int, casePct: Int, isLeftCharging: Boolean, isRightCharging: Boolean, isCaseCharging: Boolean): Icon`:
     + Vẽ Bitmap `600 x 210 px` (`ARGB_8888`, ~492 KB, an toàn tuyệt đối qua Binder IPC) trên nền trong suốt (`Color.TRANSPARENT`) với bo góc thẻ tối nhẹ `#1E2026` (`alpha 210`) để chữ trắng và đồ họa 3D nổi bật sắc nét trên cả Dark/Light theme.
     + Hỗ trợ đầy đủ 3 chế độ tự động:
       1. **Đủ 2 tai nghe + Hộp sạc (`hasLeft && hasRight && hasCase`)**: 3 cột lớn đối xứng: `[Tai nghe Trái 3D + "Trái XX%" + icon pin xanh #00D084]` | `[Hộp sạc 3D mở nắp LỚN + "Hộp sạc XX%" + icon pin xanh #00D084]` | `[Tai nghe Phải 3D + "Phải XX%" + icon pin xanh #00D084]`.
       2. **Đã lấy 2 tai nghe khỏi hộp (`hasLeft && hasRight && !hasCase`)**: 2 cột lớn cân đối: `[Tai nghe Trái 3D phóng to + "Trái XX%" + icon pin xanh]` | `[Tai nghe Phải 3D phóng to + "Phải XX%" + icon pin xanh]`.
       3. **Chỉ đeo 1 bên tai (`hasLeft != hasRight`)**: 1 cột tai nghe đang đeo + thông tin pin bên đó (kèm hộp sạc nếu `hasCase`).

## Ma Trận Phân Quyền Swarm (Coordinator: Argon | Workers: Gemini 3.8 Flash)
- [x] Task 1: Thêm `createEarbudsExpandedBanner(...)` (`600x210 px`) vào `BatteryCanvasRenderer.kt` vẽ 3D Pearl White sắc nét cho 3 kịch bản (3 cột có Case, 2 cột ẩn Case, 1 bên tai) kèm icon viên pin xanh `#00D084` ngang chuẩn mẫu Vivo | [Agent: banner-renderer (`gemini-3.8-flash-medium`)] | [Skill: hallmark] | [Locked Files: `app/src/main/java/com/originisle/android/cast/cards/BatteryCanvasRenderer.kt` -> thực tế `app/src/main/java/com/originisle/android/cards/BatteryCanvasRenderer.kt`]
- [x] Task 2: Chuyển `EarbudsCard.kt` sang `TEMPLATE_BASE = 4` + `oi_sub_image` (`createEarbudsExpandedBanner`), xóa `oi_light_color` trong `EarbudsCard.kt`, và đổi mặc định `cast_custom_light_color = "NONE"` trong `Prefs.kt`, `PlaygroundService.kt`, `HomeScreen.kt` | [Agent: no-border-earbuds (`gemini-3.8-flash-medium`)] | [Skill: impeccable] | [Locked Files: `app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`, `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`, `app/src/main/java/com/originisle/android/ui/Prefs.kt`, `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`]
- [x] Task 3: Tích hợp code từ 2 worker share, nâng version `2.0.2` (`20002`) trong `app/build.gradle.kts`, chạy `./gradlew.bat testDebugUnitTest assembleRelease`, xuất `release/OriginIsle-VN-v2.0.2.apk`, cập nhật `PROJECT_CONTEXT.md` & `task.md`, push GitHub & phát hành Release `v2.0.2` | [Agent: verifier-v202 (`gemini-3.8-flash-medium`)] | [Skill: pr-feedback-quality-gate] | [Locked Files: `app/build.gradle.kts`, `PROJECT_CONTEXT.md`, `task.md`, `release/OriginIsle-VN-v2.0.2.apk`]
