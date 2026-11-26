# Kế Hoạch Nâng Cấp Nút Đảo OriginOS & Sửa Mở TikTok — Origin Isle VN v2.10.2

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **`OriginIslandBuilder.kt`**:
   - Tại nhánh `template == 8` (`TEMPLATE_BUTTONS`): nối tham số `btnTransparent` (đã có sẵn trong signature `buildBundle`):
     - Nếu `btnTransparent == true`: `btnColor = Color.parseColor("#26FFFFFF")` (nền kính mờ OriginOS 6–7) và `textColor = Color.WHITE`.
     - Nếu `btnTransparent == false`: giữ nguyên `btnColor = Color.WHITE` và `textColor = Color.BLACK`.
2. **4 Vector Icon Nút Nhạc (`ic_media_prev.xml`, `ic_media_play.xml`, `ic_media_pause.xml`, `ic_media_next.xml`) & `MediaCard.kt`**:
   - Vẽ lại 4 icon theo tạo hình bo góc mềm mại (*squircle* / rounded corners) chuẩn `originos-design`, giữ dạng `Icon.createWithResource`.
   - Trong `MediaCard.kt`: bật `putExtra("oi_btn_transparent", true)`.
3. **Sửa mở TikTok từ Đảo mở rộng (`TikTokStreakLaunchActivity.kt`, `AndroidManifest.xml`, `TikTokStreakCard.kt`)**:
   - Tạo `TikTokStreakLaunchActivity` (translucent, `excludeFromRecents=true`, `noHistory=true`) nhận `EXTRA_TARGET_INTENT` & `EXTRA_SOURCE_PKG`: copy tin nhắn chuỗi, hiện Toast, gửi `targetPi` với `ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED` (Android 14+) hoặc dò mở các gói TikTok đã cài (`sourcePkg`, `com.ss.android.ugc.trill`, `com.zhiliaoapp.musically`, `com.ss.android.ugc.aweme`), đóng thẻ Đảo và `finish()`.
   - Trong `TikTokStreakCard.kt`: dùng `PendingIntent.getActivity(..., TikTokStreakLaunchActivity)` cho cả `click_resp`, `source_content_intent` và nút `"Giữ chuỗi"` (kèm icon `R.drawable.ic_tiktok_streak_fire`, `oi_btn_transparent = true`, `oi_show_notify = true`, `status_chip_text = " "` để không bị vivo từ chối); trong `postSample` tự dò gói TikTok thực tế đang cài trên máy.
4. **Build & Release `v2.10.2` (`21002`)**:
   - Nâng `versionCode = 21002`, `versionName = "2.10.2"` trong `app/build.gradle.kts`.

## Ma Trận Phân Quyền Subagent (`Mode: SUBAGENT`)
- [x] Task 1: Vẽ lại 4 icon nhạc chuẩn `originos-design`, nối `btnTransparent` trong `OriginIslandBuilder.kt` và bật trong `MediaCard.kt` | [Agent: `OriginOS-Media-Buttons`] | [Skill: `originos-design`] | [Locked Files: `app/src/main/res/drawable/ic_media_prev.xml`, `app/src/main/res/drawable/ic_media_play.xml`, `app/src/main/res/drawable/ic_media_pause.xml`, `app/src/main/res/drawable/ic_media_next.xml`, `app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt`, `app/src/main/java/com/originisle/android/cards/MediaCard.kt`]
- [x] Task 2: Tạo `TikTokStreakLaunchActivity.kt`, khai báo trong `AndroidManifest.xml`, cập nhật `TikTokStreakCard.kt` (nút kính OriginOS + Activity trampoline vượt BAL + dò gói TikTok VN) và nâng `v2.10.2` trong `app/build.gradle.kts` | [Agent: `OriginOS-TikTok-Launch-Fix`] | [Skill: `originos-design`] | [Locked Files: `app/src/main/java/com/originisle/android/tiktok/TikTokStreakLaunchActivity.kt`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/originisle/android/cards/TikTokStreakCard.kt`, `app/build.gradle.kts`]
