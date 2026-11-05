# KẾ HOẠCH TRIỂN KHAI CHI TIẾT CHO FLASH (FLASH EXECUTION PLAN v2.0 - ORIGINOS 6)

> **MỤC TIÊU BẢN CẬP NHẬT `v1.0.4` (`versionCode = 5`)**:
> 1. **Xóa bỏ hoàn toàn tính năng vuốt ngang / nút chuyển tác vụ giả lập trên đảo (Carousel)**: Trả về cơ chế đa nhiệm xếp chồng tự nhiên của OriginOS 6.
> 2. **Đồng bộ toàn diện xuống OriginOS 6**: Xóa mọi nhãn và tính năng gắn mác `(OriginOS 7)` (như Bóc tách ảnh chụp màn hình).
> 3. **Sửa dứt điểm Tiến độ Tải xuống (Download), Chia sẻ nhanh (Quick Share) và bổ sung Nút Test OTP**: Đưa `DownloadCard` và `QuickShareCard` về đúng chuẩn hiển thị `GenericCard` của bản `0.0.1 / 0.0.2` (giữ `smallIcon` gốc, dùng `Icon.createWithResource` cho `largeIcon`, truyền `oi_left_content`, bỏ `oi_card_ring` và bỏ `safeAppIcon` Bitmap), đồng thời ưu tiên kiểm tra Quick Share / Download trước `OtpDetector`.
> 4. **Sửa dứt điểm thẻ Tai nghe Bluetooth (`EarbudsCard.kt`)**: Hiển thị rõ ràng Pin Tai trái (L) / Tai phải (R) / Hộp sạc (Case) khi mở rộng không bị cắt chữ `...`, và lấy **100% thông số pin thật** trên OriginOS 6 (qua Fast Pair / Vivo TWS Notification + HFP Vendor AT Command + BluetoothManager).
> 5. **Thiết kế lại Logo ứng dụng chuẩn ngôn ngữ Vivo OriginOS 6**: Nền chuyển sắc xanh Vivo (`#2B44FF` -> `#0D84FF`), mặt kính mờ trắng kết hợp viên thuốc Đảo Nguyên Tử (Atomic Island) trung tâm.

---

## PHÂN TÍCH TÍNH KHẢ THI CỦA TÍNH NĂNG VUỐT ĐỔI TÁC VỤ TRÊN ĐẢO

1. **Tại sao không thể bắt cử chỉ vuốt (Swipe) trực tiếp trên viên thuốc của Vivo?**:
   - Đảo Nguyên Tử (Atomic Island / SuperX) do tiến trình hệ thống `com.android.systemui` của Vivo vẽ và quản lý cảm ứng.
   - API `com.vivo.penwrite.superx` chỉ cho phép ứng dụng bên thứ 3 gửi dữ liệu văn bản, icon, vòng tiến độ và tối đa 2 nút bấm (`Action` gắn `PendingIntent`). Vivo SystemUI **không mở bất kỳ callback nào** để ứng dụng bên thứ 3 nhận sự kiện vuốt tay trên đảo.
   - Việc nhét nút `"Đổi (1/2)"` vào màn hình mở rộng vừa xấu, vừa mất diện tích của nút hành động chính, lại không giống cử chỉ vuốt thật.
2. **Cơ chế tự nhiên của OriginOS 6**:
   - Khi mỗi thẻ (Tải xuống, Quick Share, OTP, Tai nghe) được gửi lên với `notificationId` riêng biệt và `oi_scene` phù hợp, chính hệ thống **OriginOS 6 tự động tách thành các viên thuốc xếp chồng (hoặc chấm tròn phụ bên cạnh)** cho phép người dùng vuốt chuyển tự nhiên bằng trình quản lý gốc của Vivo.
   - **Quyết định**: **BỎ HOÀN TOÀN** mọi code Carousel / nút chuyển giả lập.

---

