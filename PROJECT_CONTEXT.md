# TỔNG QUAN DỰ ÁN - ORIGIN ISLE VIỆT NAM (DAO ORIGIN)

## 1. Mục đích và Phạm vi
Bản fork và hiện đại hóa ứng dụng Origin Isle (Đảo Origin) dành riêng cho hệ điều hành OriginOS 6 (Vivo / iQOO) tại thị trường Việt Nam:
- **Phiên bản hiện tại**: **`1.0.5`** (mã bản dựng `versionCode = 10006`, tuân thủ chuẩn SemVer 2.0.0).
- **Nguồn gốc Upstream GitHub**: `https://github.com/fvhde/origin-isle` (đã đồng bộ đầy đủ lên `v2.2.3` / commit `73abb82`).
- **Cải tiến v1.0.5 (Đồng bộ chuẩn tính năng mới từ Upstream v2.2.2 & v2.2.3)**:
  1. **Tự động hạ cấp thẻ khi bị Đảo từ chối (`ACTION_REJECTED` / `onRejected`)**:
     - Khi một số bản ROM OriginOS từ chối thẻ phức tạp (`TEMPLATE_BUTTONS`, `TEMPLATE_DRIVING_NAVI`, `TEMPLATE_PROGRESS_VISUAL`), `NotificationCastListener` gửi `ACTION_REJECTED` sang `PlaygroundService` để tự động đăng lại dưới dạng thẻ cơ bản (`TEMPLATE_BASE` không kèm nút bấm) thay vì mất thông báo.
  2. **Khắc phục lỗi icon nút bấm từ ứng dụng khác (`Icon.ownedBy(sourcePkg)`)**:
     - Gắn đúng `sourcePkg` cho các `Icon` loại `TYPE_RESOURCE` bị khuyết package name khi tạo từ `addAction(int, ...)` cũ.
  3. **Đồng bộ công tắc Nút hành động (`GenericCard.shownActions`) & Bố cục Thẻ Sạc mới (`ChargingCard`)**:
     - Áp dụng `GenericCard.shownActions` xuyên suốt `GenericCard`, `MessageCard` và `NavigationCard` (nút "Thoát điều hướng" tuân theo công tắc Nút hành động).
     - `ChargingCard` hiển thị tia sét bên trái camera, phần trăm pin bên phải camera (`TEMPLATE_RIGHT_ISLAND_TEXT_ICON`) và vòng tiến độ pin (`oi_card_ring`) bên trong thân thẻ kèm màu FlashCharge.
  4. **Công tắc "Ứng dụng hệ thống" & Tối ưu tốc độ tải trong `AppsScreen.kt` (từ v2.2.2)**:
     - Bổ sung công tắc `apps_show_system` để liệt kê và chặn các dịch vụ hệ thống không có biểu tượng (như Motion Prompts của Vivo).
     - Tối ưu truy vấn `queryIntentActivities` 1 lần và giải mã biểu tượng bất đồng bộ kèm `iconCache` (`produceState`).

## 2. Bản Đồ Code (Code Map)
- `app/src/main/java/com/originisle/android/island/PlaygroundService.kt`: Dịch vụ trung tâm quản lý vòng đời thẻ SuperX (`readAndPost` L185-387, `onRejected` L395-408, `Icon.ownedBy` L415-419, `postSuperX` L426-458).
- `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`: Lắng nghe và phân loại thông báo (`onNotificationPosted` L424-621, `cleanUpRejectedSuperX` L760-774).
- `app/src/main/java/com/originisle/android/cards/`:
  - `ChargingCard.kt` (L80-132): Thẻ sạc pin FlashCharge với `oi_card_ring` và `%` bên phải camera.
  - `GenericCard.kt` (L31-215): Thẻ thông báo chung & `shownActions` helper.
  - `MessageCard.kt`, `NavigationCard.kt`, `PaymentCard.kt`, `QuickShareCard.kt`, `DownloadCard.kt`, `OtpCard.kt`, `EarbudsCard.kt`, `RideCard.kt`.
- `app/src/main/java/com/originisle/android/ui/AppsScreen.kt`: Màn hình quản lý ứng dụng (`AppsScreen` L52-175, `AppIcon` L213-223, `loadApps` L225-241).

## 3. Quy Định Xuất Bản Cố Định (Strict Policy)
File APK thành phẩm xuất bản trực tiếp tại thư mục nguồn dự án:
`C:\Users\Lenovo\.gemini\antigravity\scratch\origin-isle-vn\OriginIsle-VN-v1.0.5-debug.apk`

## 4. Tình Trạng Hiện Tại
- Biên dịch & Unit Tests: `BUILD SUCCESSFUL` (`assembleDebug` & `testDebugUnitTest` PASS).
- File APK: `OriginIsle-VN-v1.0.5-debug.apk` (76.6 MB).
