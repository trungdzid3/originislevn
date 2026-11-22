# Kế Hoạch Điều Phối Subagent — Origin Isle VN v2.2.2 (Giải Pháp Chui Vào Đảo Khi Thoát Chrome/Edge/Play Store)

## Hợp Đồng Giao Ước Dùng Chung (LOCKED CONTRACT)
1. **Bảo toàn `PendingIntent` gốc (`creatorPackage` của Chrome/Edge/Play Store) & Metadata Landing SuperX (`OriginIslandBuilder.kt`, `PlaygroundService.kt`, `DownloadCard.kt`)**:
   - `PlaygroundService.kt`:
     - Trong `postSuperX(...)`: nhận thêm `clickResp: PendingIntent?` và gọi `if (clickResp != null) builder.setContentIntent(clickResp)` để thông báo `VIVO_SUPERX_TAG` mang trực tiếp `PendingIntent` có `creatorPackage` của ứng dụng nguồn.
   - `OriginIslandBuilder.kt`:
     - Khi `!sourcePkg.isNullOrBlank()`: ngoài `island.putString(C.BUNDLE_KEY_ISLAND_LANDING_PKG, sourcePkg)`, ghi thêm `island.putBundle(C.BUNDLE_KEY_ISLAND_LANDING_INFO, Bundle().apply { putString("landingPkg", sourcePkg); putString("pkg", sourcePkg); if (clickResp != null) putParcelable("clickResp", clickResp) })`.
   - `DownloadCard.kt`:
     - Giữ nguyên `val clickPi = info.contentIntent ?: n.contentIntent ?: fallbackClickIntent(context, effectivePkg)` để bảo toàn `creatorPackage` gốc của Chrome/Edge/Play Store và mở thẳng trang Downloads khi chạm thẻ mở rộng.
     - Khi một lượt tải mới xuất hiện lần đầu (`!firstSeenTimeMap.containsKey(id)` và `!isTerminated`): nếu `KeepAliveAccessibilityService.instance != null` và `KeepAliveAccessibilityService.currentForegroundPackage.isBlank()`, gán `KeepAliveAccessibilityService.onDownloadStartedInApp(effectivePkg)` để khóa ẩn ngay tại `t = 0` khi đang ở trong trình duyệt/Play Store.
     - Chỉ đặt `putExtra("oi_force_show", isFirstPost || isTerminated || forceImmediate)` để viên thuốc bung ra mượt mà ngay khoảnh khắc thoát app và cập nhật `%` êm ái ở các nhịp tiếp theo.
2. **Bắt chính xác sự kiện thoát về Vivo Launcher ở mọi `eventType` & Khớp nhịp cử chỉ vuốt (`KeepAliveAccessibilityService.kt`)**:
   - Trong `onAccessibilityEvent(event)`:
     - Với MỌI `eventType`, khi `pkg.isNotBlank() && !isIgnoredWindowPkg(pkg)`:
       - Nếu `isLauncherPackage(pkg)` hoặc `eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED` hoặc (`eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED` && `pkg != currentForegroundPackage`):
         cập nhật ngay `lastWindowChangedPkg = pkg`, `lastWindowChangedTimeMs = System.currentTimeMillis()`, `currentForegroundPackage = pkg`.
     - Khi `DownloadCard.hasDeferredDownloads()` là `true` và nhận sự kiện từ `com.android.systemui` (cử chỉ vuốt Home/Recents bắt đầu) hoặc `isLauncherPackage(pkg)` hoặc `TYPE_WINDOWS_CHANGED`, kích hoạt chuỗi kiểm tra cử chỉ thoát siêu nhạy ở các mốc `50ms`, `150ms`, `300ms`, `550ms`.
   - Trong `isBrowserCurrentlyOpen(downloadSourcePkg)`:
     - Nếu `svc != null`: kiểm tra các cửa sổ `TYPE_APPLICATION` trong `svc.windows`. Nếu có cửa sổ `TYPE_APPLICATION` của `downloadSourcePkg`/trình duyệt trong `svc.windows` nhưng **tất cả các cửa sổ đó đều có `!w.isActive && !w.isFocused`**, trả về `false` ngay lập tức (cửa sổ trình duyệt/Play Store đang thu nhỏ hoặc đã xuống nền).

## Ma Trận Phân Quyền Subagent (`Mode: SUBAGENT` | Model: `Gemini 4 Hydrogen`)
- [x] Task 1: Nâng cấp `OriginIslandBuilder.kt`, `PlaygroundService.kt`, `KeepAliveAccessibilityService.kt`, `DownloadCard.kt` và unit test `DownloadBrowserDetectionTest.kt` | [Agent: `Subagent-Landing-And-Exit-v222`] | [Skill: `impeccable`] | [Locked Files: `app/src/main/java/com/originisle/android/island/OriginIslandBuilder.kt`, `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`, `app/src/main/java/com/originisle/android/service/KeepAliveAccessibilityService.kt`, `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`, `app/src/test/java/com/originisle/android/download/DownloadBrowserDetectionTest.kt`]
- [x] Task 2: Nâng `versionCode = 20202`, `versionName = "2.2.2"`, chạy `./gradlew.bat testDebugUnitTest assembleRelease`, xuất `release/OriginIsle-VN-v2.2.2.apk`, cập nhật `PROJECT_CONTEXT.md` & `task.md`, push GitHub & phát hành Release `v2.2.2` | [Agent: `Subagent-Release-v222`] | [Skill: `pr-feedback-quality-gate`] | [Locked Files: `app/build.gradle.kts`, `PROJECT_CONTEXT.md`, `task.md`, `release/OriginIsle-VN-v2.2.2.apk`]