## HẠNG MỤC 1: XÓA BỎ HOÀN TOÀN PHẦN ORIGINOS 7 & ĐỒNG BỘ XUỐNG ORIGINOS 6

### 1.1. File `app/src/main/java/com/originisle/android/ui/HomeScreen.kt`
- **Tại dòng 492**:
  - Đổi tiêu đề từ `"Tai nghe Bluetooth (OriginOS 7)"` thành `"Tai nghe Bluetooth"`.
  - Đổi mô tả phụ thành `"Hiển thị trực tiếp mức pin Tai trái, Tai phải và Hộp sạc thật trên OriginOS 6"`.
- **Tại dòng 503–511**:
  - **Xóa bỏ hoàn toàn** khối `SettingSwitchRow` của `"Bóc tách ảnh chụp màn hình (OriginOS 7)"` (`smartScreenshotOn`) và `HorizontalDivider` đi kèm.
- **Tại phần đầu `HomeScreen.kt` (khoảng dòng 95)**:
  - Xóa biến state `var smartScreenshotOn by remember { ... }` nếu không còn sử dụng.

### 1.2. File `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`
- **Xóa bỏ hoàn toàn** `screenshotObserver`:
  - Xóa khai báo `private var screenshotObserver: ScreenshotContentObserver? = null` (dòng 324).
  - Xóa đoạn đăng ký `screenshotObserver` trong `onListenerConnected()` (dòng 331–341).
  - Xóa đoạn hủy `screenshotObserver` trong `onListenerDisconnected()` (dòng 348–351).
  - Xóa import `com.originisle.android.smart.ScreenshotContentObserver`.

---

## HẠNG MỤC 2: KHẮC PHỤC TRIỆT ĐỂ TIẾN ĐỘ DOWNLOAD, QUICK SHARE & BỔ SUNG NÚT TEST OTP

### Nguyên nhân gốc rễ khiến Download và Quick Share không lên đảo ở bản `1.0.3`
Khi đối chiếu với `GenericCard.kt` (vốn chạy tốt ở bản `0.0.1 / 0.0.2`), `DownloadCard.kt` và `QuickShareCard.kt` đang mắc **3 lỗi nghiêm trọng**:
1. **Sai kiểu Icon (`Bitmap` thay vì `Resource Icon`)**:
   - `DownloadCard` và `QuickShareCard` gọi `IconCache.activeSmallIcons.remove(id)` và dùng `IconCache.safeAppIcon()` (render ra `Icon.TYPE_BITMAP`) gán cho cả `oi_left_icon` lẫn `oi_large_icon`.
   - Trong khi đó, `GenericCard.post()` giữ nguyên `sbn.notification.smallIcon` (`Icon.TYPE_RESOURCE`) trong `IconCache.activeSmallIcons[id]` và dùng `Icon.createWithResource(sbn.packageName, appInfo.icon)` trong `IconCache.activeLargeIcons[id]`, đồng thời **không gửi `oi_left_icon` dạng Bitmap** trong Intent!
2. **Truyền thừa `oi_card_ring` và thiếu `oi_left_content`**:
   - Trong `PlaygroundService.kt`, khi `intent.hasExtra("oi_card_ring")` mà không có màu `oi_fg_color`, nó ép `baseInfos` sang `BASE_SUB_INFO_PROGRESS (5)` xung đột với `TEMPLATE_RIGHT_ISLAND_PROGRESS (2)`.
   - Trong `GenericCard.kt` (dòng 133 & 151–157): Khi hiển thị vòng tiến độ trên đảo (`oi_right_template = 2`), `GenericCard` **KHÔNG truyền `oi_card_ring`**, mà chỉ truyền:
     - `oi_template = PlaygroundService.TEMPLATE_BASE` (`4`)
     - `oi_right_template = PlaygroundService.TEMPLATE_RIGHT_ISLAND_PROGRESS` (`2`)
     - `oi_progress = pct` (`0..100`)
     - `oi_left_content = title.take(12)` (Hiển thị tên file/tiến độ ngắn gọn bên cạnh vòng tròn).
