# Kế Hoạch Sửa Lỗi Đảo Âm Nhạc (Zing MP3) — Origin Isle VN v2.10.1

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **`MediaCard.kt`**:
   - Khôi phục `chip = if (playing) "Playing" else " "` (không truyền chuỗi rỗng `""` vào `status_chip_text` / `oi_right_content` để tránh vivo SystemUI từ chối `capsule.content`).
   - Khi `!playing`: đặt `oi_right_template = OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON`, `oi_right_icon = Icon.createWithResource(context, R.drawable.ic_media_pause)`, `oi_show_right_icon = true`.
   - Đặt `oi_show_notify = true`, `oi_island_click_expand = true`, và dùng `source_content_intent = contentClickIntent` (Activity `PendingIntent` gốc, bỏ ghi đè `oi_capsule_click_resp`/`click_resp` bằng Broadcast `PendingIntent` để khớp `landingInfo` của vivo SuperX).
   - Khôi phục `makeAction` dùng `Icon.createWithResource(context, iconRes)` (0 KB Binder IPC), xóa `renderTintedBitmapIcon` nếu không còn dùng.
2. **`NotificationCastListener.kt`**:
   - Tại `onNotificationPosted`: chỉ bỏ qua sớm khi `sbn.tag == OriginIslandConstants.SUPERX_TAG` (tránh bỏ sót thông báo `MediaStyle` của Zing MP3 nếu hệ thống gắn sẵn metadata `BUNDLE_KEY_SCENE` / `ISLAND_NOTIFY`).
3. **`PlaygroundService.kt`**:
   - Khôi phục `val showNotify = intent.getBooleanExtra("oi_show_notify", true)` giống `fvhde/origin-isle`.
4. **Build & Release `v2.10.1` (`21001`)**:
   - Nâng `versionCode = 21001`, `versionName = "2.10.1"` trong `app/build.gradle.kts`.
   - Chạy `.\gradlew.bat testDebugUnitTest assembleRelease --console=plain`, xuất `release/OriginIsle-VN-v2.10.1.apk`, cập nhật `PROJECT_CONTEXT.md`.

## Ma Trận Phân Quyền Subagent (`Mode: SUBAGENT`)
- [x] Task 1: Sửa `MediaCard.kt`, `NotificationCastListener.kt`, `PlaygroundService.kt`, nâng `v2.10.1` trong `app/build.gradle.kts`, chạy Gradle test + build release APK và cập nhật `PROJECT_CONTEXT.md` | [Agent: `Subagent-Fix-ZingMP3-Media`] | [Skill: `impeccable`] | [Locked Files: `app/src/main/java/com/originisle/android/cards/MediaCard.kt`, `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`, `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`, `app/build.gradle.kts`, `PROJECT_CONTEXT.md`]
