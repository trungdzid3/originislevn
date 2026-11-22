# Kế Hoạch Điều Phối Subagent — Origin Isle VN v2.2.3 (Chặn Thông Báo Ẩn Danh / Nền Trình Duyệt Tạo Đảo Động)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **`DownloadDetector.kt`**:
   - Bổ sung hàm public `isIncognitoOrPrivateBrowsing(channelId: String, combinedText: String): Boolean` nhận diện kênh/nội dung ẩn danh (`incognito`, `inprivate`, `ẩn danh`, `private tab`, `private browsing`, `duyệt web riêng tư`, `chế độ riêng tư`, `secret mode`, `đóng tất cả các thẻ`, `đóng tất cả các tab`, `close all incognito`, `close all inprivate`, `close private tabs`).
   - Trong `DownloadDetector.isDownload(sbn)`: nếu `isBrowserPackage(pkg)` và `isIncognitoOrPrivateBrowsing(channelId, allText)` thì trả `false` ngay lập tức.
2. **`NotificationCastListener.kt`**:
   - Trong `onNotificationPosted(sbn)`:
     - Sau nhánh xử lý Media (`isActualMedia`) và Tải xuống (`DownloadDetector.isDownload(sbn)`), chặn triệt để thông báo từ trình duyệt (`DownloadDetector.isBrowserPackage(sbn.packageName)`):
       - Nếu là thông báo ẩn danh (`DownloadDetector.isIncognitoOrPrivateBrowsing(...)`) -> `log(sbn, "bỏ qua — chế độ ẩn danh trình duyệt", false); return`.
       - Nếu là thông báo `isOngoing` từ trình duyệt mà không phải cuộc gọi, không có tiến độ tải (`!hasProgress`) và không phải media -> `log(sbn, "bỏ qua — thông báo nền trình duyệt", false); return` (tránh mọi thông báo thường trực không phải tải xuống của Chrome/Edge/Firefox/Brave/... lọt xuống `GenericCard`).
3. **Unit Tests & Phát hành `v2.2.3` (`versionCode = 20203`)**:
   - Thêm test `isIncognitoOrPrivateBrowsing_filtersChromeEdgePrivateTabs` trong `DownloadBrowserDetectionTest.kt`.
   - Chạy `./gradlew.bat testDebugUnitTest assembleRelease --console=plain`, xuất `release/OriginIsle-VN-v2.2.3.apk`, cập nhật `PROJECT_CONTEXT.md` & `task.md`, push GitHub + phát hành Release `v2.2.3`.

## Ma Trận Phân Quyền Subagent (`Mode: SUBAGENT` | Model: `Gemini 4 Hydrogen`)
- [x] Task 1: Chặn thông báo ẩn danh / nền trình duyệt trong `DownloadDetector.kt`, `NotificationCastListener.kt`, bổ sung unit test `DownloadBrowserDetectionTest.kt`, nâng `v2.2.3` (`20203`), build APK, cập nhật docs và phát hành GitHub Release `v2.2.3` | [Agent: `Subagent-Block-Incognito-v223`] | [Skill: `impeccable`] | [Locked Files: `app/src/main/java/com/originisle/android/download/DownloadDetector.kt`, `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`, `app/src/test/java/com/originisle/android/download/DownloadBrowserDetectionTest.kt`, `app/build.gradle.kts`, `PROJECT_CONTEXT.md`, `task.md`, `release/OriginIsle-VN-v2.2.3.apk`]