3. **Thứ tự bắt thông báo trong `NotificationCastListener.kt`**:
   - Hiện tại `OtpDetector.find()` đang đặt **trước** `QuickShareDetector` và `DownloadDetector`. Do `OtpDetector` có regex bắt chuỗi 4–8 chữ số sau từ `"là"` / `"is"` hoặc từ khóa `"code"` / `"pin"` / `"xác nhận"`, các thông báo Quick Share (có mã PIN ghép đôi hoặc chữ Xác nhận) hoặc tên file tải xuống có số dễ bị `OtpDetector` chặn mất trước khi tới `QuickShareDetector` / `DownloadDetector`.

### Chi tiết kỹ thuật cần sửa

#### 2.1. Sửa `app/src/main/java/com/originisle/android/service/NotificationCastListener.kt`
Đổi thứ tự ưu tiên trong `onNotificationPosted(sbn: StatusBarNotification)` theo đúng thứ tự sau:
1. **Bước 0 - Bắt thông báo Pin Tai nghe Bluetooth thực tế** (chi tiết ở Hạng mục 3): Nếu thông báo từ hệ thống Bluetooth / Fast Pair / Vivo TWS có thông tin `%` pin tai nghe -> cập nhật ngay vào `EarbudsCard`.
2. **Bước 1 - Kiểm tra Quick Share (`QuickShareDetector.detect(sbn, applicationContext)`) TRƯỚC TIÊN**:
   - Nếu `qsInfo != null`: Gọi `QuickShareCard.post(applicationContext, sbn, qsInfo)` và `return`.
3. **Bước 2 - Kiểm tra Tải xuống (`DownloadDetector.detect(sbn, applicationContext)`) THỨ HAI**:
   - Nếu `dlInfo != null`: Gọi `DownloadCard.post(applicationContext, sbn, dlInfo)` và `return`.
4. **Bước 3 - Kiểm tra OTP (`OtpDetector.find(...)`) THỨ BA**:
   - Không chạy `OtpDetector` trên các gói trình duyệt/tải xuống/chia sẻ hệ thống (`com.vivo.easyshare`, `com.android.providers.downloads`, `com.bbk.appstore`, `com.android.vending`) trừ khi văn bản có từ khóa `"OTP"` tường minh.

#### 2.2. Sửa `app/src/main/java/com/originisle/android/cards/DownloadCard.kt`
Viết lại hàm `post(ctx: Context, sbn: StatusBarNotification, info: DownloadInfo)` theo đúng chuẩn `GenericCard`:
- Tính `id = idFor(sbn)`.
- **Quản lý Icon chuẩn Resource**:
  ```kotlin
  val n = sbn.notification
  n.smallIcon?.let { IconCache.activeSmallIcons[id] = it } ?: IconCache.activeSmallIcons.remove(id)
  val largeIcon: Icon? = try {
      val pm = ctx.packageManager
      val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
      if (appInfo.icon != 0) Icon.createWithResource(sbn.packageName, appInfo.icon)
      else n.getLargeIcon()
  } catch (_: Throwable) { n.getLargeIcon() }
  if (largeIcon != null) IconCache.activeLargeIcons[id] = largeIcon
  else IconCache.activeLargeIcons.remove(id)
  ```
