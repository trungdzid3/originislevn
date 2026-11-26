# Kế Hoạch Sửa Lỗi Sóng Nhạc Trên Đảo Thu Nhỏ — Origin Isle VN v2.10.3

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **`OriginIslandBuilder.kt`**:
   - Tại nhánh `when (rightTemplate) { 1 -> ... }`: xóa dòng `right.putBoolean(C.BUNDLE_KEY_ISLAND_RIGHT_PLAY_PAG, waveState == 1)` để khớp 100% với `fvhde/origin-isle` (chỉ ghi `BUNDLE_KEY_ISLAND_RIGHT_WAVE_COLOR` và `BUNDLE_KEY_ISLAND_RIGHT_WAVE_STATE`).
2. **`MediaCard.kt`**:
   - Trong `post(...)`: bổ sung `PlaybackState.STATE_NONE` cùng nhánh `null` (`null, PlaybackState.STATE_NONE -> sbn.isOngoing || (n.flags and Notification.FLAG_ONGOING_EVENT) != 0`) để không đánh giá sai `playing = false` khi app nhạc chưa kịp gửi `PlaybackState`.
   - Trong `dispatchMediaCard`: khi `playing == true`, không gửi `oi_right_content` đè lên template sóng nhạc (`TEMPLATE_RIGHT_ISLAND_WAVE = 1`), chỉ giữ `status_chip_text = "Playing"`.
3. **`app/build.gradle.kts` & `PROJECT_CONTEXT.md`**:
   - Nâng `versionCode = 21003`, `versionName = "2.10.3"`.
   - Chạy `.\gradlew.bat testDebugUnitTest assembleRelease --console=plain`, xuất `release/OriginIsle-VN-v2.10.3.apk` và cập nhật `PROJECT_CONTEXT.md`.

## Ma Trận Phân Quyền Subagent (`Mode: SUBAGENT`)
- [x] Task 1: Sửa `OriginIslandBuilder.kt` & `MediaCard.kt`, nâng `v2.10.3` trong `app/build.gradle.kts`, chạy Gradle test + build APK release và cập nhật `PROJECT_CONTEXT.md` | [Agent: `Subagent-Fix-Music-Wave`] | [Skill: `originos-design`] | [Locked Files: `app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt`, `app/src/main/java/com/originisle/android/cards/MediaCard.kt`, `app/build.gradle.kts`, `PROJECT_CONTEXT.md`]
