# Kế Hoạch Khôi Phục Đảo Âm Nhạc — Origin Isle VN v2.10.8

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **`MediaCard.kt`**:
   - Đưa `com.spotify.music`, `com.spotify.music.lite`, `com.spotify.lite`, `com.spotify.music.canary`, `com.google.android.apps.youtube.music`, `com.apple.android.music`, `com.zing.mp3`, `com.vng.zingmp3`, `vn.com.vng.zingmp3` trở lại `THIRD_PARTY_MUSIC_PACKAGES`.
   - `ORIGIN_SUPPORTED_MUSIC_PACKAGES` chỉ giữ các app nhạc nội địa Trung Quốc thực sự có Đảo nhạc gốc OriginOS 6: `com.tencent.qqmusic`, `com.netease.cloudmusic`, `com.kugou.android`, `cn.kuwo.player`, `com.luna.music`.
   - Khôi phục `putExtra("oi_show_notify", true)` trong `dispatchMediaCard` (bắt buộc trên ROM OriginOS 6 để thẻ nhạc hiển thị trên Đảo).
   - Giữ nguyên sóng nhạc 5 cột `createMusicWaveIcon`, `status_chip_text = " "` (không chữ `"Playing"`), nút kính mờ `oi_btn_transparent = true` và `MediaIslandLaunchActivity` cho `source_content_intent`.
2. **`app/build.gradle.kts` & `PROJECT_CONTEXT.md`**:
   - Nâng `versionCode = 21008`, `versionName = "2.10.8"`.
   - Chạy `.\gradlew.bat testDebugUnitTest assembleRelease --console=plain`, xuất `release/OriginIsle-VN-v2.10.8.apk` và cập nhật `PROJECT_CONTEXT.md`.

## Ma Trận Phân Quyền Subagent (`Mode: SUBAGENT`)
- [x] Task 1: Sửa `MediaCard.kt`, nâng `v2.10.8` trong `app/build.gradle.kts`, chạy Gradle test + build APK release và cập nhật `PROJECT_CONTEXT.md` | [Agent: `Subagent-Restore-Music-Island`] | [Skill: `originos-design`] | [Locked Files: `app/src/main/java/com/originisle/android/cards/MediaCard.kt`, `app/build.gradle.kts`, `PROJECT_CONTEXT.md`]