- **Thiết lập tham số Intent cho `PlaygroundService`**:
  - Khi đang tải (`!info.completed && !info.indeterminate`):
    - `val pct = info.progress.coerceIn(1, 99)`
    - `putExtra("oi_template", PlaygroundService.TEMPLATE_BASE)` (4)
    - `putExtra("oi_right_template", PlaygroundService.TEMPLATE_RIGHT_ISLAND_PROGRESS)` (2)
    - `putExtra("oi_progress", pct)`
    - `putExtra("oi_left_content", "${pct}%")` (hiển thị rõ `%` ngay trên viên thuốc thu gọn cạnh vòng tròn tiến độ!)
    - `putExtra("oi_right_content", "")`
    - **TUYỆT ĐỐI KHÔNG truyền `oi_card_ring` và KHÔNG truyền `oi_left_icon`**.
    - Trên thẻ mở rộng:
      + `oi_title`: `"${info.title.take(26)} (${pct}%)"`
      + `oi_text`: `info.subText.ifBlank { "${info.appLabel} · Đang tải xuống" }`
  - Khi tải ở chế độ `indeterminate` (đang bắt đầu tải, chưa rõ dung lượng):
    - `putExtra("oi_template", PlaygroundService.TEMPLATE_BASE)` (4)
    - `putExtra("oi_right_template", PlaygroundService.TEMPLATE_RIGHT_ISLAND_PROGRESS)` (2)
    - `putExtra("oi_progress", 5)`
    - `putExtra("oi_left_content", "Tải...")`
    - `oi_title`: `info.title.take(28)`
    - `oi_text`: `"${info.appLabel} · Đang chuẩn bị tệp..."`
  - Khi đã hoàn tất (`info.completed`):
    - `putExtra("oi_template", PlaygroundService.TEMPLATE_ISLAND_TEXT_SWAP)` (1)
    - `putExtra("oi_right_template", PlaygroundService.TEMPLATE_RIGHT_ISLAND_NONE)` (0)
    - `putExtra("oi_left_content", "Xong")`
    - `putExtra("oi_right_content", info.title.take(10))`
    - `oi_title`: `"Tải xong · ${info.title.take(22)}"`
    - `oi_text`: `info.subText.ifBlank { "Chạm để mở tệp từ ${info.appLabel}" }`

#### 2.3. Sửa `app/src/main/java/com/originisle/android/quickshare/QuickShareDetector.kt` và `QuickShareCard.kt`
- **Trong `QuickShareDetector.kt`**:
  - Mở rộng `QUICK_SHARE_PACKAGES`: Thêm `"com.vivo.share"`, `"com.bbk.share"`, `"com.android.bluetooth"`.
  - Mở rộng kiểm tra cho `"com.google.android.gms"`: Trên Android 14/15 OriginOS 6, Google Quick Share có thể dùng channel `"nearby_sharing"`, `"quick_share"`, `"fast_pair"`, hoặc tiêu đề/nội dung có từ `"Quick Share"`, `"Chia sẻ nhanh"`, `"Nearby Share"`, `"Đang gửi"`, `"Đang nhận"`, `"Sending"`, `"Receiving"`, `"Đã nhận"`, `" đã chia sẻ"`. Nếu channel hoặc tiêu đề khớp một trong các dấu hiệu này -> nhận diện ngay lập tức là Quick Share!
  - Nếu thông báo Quick Share đang ở trạng thái chờ xác nhận hoặc đang kết nối mà chưa có `EXTRA_PROGRESS_MAX > 0`, trả về `indeterminate = true, progress = 10` để thẻ xuất hiện ngay trên đảo.
- **Trong `QuickShareCard.kt`**:
  - Áp dụng cùng kiến trúc chuẩn `GenericCard` giống hệt `DownloadCard`:
    - Giữ `sbn.notification.smallIcon` trong `IconCache.activeSmallIcons[id]`.
    - Đặt `Icon.createWithResource(sbn.packageName, appInfo.icon)` vào `IconCache.activeLargeIcons[id]`.
    - **Bỏ hoàn toàn `oi_card_ring` và `oi_left_icon`**.
    - Khi đang truyền (`!info.completed`):
      + `val pct = if (info.indeterminate) 10 else info.progress.coerceIn(1, 99)`
      + `putExtra("oi_template", PlaygroundService.TEMPLATE_BASE)` (4)
      + `putExtra("oi_right_template", PlaygroundService.TEMPLATE_RIGHT_ISLAND_PROGRESS)` (2)
      + `putExtra("oi_progress", pct)`
      + `putExtra("oi_left_content", if (info.indeterminate) "Chia sẻ" else "${pct}%")`
      + `oi_title`: `"Quick Share · ${if (info.indeterminate) "Đang kết nối" else "$pct%"}"`
      + `oi_text`: `"${info.title.take(22)} · ${info.subText.ifBlank { "Đang truyền tệp" }}"`

