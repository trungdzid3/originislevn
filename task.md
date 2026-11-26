# Kế Hoạch Điều Phối Subagent — Origin Isle VN v2.8.5 (Đảo Âm Nhạc Hybrid OriginOS 6 & Đảo TikTok M3)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **`MediaControlReceiver.kt`**:
   - Hằng số `const val ACTION_OPEN_MEDIA_ISLAND = "com.originisle.android.OPEN_MEDIA_ISLAND"`.
   - Khi nhận `ACTION_OPEN_MEDIA_ISLAND`, gọi `MediaIslandOverlay.toggle(context)`.
2. **`MediaIslandOverlay.kt`** (`com.originisle.android.service`):
   - `data class MediaSnapshot(val pkg: String, val track: String, val artist: String, val playing: Boolean, val badgedAlbumArt: Bitmap?, val durationMs: Long, val positionMs: Long, val controller: MediaController?, val contentIntent: PendingIntent?)`
   - `fun canShow(context: Context): Boolean`
   - `fun updateSnapshot(snapshot: MediaSnapshot)`
   - `fun show(context: Context, snapshot: MediaSnapshot? = null): Boolean`
   - `fun toggle(context: Context)`
   - `fun dismiss()`
3. **`Prefs.kt`**:
   - `const val PREF_MEDIA_OVERLAY_EXPAND = "cast_media_overlay_expand"` (mặc định `true`).
4. **`MediaCard.kt`**:
   - Tạo `MediaIslandOverlay.MediaSnapshot(...)` và gọi `MediaIslandOverlay.updateSnapshot(...)` trong `dispatchMediaCard`.
   - Nếu `MediaIslandOverlay.canShow(context)` và pref `PREF_MEDIA_OVERLAY_EXPAND` bật: `oi_island_click_expand = false`, `click_resp` & `oi_capsule_click_resp` trỏ `MediaControlReceiver.ACTION_OPEN_MEDIA_ISLAND`. Ngược lại fallback `oi_island_click_expand = true`.
   - Trong `clear(...)`: gọi `MediaIslandOverlay.dismiss()`.

## Ma Trận Phân Quyền Subagent (`Mode: SUBAGENT` | Model: `Gemini 4 Hydrogen`)
- [x] Task 1: Tạo `ic_media_output.xml`, `MediaIslandOverlay.kt` và nối `ACTION_OPEN_MEDIA_ISLAND` trong `MediaControlReceiver.kt` | [Agent: `Subagent-Media-Overlay`] | [Skill: `material-design-3-ui`] | [Locked Files: `app/src/main/res/drawable/ic_media_output.xml`, `app/src/main/java/com/originisle/android/service/MediaIslandOverlay.kt`, `app/src/main/java/com/originisle/android/service/MediaControlReceiver.kt`]
- [x] Task 2: Nối `MediaCard.kt` sang Hybrid `MediaIslandOverlay`, thêm `PREF_MEDIA_OVERLAY_EXPAND` trong `Prefs.kt` và nâng `v2.8.5` (`20805`) trong `app/build.gradle.kts` | [Agent: `Subagent-Media-Hybrid`] | [Skill: `material-design-3-ui`] | [Locked Files: `app/src/main/java/com/originisle/android/cards/MediaCard.kt`, `app/src/main/java/com/originisle/android/ui/Prefs.kt`, `app/build.gradle.kts`]
