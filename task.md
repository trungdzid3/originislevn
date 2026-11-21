# Kế Hoạch Điều Phối — Origin Isle VN v2.1.4 (Ẩn Tiến Trình Tải Khi Đang Ở Trong Edge/Chrome/Play Store & Tự Chui Vào Đảo Khi Thoát Ra)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **Nhận diện Trình duyệt & Cửa hàng ứng dụng đang mở ở tiền cảnh (`DownloadDetector.kt`, `KeepAliveAccessibilityService.kt`)**:
   - `DownloadDetector.isBrowserPackage(pkg: String): Boolean`: Trả về `true` cho các trình duyệt (`com.android.chrome*`, `com.chrome.*`, `com.microsoft.emmx*`, `com.vivo.browser`, `com.brave.browser`, `org.mozilla.*`, `com.opera.*`, `com.coccoc.trinhduyet`, `com.sec.android.app.sbrowser*`, `com.duckduckgo.mobile.android`, `mark.via.gp`, hoặc chứa `"browser"`).
   - `KeepAliveAccessibilityService`:
     + Lưu `@Volatile var instance: KeepAliveAccessibilityService? = null` và `@Volatile var currentForegroundPackage: String = ""`.
     + Trong `onAccessibilityEvent(event)`: lắng nghe mọi sự kiện cửa sổ từ các package thực (bỏ qua `com.android.systemui`, `android`, `com.autonavi.minimap`, các gói bàn phím `inputmethod`/`ime`/`keyboard`, `permissioncontroller`, `com.vivo.upslide`, `com.vivo.smartmultiwindow`, `com.vivo.fingerprint`). Khi `currentForegroundPackage` thay đổi, gọi ngay `DownloadCard.onForegroundPackageChanged(applicationContext, currentForegroundPackage)`.
     + Hàm `KeepAliveAccessibilityService.isBrowserCurrentlyOpen(downloadSourcePkg: String): Boolean`: kiểm tra live `instance?.rootInActiveWindow?.packageName` (nếu hợp lệ thì cập nhật `currentForegroundPackage`) và trả về `true` nếu người dùng đang mở chính trình duyệt `downloadSourcePkg`, đang mở bất kỳ trình duyệt nào khi `downloadSourcePkg` là `com.android.providers.downloads*`, hoặc đang mở chính ứng dụng tải (`com.android.vending` / `DownloadDetector.isDownloadPackage(downloadSourcePkg)`).
2. **Ẩn tiến trình tải khi đang ở trong trình duyệt / Play Store & Chui ngay vào Đảo khi thoát ra (`DownloadCard.kt`)**:
   - Trong `DownloadCard.post(context, info, sbn, forceImmediate: Boolean = false)`:
     + Khi tệp đang tải (`!info.isFinished && !info.isCanceled`) và `DownloadDetector.isDownloadPackage(sbn.packageName)` (bao gồm trình duyệt, `com.android.vending`, `com.android.providers.downloads*`):
       * Lưu bản ghi mới nhất vào `deferredDownloads[id] = DeferredDownload(sbn, info, now)`.
       * Nếu `KeepAliveAccessibilityService.isBrowserCurrentlyOpen(sbn.packageName)` là `true`:
         - Không đăng thẻ tiến trình lên Đảo; nếu thẻ `id` đang hiện trên Đảo (do người dùng vừa quay lại trình duyệt/Play Store), thu hồi tạm thời thẻ khỏi Đảo (`hideTemporarily(context, id)`) mà vẫn giữ trong `deferredDownloads[id]`.
         - `return`.
     + Ngay khi người dùng thoát khỏi Chrome/Edge/Play Store (`DownloadCard.onForegroundPackageChanged` thấy không còn ở trình duyệt / app nguồn):
       * Duyệt `deferredDownloads.values` và gọi ngay `post(context, item.info, item.sbn, forceImmediate = true)` (bỏ qua throttle 800ms) để viên thuốc tiến trình tải **chui ngay vào Đảo động tức thì** giống hệt trình phát nhạc.
     + Khi tải xong (`info.isFinished`) hoặc hủy (`info.isCanceled`): xóa khỏi `deferredDownloads[id]` và hiển thị kết quả hoàn tất / hủy.

## Ma Trận Phân Quyền (Orchestrator: Gemini 4 Argon | Worker: Gemini 4 Hydrogen)
- [x] Task 1: Triển khai cơ chế ẩn tiến trình tải khi đang ở trong Edge/Chrome/Play Store và tự động chui vào Đảo khi thoát ra trên `DownloadDetector.kt`, `KeepAliveAccessibilityService.kt`, `DownloadCard.kt`; bổ sung unit test `DownloadBrowserDetectionTest.kt`; nâng version `2.1.4` (`20104`), build `release/OriginIsle-VN-v2.1.4.apk` và phát hành GitHub Release `v2.1.4` | [Agent: Download-Exit-To-Island (`gemini-4-hydrogen-medium`)] | [Skill: impeccable] | [Locked Files: `app/src/main/java/com/originisle/android/download/DownloadDetector.kt`, `app/src/main/java/com/originisle/android/service/KeepAliveAccessibilityService.kt`, `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`, `app/src/test/java/com/originisle/android/download/DownloadBrowserDetectionTest.kt`, `app/build.gradle.kts`, `PROJECT_CONTEXT.md`, `task.md`]