#### 2.4. Bổ sung tính năng Test thực tế cho OTP, Download và Quick Share trong `IslandSamples.kt`
Để người dùng test ngay được toàn bộ luồng bắt thông báo thật (kể cả khi chưa có tin nhắn SMS ngân hàng gửi tới):
- Khi bấm mẫu **"Mã OTP Ngân hàng"** trong `IslandSamples.kt`:
  - Phát một **Notification hệ thống thực sự** (qua `NotificationManager`) giả lập tin nhắn SMS ngân hàng: `"Vietcombank: Ma OTP giao dich cua Quy khach la 482915. Tuyet doi khong chia se ma nay."`
  - `NotificationCastListener` sẽ tự động bắt thông báo thật này -> chạy qua `OtpDetector` -> trích xuất mã `482915` -> đẩy lên Đảo Nguyên Tử qua `OtpCard` kèm nút **"Sao chép 482915"**!
- Tương tự, mẫu **"Tiến độ Tải xuống"** và **"Chia sẻ Quick Share"** cũng phát Notification có `setProgress(100, pct, false)` thực tế để đi qua đúng luồng `DownloadDetector -> DownloadCard` và `QuickShareDetector -> QuickShareCard`.

---

## HẠNG MỤC 3: NÂNG CẤP THẺ TAI NGHE BLUETOOTH (`EarbudsCard.kt`) CHUẨN ORIGINOS 6

### 3.1. Sửa bố cục mở rộng để hiện rõ 100% Pin Trái / Phải / Hộp sạc (không bị cắt `...`)
- **Nguyên nhân hiện tại**: Dòng `text` ghép cả 3 thông số `"Tai Trái: 95% ⚡   |   Tai Phải: 90%   |   Hộp sạc: 100% ⚡"` quá dài (58 ký tự) nên Vivo cắt mất phần Hộp sạc ở cuối.
- **Thiết kế bố cục 2 dòng mới trong `EarbudsCard.postToService()`**:
  - **Trường hợp 1: Có đủ thông số Trái / Phải (và có hoặc không có Hộp sạc)**:
    - Dòng 1 (`oi_title`):
      + Nếu có thông tin Hộp sạc thật (`case > 0`): `"${info.deviceName.take(16)} · Hộp: ${info.case}%${if (info.caseCharging) "⚡" else ""}"`
      + Nếu tai nghe không báo pin Hộp sạc (`case <= 0`): `"${info.deviceName.take(24)}"` (Tuyệt đối không hiển thị số giả!).
    - Dòng 2 (`oi_text`):
      + `"Trái (L): ${lText}   ·   Phải (R): ${rText}"` (Chỉ dài ~32 ký tự, hiển thị trọn vẹn 100% trên mọi màn hình OriginOS 6).
  - **Trường hợp 2: Tai nghe chuẩn chỉ báo 1 mức pin chung (`main > 0`, không tách L/R)**:
    - Dòng 1 (`oi_title`): `"${info.deviceName.take(24)}"`
    - Dòng 2 (`oi_text`): `"Pin tai nghe: ${info.main}% · Đang kết nối âm thanh"`
  - **Trên viên thuốc thu gọn (Collapsed Pill)**:
    - `oi_left_content`: `"${effectivePct}%"`
    - Vòng tròn tiến độ bên phải: `oi_right_template = TEMPLATE_RIGHT_ISLAND_PROGRESS` (`2`), `oi_progress = effectivePct`.

