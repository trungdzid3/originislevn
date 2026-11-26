# Kế Hoạch Điều Khiển Nhạc Kính Mờ Qua Activity — Origin Isle VN v2.10.7

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **`MediaIslandLaunchActivity.kt`** (`app/src/main/java/com/originisle/android/service/MediaIslandLaunchActivity.kt`):
   - Extends `Activity`, không animation (`overridePendingTransition(0, 0)` / `overrideActivityTransition` trên API 34+).
   - Trong `onCreate`: gọi `MediaIslandOverlay.show(applicationContext)`. Nếu trả về `false` (thiếu quyền Overlay hoặc chưa có snapshot), fallback mở `EXTRA_FALLBACK_INTENT` (`PendingIntent`) hoặc app nhạc từ `EXTRA_SOURCE_PKG`, rồi `finish()`.
2. **`AndroidManifest.xml`**:
   - Khai báo `<activity android:name=".service.MediaIslandLaunchActivity" android:exported="false" android:theme="@android:style/Theme.Translucent.NoTitleBar" android:excludeFromRecents="true" android:noHistory="true" android:taskAffinity="" />`.
3. **`MediaCard.kt`**:
   - Giữ nguyên `oi_island_click_expand = true`, `oi_show_notify = false`, `status_chip_text = " "`, `TEMPLATE_RIGHT_ISLAND_TEXT_ICON` (sóng 5 cột `createMusicWaveIcon` / `ic_media_pause`).
   - Khi `useHybridOverlay == true` (`MediaIslandOverlay.canShow(context)` && `PREF_MEDIA_OVERLAY_EXPAND` bật): đặt `source_content_intent` là `PendingIntent.getActivity(..., MediaIslandLaunchActivity)` (Activity hợp lệ với SuperX) để chạm thẻ mở rộng bung bảng kính mờ `MediaIslandOverlay` (có `SeekBar` kéo tua + nút chuyển loa/tai nghe + 3 nút điều khiển). Chạm vào bìa album trong `MediaIslandOverlay` vẫn mở app nhạc nguồn qua `MediaSnapshot.contentIntent`.
4. **`app/build.gradle.kts` & `PROJECT_CONTEXT.md`**:
   - Nâng `versionCode = 21007`, `versionName = "2.10.7"`.
   - Chạy `.\gradlew.bat testDebugUnitTest assembleRelease --console=plain`, xuất `release/OriginIsle-VN-v2.10.7.apk`, cập nhật `PROJECT_CONTEXT.md`.

## Ma Trận Phân Quyền Subagent (`Mode: SUBAGENT`)
- [x] Task 1: Tạo `MediaIslandLaunchActivity.kt`, khai báo trong `AndroidManifest.xml`, nối `MediaCard.kt`, nâng `v2.10.7` trong `app/build.gradle.kts`, build APK release và cập nhật `PROJECT_CONTEXT.md` | [Agent: `Subagent-Media-Overlay-Activity`] | [Skill: `originos-design`] | [Locked Files: `app/src/main/java/com/originisle/android/service/MediaIslandLaunchActivity.kt`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/originisle/android/cards/MediaCard.kt`, `app/build.gradle.kts`, `PROJECT_CONTEXT.md`]
