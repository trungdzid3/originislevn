# Kế Hoạch Điều Phối Swarm — Origin Isle VN v2.0.0

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT — SharedPreferences `"experimental_prefs"`)
- `cast_suppress_heads_up` (`Boolean`, default `false`): Bật/tắt chặn thông báo nổi (Heads-Up Pop-up) khi thông báo hiển thị trên Đảo.
- `cast_suppress_mode` (`String`, default `"SNOOZE"`): `"SNOOZE"` (ẩn pop-up nổi ngắn, giữ ngăn kéo thông báo) hoặc `"ISLAND_ONLY"` (chỉ hiện trên viên thuốc Đảo, tự ẩn thông báo phụ ở thanh trạng thái mà vẫn giữ thẻ Đảo).
- `cast_custom_light_color` (`String`, default `"AUTO"`): `"AUTO"` | `"NONE"` | `"#00D084"` | `"#0A59F7"` | `"#9D4EDD"` | `"#FFB020"`.
- `cast_island_duration_sec` (`Int`, default `5`): Thời lượng hiển thị nhanh viên thuốc (`3`, `5`, `7`, `10` giây).
- `cast_tap_expand_card` (`Boolean`, default `true`): `true` = chạm viên thuốc để mở rộng thẻ chi tiết (`oi_island_click_expand`), `false` = mở thẳng app gốc.

## Ma Trận Phân Quyền Swarm (Coordinator: Argon | Workers: Gemini 3.8 Flash)
- [x] Task 1: Đại tu giao diện Tai nghe Bluetooth 3D (xóa thanh pin xanh lá, đảo vị trí 2 icon tai nghe ở đảo thu gọn, sửa `OriginIslandBuilder` chống Binder overflow, vẽ hộp sạc 3D trắng ngọc trai phóng to kèm 2 icon tai nghe 3D trong thẻ mở rộng, bỏ chữ `xxx%` khổng lồ, hỗ trợ ẩn case / 1 bên tai) | [Agent: earbuds-3d (`gemini-3.8-flash-medium`)] | [Locked Files: `app/src/main/java/com/originisle/android/cards/BatteryCanvasRenderer.kt`, `app/src/main/java/com/originisle/android/cards/EarbudsCard.kt`, `app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt`]
- [x] Task 2: Triển khai Chặn Pop-up (Heads-Up / Island-Only Mode) và Engine tùy biến màu viền sáng / thời lượng / chạm viên thuốc 2.0.0 | [Agent: popup-blocker (`gemini-3.8-flash-medium`)] | [Locked Files: `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`, `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`]
- [x] Task 3: Xây dựng UI chính về Thông tin Thiết bị (Device Info Hero Dashboard) và Trung tâm Tùy biến sâu 2.0.0 với Animation mượt mà | [Agent: ui-20-studio (`gemini-3.8-flash-medium`)] | [Locked Files: `app/src/main/java/com/originisle/android/ui/DeviceInfoHelper.kt`, `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`, `app/src/main/java/com/originisle/android/ui/Components.kt`, `app/src/main/java/com/originisle/android/ui/Prefs.kt`]
- [x] Task 4: Tích hợp code từ 3 worker share, nâng version `2.0.0` (`20000`), chạy unit test + `assembleRelease`, xuất `release/OriginIsle-VN-v2.0.0.apk`, cập nhật `PROJECT_CONTEXT.md` | [Agent: verifier-qa (`gemini-3.8-flash-medium`)] | [Locked Files: `app/build.gradle.kts`, `PROJECT_CONTEXT.md`, `app/src/test/**`, `release/OriginIsle-VN-v2.0.0.apk`]