### 3.2. Lấy thông tin Pin Tai trái / Tai phải / Hộp sạc THẬT trên OriginOS 6 (Không dùng dữ liệu giả)
Trên Android 14/15 (OriginOS 6), hàm ẩn `BluetoothDevice.getMetadata(int)` bị chặn bởi Hidden API Blacklist. Để lấy thông tin **Pin Trái / Phải / Hộp sạc thật** mà không cần quyền Root/ADB, triển khai **3 tầng thu thập dữ liệu thực tế**:

1. **Tầng 1 - Trích xuất trực tiếp từ Thông báo Hệ thống Fast Pair / Vivo TWS (`EarbudsBatteryParser`)**:
   - Khi mở nắp hộp sạc hoặc kết nối tai nghe TWS trên Vivo OriginOS 6, hệ thống (`com.google.android.gms` - Google Fast Pair, `com.vivo.tws`, `com.vivo.earphone`, `com.android.bluetooth`, `com.vivo.audiofx`) sẽ phát một thông báo trạng thái (thường là silent/ongoing notification) chứa mức pin thật của **Tai trái (L)**, **Tai phải (R)** và **Hộp sạc (Case)**.
   - Bổ sung hàm `EarbudsCard.tryParseBatteryFromNotification(ctx: Context, sbn: StatusBarNotification): Boolean` được gọi ngay đầu `NotificationCastListener.onNotificationPosted`:
     - Kiểm tra gói nguồn thuộc nhóm Bluetooth/TWS/GMS hoặc có chứa từ khóa tai nghe (`earbuds`, `tws`, `airpods`, `buds`, `tai nghe`, `hộp sạc`, `charging case`) kèm ký hiệu `%`.
     - Dùng Regex bóc tách chính xác:
       + Pin Trái: `(?:L|Left|Trái|Tai trái)\s*[:：-]?\s*(\d{1,3})\s*%`
       + Pin Phải: `(?:R|Right|Phải|Tai phải)\s*[:：-]?\s*(\d{1,3})\s*%`
       + Pin Hộp sạc: `(?:Case|Box|C|Hộp|Hộp sạc|Dock)\s*[:：-]?\s*(\d{1,3})\s*%`
     - Ngay khi bóc tách được thông số thật từ thông báo hệ thống, lưu vào cache trạng thái thật của `EarbudsCard` và hiển thị ngay lên Đảo Nguyên Tử!

2. **Tầng 2 - Giải mã gói tin Bluetooth HFP Vendor-Specific AT Command (`+IPHONEACCEV` / `+XEVENT`)**:
   - Trong `EarbudsCard.registerReceiver(ctx)`, đăng ký thêm Intent Action:
     - `"android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT"` (`BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT`)
     - Thêm category: `"android.bluetooth.headset.intent.category.companyid.76"` (Apple/TWS standard) và mặc định.
   - Khi tai nghe gửi sự kiện `+IPHONEACCEV`, đọc mảng `BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS`:
     - Cặp `(key = 1, val = 0..9)` -> Mức pin thật `(val + 1) * 10%`.
     - Cặp `(key = 2, val = 0..1)` -> Trạng thái đang sạc (`val == 1`).

3. **Tầng 3 - Truy vấn thiết bị đang kết nối thực tế qua `BluetoothManager` & `BluetoothProfile`**:
   - Bổ sung hàm `EarbudsCard.pollConnectedDeviceNow(ctx: Context): Boolean`:
     - Dùng `BluetoothManager.getConnectedDevices(BluetoothProfile.HEADSET)` và `BluetoothAdapter.bondedDevices` lọc thiết bị có `isConnected() == true` (reflection method `isConnected` nằm trong greylist được phép gọi trên Android 14/15) và `getBatteryLevel()` (cũng được phép gọi).
     - Đồng thời đọc các Intent sticky hoặc `Settings.Global` / `Settings.Secure` (nếu Vivo lưu key `vivo_tws_battery_*` hoặc `bluetooth_bat_*`).
   - Khi người dùng bấm vào mẫu **"Tai nghe Bluetooth"** trong ứng dụng:
     - Gọi `EarbudsCard.pollConnectedDeviceNow(ctx)` trước tiên: Nếu đang có tai nghe Bluetooth thật kết nối với máy, hiển thị **đúng tên tai nghe thật và mức pin thật** của tai nghe đó!
     - Chỉ khi máy hoàn toàn không kết nối tai nghe Bluetooth nào mới hiển thị thẻ xem trước (và ghi rõ chú thích `"Chưa kết nối tai nghe · Dữ liệu xem trước"` để người dùng không nhầm với pin thật).

