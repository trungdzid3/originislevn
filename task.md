# KẾ HOẠCH TRIỂN KHAI CƠ CHẾ BÁO PIN SAMSUNG FIT 3 & QUICK SHARE 0%

[CONTRACT LOCKED]
- Quick Share:
  1. `QuickShareDetector.kt`: Khi `isIndeterminate` hoặc đang kết nối/chuẩn bị, gán `progress = 0` (thay vì 10).
  2. `QuickShareCard.kt`: Khi `isIndeterminate`, hiển thị `pct = 0` và `oi_left_content = "0%"`.
- Samsung Galaxy Fit 3 Battery Mechanism (từ kết quả Reverse-Engineering):
  1. `EarbudsCard.kt`:
     - Tích hợp hàm `querySamsungFit3Battery(context: Context)`:
       + Gọi `sync_request` trên `content://com.samsung.wearable.fit3plugin.BatteryInfoProvider` với `sync_battery_data: 1`.
       + Truy vấn trực tiếp từ `content://com.samsung.wearable.providers.find.batteryproviderhelper/BatteryDBProvider/Battery_tb` đọc cột `Value` (`_btAddress`).
       + Lưu cache pin `saveCachedBattery(context, "Galaxy Fit 3", level)`.
     - Đăng ký `ContentObserver` trên URI `content://com.samsung.wearable.providers.find.batteryproviderhelper/BatteryDBProvider/Battery_tb` để nhận cập nhật pin ngay tức thì khi Fit 3 thay đổi pin.
     - Trong `show()`: nếu là thiết bị đeo mà pin chưa có, chủ động gọi `querySamsungFit3Battery(context)` trước khi fallback, đảm bảo luôn hiển thị `$pct%` thay vì "Đã kết nối".
  2. `NotificationCastListener.kt`:
     - Khi nhận notification từ các package Samsung (`com.samsung.android.app.watchmanager`, `com.samsung.wearable.fit3plugin`), tự động gọi `querySamsungFit3Battery` để cập nhật pin.
  3. `AndroidManifest.xml`:
     - Khai báo `<uses-permission>` cho `com.samsung.wearable.hostmanager.BATTERY_INFO_PROVIDER` và `com.samsung.android.companionapps.stfinder.READ_BATTERY`.
     - Khai báo `<queries>` cho các package và content provider của Samsung Wearable / Fit 3 Plugin.

[SUBAGENT TASKS]
- [x] Task 1: Tinh chỉnh Quick Share hiển thị ban đầu 0% trong QuickShareDetector và QuickShareCard | [Agent: Subagent-QuickShareZero] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/quickshare/QuickShareDetector.kt, app/src/main/java/com/originisle/android/cards/QuickShareCard.kt]
- [x] Task 2: Triển khai đọc pin phần cứng Samsung Fit 3 qua ContentProvider và ContentObserver trong EarbudsCard, NotificationCastListener và AndroidManifest.xml | [Agent: Subagent-Fit3BatteryProvider] | [Skill: android-cli] | [Locked Files: app/src/main/java/com/originisle/android/cards/EarbudsCard.kt, app/src/main/java/com/originisle/android/service/NotificationCastListener.kt, app/src/main/AndroidManifest.xml]
