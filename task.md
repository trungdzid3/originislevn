# Kế Hoạch Gỡ Phát Nhạc Khỏi Các App OriginOS Gốc Đã Hỗ Trợ — Origin Isle VN v2.10.6

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **`MediaCard.kt`**:
   - Khai báo `val ORIGIN_SUPPORTED_MUSIC_PACKAGES = setOf("com.spotify.music", "com.spotify.music.lite", "com.spotify.lite", "com.spotify.music.canary", "com.google.android.apps.youtube.music", "com.apple.android.music", "com.zing.mp3", "com.vng.zingmp3", "vn.com.vng.zingmp3")`.
   - Trong `post(...)` và `postFromController(...)`: nếu `pkgLower in ORIGIN_NATIVE_MUSIC_PACKAGES || pkgLower in ORIGIN_SUPPORTED_MUSIC_PACKAGES`, gọi `clear(context, ID)` và `return` ngay để nhường hoàn toàn cho trình phát nhạc gốc OriginOS 6.
   - Trong `dispatchMediaCard(...)`: đặt `putExtra("oi_show_notify", false)` để các app nhạc ngoài (nếu có dùng `MediaCard`) chỉ hiển thị trên Đảo, không tạo thêm thông báo đè trong bảng thông báo.
2. **`NotificationCastListener.kt`**:
   - Trong `onNotificationPosted(sbn)`: nếu `pkgLower in MediaCard.ORIGIN_SUPPORTED_MUSIC_PACKAGES` (và không phải thông báo thường không phải media nếu cần — thực tế app nhạc chỉ gửi thông báo nhạc/tải nhạc): gọi `cancelStrayDownloadCard(IconCache.castIdFor(sbn))`, `log(sbn, "bỏ qua — nhường Đảo nhạc gốc OriginOS 6", false)` và `return` ngay (không đẩy sang `MediaCard`, không rơi xuống `DownloadDetector` hay `GenericCard`).
   - Trong `pollRunnable`, `syncMediaSessions`, `isEligibleMediaController`, `dispatchControllerSession`: bỏ qua `pkgLower in MediaCard.ORIGIN_SUPPORTED_MUSIC_PACKAGES` tương tự `ORIGIN_NATIVE_MUSIC_PACKAGES`.
3. **`app/build.gradle.kts` & `PROJECT_CONTEXT.md`**:
   - Nâng `versionCode = 21006`, `versionName = "2.10.6"`.
   - Chạy `.\gradlew.bat testDebugUnitTest assembleRelease --console=plain`, xuất `release/OriginIsle-VN-v2.10.6.apk` và cập nhật `PROJECT_CONTEXT.md`.

## Ma Trận Phân Quyền Subagent (`Mode: SUBAGENT`)
- [x] Task 1: Cập nhật `MediaCard.kt` & `NotificationCastListener.kt`, nâng `v2.10.6` trong `app/build.gradle.kts`, chạy Gradle test + build APK release và cập nhật `PROJECT_CONTEXT.md` | [Agent: `Subagent-Yield-Native-Music-Apps`] | [Skill: `originos-design`] | [Locked Files: `app/src/main/java/com/originisle/android/cards/MediaCard.kt`, `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`, `app/build.gradle.kts`, `PROJECT_CONTEXT.md`]