---

## HẠNG MỤC 4: THIẾT KẾ LẠI LOGO ỨNG DỤNG CHUẨN VIVO ORIGINOS 6

### Ngôn ngữ thiết kế Vivo OriginOS 6
- **Bảng màu nền (`ic_launcher_background.xml` & `colors.xml`)**:
  - Loại bỏ nền đen `#0B0E14` kiểu cyberpunk cũ.
  - Sử dụng nền chuyển sắc (Linear Gradient 135 độ) xanh dương biểu trưng của **Vivo OriginOS 6**: từ `#2555FF` (Vivo Electric Blue) sang `#0099FF` (Origin Cyan-Blue), kèm hiệu ứng ánh sáng vòm mềm mại ở góc trên trái.
- **Biểu tượng trung tâm (`ic_launcher_foreground.xml` & bộ PNG `mipmap-*`)**:
  - **Lớp 1 (Origin Glass Card)**: Một tấm thẻ squircle trắng kính mờ (`#33FFFFFF` với viền sáng `#66FFFFFF`) nằm cân đối ở trung tâm biểu tượng, đặc trưng của hệ thống Origin Widget.
  - **Lớp 2 (Atomic Island Pill)**: Viên thuốc Đảo Nguyên Tử màu đen sâu (`#0A0E17`) đặt nổi bật ở phần trên của tấm kính, bên trái là chấm cảm biến xanh ngọc (`#00E5FF`), bên phải là vòng tròn năng lượng đa sắc (`#387AFF` -> `#00E5FF`).
  - **Lớp 3 (Sóng âm & Đồng bộ bên dưới đảo)**: Hai thanh thông tin bo tròn màu trắng tinh khiết (`#FFFFFF` và `#B3FFFFFF`) biểu thị thông báo trực tiếp được nâng lên từ hệ thống.
- **Đồng bộ hóa toàn bộ tài nguyên**:
  - Cập nhật `app/src/main/res/drawable/ic_launcher_background.xml` (Vector gradient chuẩn).
  - Cập nhật `app/src/main/res/drawable/ic_launcher_foreground.xml` (Vector chuẩn OriginOS 6).
  - Cập nhật `app/src/main/res/drawable/ic_launcher_monochrome.xml`.
  - Cập nhật `app/src/main/res/values/colors.xml`.
  - Chạy script Python (`Pillow`) render lại toàn bộ các file ảnh PNG `ic_launcher.png`, `ic_launcher_round.png`, `ic_launcher_foreground.png` trong cả 5 thư mục `mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, `mipmap-xxxhdpi` để mọi trình khởi chạy (Launcher) của Vivo đều nhận ngay logo mới.

---

## HẠNG MỤC 5: KIỂM ĐỊNH, BUILD APK & ĐỒNG BỘ GIT (`v1.0.4`)

1. Nâng `versionCode = 5`, `versionName = "1.0.4"` trong `app/build.gradle.kts`.
2. Chạy `./gradlew assembleDebug` đảm bảo `BUILD SUCCESSFUL` 100%, không còn bất kỳ tham chiếu nào tới `OriginOS 7` hay `Carousel`.
3. Xuất file `OriginIsle-VN-v1.0.4-debug.apk` ra thư mục gốc và đẩy lên GitHub.
