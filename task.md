# Kế Hoạch Hiển Thị Sóng Nhạc 5 Cột Màu Động — Origin Isle VN v2.10.4

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **`MediaCard.kt`**:
   - Bổ sung `createMusicWaveIcon(waveColors: List<String>, size: Int = 96): Icon` vẽ 5 cột sóng nhạc bo tròn viên thuốc (`0.36f, 0.72f, 0.52f, 0.88f, 0.44f`) căn giữa trên nền trong suốt, tô màu từ `waveColors` trích xuất theo ảnh bìa album.
   - Trong `dispatchMediaCard`: dùng `TEMPLATE_RIGHT_ISLAND_TEXT_ICON (4)` cho cả phát và tạm dừng:
     - `playing == true`: `oi_right_icon = createMusicWaveIcon(waveColors)`, `oi_show_right_icon = true`, `oi_right_content = ""`, `status_chip_text = "Playing"`.
     - `playing == false`: `oi_right_icon = Icon.createWithResource(context, R.drawable.ic_media_pause)`, `oi_show_right_icon = true`, `oi_right_content = ""`, `status_chip_text = " "`.
2. **`app/build.gradle.kts` & `PROJECT_CONTEXT.md`**:
   - Nâng `versionCode = 21004`, `versionName = "2.10.4"`.
   - Chạy `.\gradlew.bat testDebugUnitTest assembleRelease --console=plain`, xuất `release/OriginIsle-VN-v2.10.4.apk` và cập nhật `PROJECT_CONTEXT.md`.

## Ma Trận Phân Quyền Subagent (`Mode: SUBAGENT`)
- [x] Task 1: Thêm `createMusicWaveIcon` trong `MediaCard.kt`, nâng `v2.10.4` trong `app/build.gradle.kts`, chạy Gradle test + build APK release và cập nhật `PROJECT_CONTEXT.md` | [Agent: `Subagent-Music-Wave-Icon`] | [Skill: `originos-design`] | [Locked Files: `app/src/main/java/com/originisle/android/cards/MediaCard.kt`, `app/build.gradle.kts`, `PROJECT_CONTEXT.md`]
